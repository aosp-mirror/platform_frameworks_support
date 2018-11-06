/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages connected {@link ControllerInfo} from the legacy controller/browser. This is thread-safe.
 */
class ConnectedLegacyControllersManager<T> extends ConnectedControllersManager<T> {
    private static final String TAG = "MS2LegacyControllerMgr";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final long CHECK_CONTROLLER_DELAY_MS = TimeUnit.MINUTES.toMillis(5);

    private final ControllerCheckHandler mHandler;
    private final ActivityManager mActivityManager;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, ControllerCheckRunnable> mControllerCheckRunnableMap =
            new ArrayMap<>();
    private volatile long mConnectedLegacyControllerCheckDelay; // only for testing.

    ConnectedLegacyControllersManager(MediaSession2Impl session) {
        super(session);
        mHandler = new ControllerCheckHandler(Looper.getMainLooper());
        mActivityManager =
                (ActivityManager) session.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        mConnectedLegacyControllerCheckDelay = CHECK_CONTROLLER_DELAY_MS;
    }

    @Override
    public void addController(T key, ControllerInfo controller, SessionCommandGroup2 commands) {
        super.addController(key, controller, commands);
        mHandler.checkControllerDelayed(controller);
    }

    @Override
    public ControllerInfo getController(T key) {
        ControllerInfo controllerInfo = super.getController(key);
        mHandler.checkControllerDelayed(controllerInfo);
        return controllerInfo;
    }

    /**
     * Only for testing. Do not use otherwise.
     *
     * @param delayMs
     */
    public void setConnectedLegacyControllerCheckDelay(long delayMs) {
        mConnectedLegacyControllerCheckDelay = delayMs;
    }

    private class ControllerCheckHandler extends Handler {
        ControllerCheckHandler(Looper looper) {
            super(looper);
        }

        /**
         * Checks whether the process that holds the {@link ControllerInfo} is still alive after
         * the delay. Note that we cannot simply remove a {@link ControllerInfo} due to the
         * inactivity because a {@link android.support.v4.media.session.MediaControllerCompat} can
         * only monitor the playback for several hours.
         *
         * @param info controllerInfo to check
         */
        public void checkControllerDelayed(ControllerInfo info) {
            if (info == null) {
                // Not an error. getController() might have been called to check whether the
                // ControllerInfo is registered already.
                return;
            }
            if (info.getRemoteUserInfo() == null) {
                Log.w(TAG, "Cannot clean up legacy controller. Failed to get RemoteUserInfo");
                return;
            } else if (info.getRemoteUserInfo().getPid() == RemoteUserInfo.UNKNOWN_PID
                    && TextUtils.isEmpty(info.getRemoteUserInfo().getPackageName())) {
                Log.w(TAG, "Cannot clean up legacy controller. Failed to get valid RemoteUserInfo"
                        + ", controllerInfo.remoteUserInfo.pid=" + info.getRemoteUserInfo().getPid()
                        + ", controllerInfo.remoteUserInfo.pkg="
                        + info.getRemoteUserInfo().getPackageName());
                return;
            }

            ControllerCheckRunnable runnable;
            synchronized (mLock) {
                runnable = mControllerCheckRunnableMap.get(info);
                if (runnable == null) {
                    runnable = new ControllerCheckRunnable(info);
                    mControllerCheckRunnableMap.put(info, runnable);
                }
            }
            removeCallbacks(runnable);
            postDelayed(runnable, mConnectedLegacyControllerCheckDelay);
        }
    }

    private class ControllerCheckRunnable implements Runnable {
        public final ControllerInfo controllerInfo;

        ControllerCheckRunnable(ControllerInfo controllerInfo) {
            this.controllerInfo = controllerInfo;
        }

        @Override
        public void run() {
            List<RunningAppProcessInfo> list = mActivityManager.getRunningAppProcesses();
            if (list == null) {
                Log.w(TAG, "Cannot clean up legacy controller. Failed to call"
                        + " getRunningAppProcesses()");
                return;
            }
            for (RunningAppProcessInfo process : list) {
                if (controllerInfo.getRemoteUserInfo().getPid() == RemoteUserInfo.UNKNOWN_PID
                        || process.pid == controllerInfo.getRemoteUserInfo().getPid()) {
                    if (process.pkgList == null || process.pkgList.length == 0) {
                        Log.w(TAG, "Cannot clean up legacy controller. Failed to get pkgList from"
                                + " the pid=" + process.pid);
                        return;
                    }
                    for (String pkg : process.pkgList) {
                        if (TextUtils.equals(pkg, controllerInfo.getPackageName())) {
                            // process is still alive. Check controller again later.
                            mHandler.checkControllerDelayed(controllerInfo);
                            return;
                        }
                    }
                    if (process.pid == controllerInfo.getRemoteUserInfo().getPid()) {
                        if (DEBUG) {
                            Log.d(TAG, "Removing legacy controller from removed packaged");
                        }
                        removeController(controllerInfo);
                        return;
                    }
                }
            }
            if (DEBUG) {
                Log.d(TAG, "Removing legacy controller from removed process, controllerInfo="
                        + controllerInfo);
            }
            removeController(controllerInfo);
        }
    }
}
