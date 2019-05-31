/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room;

import static androidx.room.DatabaseConfiguration.COPY_FROM_ASSET;
import static androidx.room.DatabaseConfiguration.COPY_FROM_FILE;
import static androidx.room.DatabaseConfiguration.COPY_FROM_NONE;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.room.DatabaseConfiguration.CopyFrom;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An open helper that will copy & open a pre-populated database if it doesn't exists in internal
 * storage.
 */
class SQLiteCopyOpenHelper implements SupportSQLiteOpenHelper {

    @NonNull
    private final Context mContext;
    @CopyFrom
    private final int mCopyFrom;
    @NonNull
    private final String mCopyFromFilePath;
    private final boolean mCopyOnDestructiveMigration;
    private final int mDatabaseVersion;
    @NonNull
    private final SupportSQLiteOpenHelper mDelegate;
    @Nullable
    private DatabaseConfiguration mDatabaseConfiguration;

    private boolean mVerified;

    SQLiteCopyOpenHelper(
            @NonNull Context context,
            @CopyFrom int copyFrom,
            @NonNull String copyFromFilePath,
            boolean copyOnDestructiveMigration,
            int databaseVersion,
            @NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper) {
        mContext = context;
        mCopyFromFilePath = copyFromFilePath;
        mCopyFrom = copyFrom;
        mCopyOnDestructiveMigration = copyOnDestructiveMigration;
        mDatabaseVersion = databaseVersion;
        mDelegate = supportSQLiteOpenHelper;
    }

    @Override
    public String getDatabaseName() {
        return mDelegate.getDatabaseName();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mDelegate.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public synchronized SupportSQLiteDatabase getWritableDatabase() {
        if (!mVerified) {
            verifyDatabaseFile();
            mVerified = true;
        }
        return mDelegate.getWritableDatabase();
    }

    @Override
    public synchronized SupportSQLiteDatabase getReadableDatabase() {
        if (!mVerified) {
            verifyDatabaseFile();
            mVerified = true;
        }
        return mDelegate.getReadableDatabase();
    }

    @Override
    public synchronized void close() {
        mDelegate.close();
        mVerified = false;
    }

    void setDatabaseConfiguration(@Nullable DatabaseConfiguration databaseConfiguration) {
        mDatabaseConfiguration = databaseConfiguration;
    }

    private void verifyDatabaseFile() {
        String databaseName = getDatabaseName();
        File databaseFile = mContext.getDatabasePath(databaseName);
        if (!databaseFile.exists()) {
            copyDatabaseFile(databaseFile);
            return;
        }

        // A database file is present, check if we need to re-copy it.
        if (mCopyOnDestructiveMigration) {
            try {
                if (mDatabaseConfiguration == null) {
                    return;
                }

                int currentVersion = DBUtil.readVersion(databaseFile);
                if (currentVersion == mDatabaseVersion) {
                    return;
                }

                if (mDatabaseConfiguration.isMigrationRequired(currentVersion, mDatabaseVersion)) {
                    // A migration is required, don't copy the database file, instead open it and
                    // let migrations run.
                    return;
                }

                // Current database file is needs to migrate but a migration is not required, delete
                // database and re-copy.
                if (mContext.deleteDatabase(databaseName)) {
                    copyDatabaseFile(databaseFile);
                }
            } catch (IOException e) {
                Log.w(Room.LOG_TAG, "Unable to read database version.", e);
            }
        }
    }

    private void copyDatabaseFile(File destinationFile) {
        try {
            InputStream input;
            switch (mCopyFrom) {
                case COPY_FROM_NONE:
                    return;
                case COPY_FROM_ASSET:
                    input = mContext.getAssets().open(mCopyFromFilePath);
                    break;
                case COPY_FROM_FILE:
                    input = new FileInputStream(mCopyFromFilePath);
                    break;
                default:
                    throw new IllegalStateException("Unknown CopyFrom: " + mCopyFrom);
            }
            OutputStream output = new FileOutputStream(destinationFile);
            copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy database file.", e);
        }
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        try {
            int length;
            byte[] buffer = new byte[1024 * 4];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}
