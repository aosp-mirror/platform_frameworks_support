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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An open helper that will copy & open a pre-populated database if it doesn't exists in internal
 * storage.
 */
class SQLiteCopyOpenHelper implements SupportSQLiteOpenHelper {

    @NonNull
    private final Context mContext;
    @Nullable
    private final String mCopyFromAssetPath;
    @Nullable
    private final File mCopyFromFile;
    private final int mDatabaseVersion;
    @NonNull
    private final SupportSQLiteOpenHelper mDelegate;
    @Nullable
    private DatabaseConfiguration mDatabaseConfiguration;

    private boolean mVerified;

    SQLiteCopyOpenHelper(
            @NonNull Context context,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            int databaseVersion,
            @NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper) {
        mContext = context;
        mCopyFromAssetPath = copyFromAssetPath;
        mCopyFromFile = copyFromFile;
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

    // Can't be constructor param because the factory is needed by the database builder which in
    // turn is the one that actually builds the configuration.
    void setDatabaseConfiguration(@Nullable DatabaseConfiguration databaseConfiguration) {
        mDatabaseConfiguration = databaseConfiguration;
    }

    private void verifyDatabaseFile() {
        String databaseName = getDatabaseName();
        File databaseFile = mContext.getDatabasePath(databaseName);
        CopyLock copyLock = new CopyLock(mContext.getCacheDir(), databaseName);
        try {
            try {
                // Acquire a copy lock, this lock works across threads and processes, preventing
                // concurrent copy attempts from occurring.
                copyLock.acquire();
            } catch (CopyLockException e) {
                Log.w(Room.LOG_TAG, "Unable to copy database file.", e);
                return;
            }

            if (!databaseFile.exists()) {
                try {
                    // No database file found, copy and be done.
                    copyDatabaseFile(databaseFile);
                    return;
                } catch (IOException e) {
                    throw new RuntimeException("Unable to copy database file.", e);
                }
            }

            if (mDatabaseConfiguration == null) {
                return;
            }

            // A database file is present, check if we need to re-copy it.
            int currentVersion;
            try {
                currentVersion = DBUtil.readVersion(databaseFile);
            } catch (IOException e) {
                Log.w(Room.LOG_TAG, "Unable to read database version.", e);
                return;
            }

            if (currentVersion == mDatabaseVersion) {
                return;
            }

            if (mDatabaseConfiguration.isMigrationRequired(currentVersion, mDatabaseVersion)) {
                // From the current version to the desired version a migration is required, i.e.
                // we won't be performing a copy destructive migration.
                return;
            }

            if (mContext.deleteDatabase(databaseName)) {
                try {
                    copyDatabaseFile(databaseFile);
                } catch (IOException e) {
                    // We are more forgiving copying a database on a destructive migration since
                    // there is already a database file that can be opened.
                    Log.w(Room.LOG_TAG, "Unable to copy database file.", e);
                }
            } else {
                Log.w(Room.LOG_TAG, "Failed to delete database file ("
                        + databaseName + ") for a copy destructive migration.");
            }
        } finally {
            copyLock.release();
        }
    }

    private void copyDatabaseFile(File destinationFile) throws IOException {
        ReadableByteChannel input;
        if (mCopyFromAssetPath != null) {
            input = Channels.newChannel(mContext.getAssets().open(mCopyFromAssetPath));
        } else if (mCopyFromFile != null) {
            input = new FileInputStream(mCopyFromFile).getChannel();
        } else {
            throw new IllegalStateException("copyFromAssetPath and copyFromFile == null!");
        }

        // An intermediate file is used so that we never end up with a half-copied database file
        // in the internal directory.
        File intermediateFile = File.createTempFile(getDatabaseName(), ".tmp");
        FileChannel output = new FileOutputStream(intermediateFile).getChannel();
        copy(input, output);

        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directories for "
                    + destinationFile.getAbsolutePath());
        }

        if (!intermediateFile.renameTo(destinationFile)) {
            throw new IOException("Failed to move intermediate file ("
                    + intermediateFile.getAbsolutePath() + ") to destination ("
                    + destinationFile.getAbsolutePath() + ").");
        }
    }

    private void copy(ReadableByteChannel input, FileChannel output) throws IOException {
        try {
            output.lock();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                output.transferFrom(input, 0, Long.MAX_VALUE);
            } else {
                InputStream inputStream = Channels.newInputStream(input);
                OutputStream outputStream = Channels.newOutputStream(output);
                int length;
                byte[] buffer = new byte[1024 * 4];
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        } finally {
            input.close();
            output.close();
        }
    }

    /**
     * Utility class for in-process and multi-process lock mechanism.
     *
     * There is two levels of locking:
     * 1. Thread locking within the same JVM process is done via a map of lock file paths to
     *    ReentrantLock objects.
     * 2. Processing locking via a dummy file and FileLock.
     *
     * Acquiring this lock will be quick if no other thread or process has the lock. But if the
     * lock is already held then acquiring will block, until the other thread or process releases
     * the lock.
     */
    private static class CopyLock {

        // in-process lock map
        private static final Map<String, Lock> sThreadLocks = new HashMap<>();

        private final File mCopyLockFile;
        private final Lock mThreadLock;
        private FileChannel mLockChannel;

        CopyLock(File lockDir, String name) {
            mCopyLockFile = new File(lockDir, name + ".lck");
            mThreadLock = getThreadLock(mCopyLockFile.getAbsolutePath());
        }

        boolean acquire() throws CopyLockException {
            if (mThreadLock.tryLock()) {
                try {
                    mLockChannel = new FileOutputStream(mCopyLockFile).getChannel();
                    if (mLockChannel.tryLock().isValid()) {
                        return true;
                    }
                    mLockChannel.lock();
                } catch (IOException e) {
                    throw new CopyLockException("Unable to grab copy lock.", e);
                }
            } else {
                mThreadLock.lock();
            }

            return false;
        }

        void release() {
            if (mLockChannel != null) {
                try {
                    mLockChannel.close();
                } catch (IOException ignored) { }
            }
            mThreadLock.unlock();
        }

        private static Lock getThreadLock(String key) {
            synchronized (sThreadLocks) {
                final Lock threadLock = sThreadLocks.get(key);
                if (threadLock == null) {
                    sThreadLocks.put(key, new ReentrantLock());
                }
                return threadLock != null ? threadLock : sThreadLocks.get(key);
            }
        }
    }

    private static class CopyLockException extends Exception {
        CopyLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
