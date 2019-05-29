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

package androidx.browser.trusted.splashscreens;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves the splash image to a file and transfers it to Custom Tabs provider.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SplashImageTransferTask {

    private static final String TAG = "SplashImageTransferTask";

    private static final String FOLDER_NAME = "twa_splash";
    private static final String FILE_NAME = "splash_image.png";
    private static final String PREFS_FILE = "splashImagePrefs";
    private static final String PREF_LAST_UPDATE_TIME = "lastUpdateTime";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Bitmap mBitmap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final String mAuthority;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final CustomTabsSession mSession;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final String mProviderPackage;

    @Nullable
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Callback mCallback;

    /**
     * @param context {@link Context} to use.
     * @param bitmap image to transfer.
     * @param authority {@link FileProvider} authority.
     * @param session {@link CustomTabsSession} to use for transferring the file.
     * @param providerPackage Package name of the Custom Tabs provider.
     */
    public SplashImageTransferTask(Context context, Bitmap bitmap, String authority,
            CustomTabsSession session, String providerPackage) {
        mContext = context.getApplicationContext();
        mBitmap = bitmap;
        mAuthority = authority;
        mSession = session;
        mProviderPackage = providerPackage;
    }

    /**
     * Executes the task. Should be called only once.
     * @param callback {@link Callback} to be called when done.
     */
    public void execute(Callback callback) {
        assert mAsyncTask.getStatus() == AsyncTask.Status.PENDING;
        mCallback = callback;
        mAsyncTask.execute();
    }

    /**
     * Cancels the execution. The callback passed into {@link #execute} won't be called, and
     * the references to it will be released.
     */
    public void cancel() {
        mAsyncTask.cancel(true);
        mCallback = null;
    }

    @SuppressLint("StaticFieldLeak") // No leaking should happen
    private final AsyncTask<Void, Void, Boolean> mAsyncTask = new AsyncTask<Void, Void, Boolean>() {

        @Override
        protected Boolean doInBackground(Void... args) {
            if (isCancelled()) return false;
            File dir = new File(mContext.getFilesDir(), FOLDER_NAME);
            if (!dir.exists()) {
                boolean mkDirSuccessful = dir.mkdir();
                if (!mkDirSuccessful) {
                    Log.w(TAG, "Failed to create a directory for storing a splash image");
                    return false;
                }
            }
            File file = new File(dir, FILE_NAME);
            SharedPreferences prefs =
                    mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
            long lastUpdateTime = getLastAppUpdateTime();
            if (file.exists() && lastUpdateTime == prefs.getLong(PREF_LAST_UPDATE_TIME, 0)) {
                // Don't overwrite existing file, if it was saved later than the last time app was
                // updated
                return transferToCustomTabsProvider(file);
            }

            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                if (isCancelled()) return false;
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                prefs.edit().putLong(PREF_LAST_UPDATE_TIME, lastUpdateTime).commit();

                if (isCancelled()) return false;
                return transferToCustomTabsProvider(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (os != null) os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private boolean transferToCustomTabsProvider(File file) {
            return TrustedWebUtils.transferSplashImage(mContext, file, mAuthority, mProviderPackage,
                    mSession);
        }

        private long getLastAppUpdateTime() {
            try {
                return mContext.getPackageManager()
                        .getPackageInfo(mContext.getPackageName(), 0).lastUpdateTime;
            } catch (PackageManager.NameNotFoundException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mCallback != null && !isCancelled()) {
                mCallback.onFinished(success);
            }
        }
    };

    /** Callback to be called when the file is saved and transferred to Custom Tabs provider. */
    public interface Callback {
        /** Called when the file is saved and transferred. */
        void onFinished(boolean successfully);
    }
}
