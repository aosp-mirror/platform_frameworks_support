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

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An open helper that will copy & open a prepackaged database if it doesn't exists in internal
 * storage.
 */
class SQLiteCopyOpenHelper implements SupportSQLiteOpenHelper {

    // File path we'll interpret to look for DB file in the assets directory.
    static final String FROM_ASSETS_PATH = "assets:/";

    // Folder assets directory where we'll look for DB files.
    private static final String ASSETS_DB_FOLDER = "databases/";

    private final Context mContext;
    private final String mCopyFromFilePath;
    private final SupportSQLiteOpenHelper mDelegate;

    private SupportSQLiteDatabase mDatabase;

    SQLiteCopyOpenHelper(Context context, String copyFromFilePath,
            SupportSQLiteOpenHelper supportSQLiteOpenHelper) {
        mContext = context;
        mCopyFromFilePath = copyFromFilePath;
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
        if (mDatabase == null) {
            copyDatabaseFile();
        }
        mDatabase = mDelegate.getWritableDatabase();
        return mDatabase;
    }

    @Override
    public synchronized SupportSQLiteDatabase getReadableDatabase() {
        if (mDatabase == null) {
            copyDatabaseFile();
        }
        mDatabase =  mDelegate.getWritableDatabase();
        return mDatabase;
    }

    @Override
    public synchronized void close() {
        mDelegate.close();
        mDatabase = null;
    }

    private void copyDatabaseFile() {
        String databaseName = getDatabaseName();
        File databaseFile = mContext.getDatabasePath(databaseName);
        if (databaseFile.exists()) {
            // internal database file is present, no need to copy
            return;
        }

        try {
            InputStream input;
            if (mCopyFromFilePath.equals(FROM_ASSETS_PATH)) {
                input = mContext.getAssets().open(ASSETS_DB_FOLDER + databaseName);
            } else {
                input = new FileInputStream(mCopyFromFilePath);
            }
            OutputStream output = new FileOutputStream(databaseFile);
            copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy pre-packaged database file.", e);
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
