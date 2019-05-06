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

package androidx.camera.camera2.impl.compat;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.impl.compat.params.SessionConfigurationCompat;

import java.util.concurrent.Executor;

/**
 * Helper for accessing features in CameraDevice in a backwards compatible fashion.
 */
@TargetApi(21)
public final class CameraDeviceCompat {

    /**
     * Standard camera operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_NORMAL =
            0; // ICameraDeviceUser.NORMAL_MODE;
    /**
     * Constrained high-speed operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED =
            1; // ICameraDeviceUser.CONSTRAINED_HIGH_SPEED_MODE;

    private static final CameraDeviceCompatImpl IMPL = chooseImplementation();

    /**
     * Create a new {@link CameraCaptureSession} using a {@link SessionConfigurationCompat}
     * helper object that aggregates all supported parameters.
     *
     * @param config A session configuration (see {@link SessionConfigurationCompat}).
     * @throws IllegalArgumentException In case the session configuration
     *                                  is invalid; or the output configurations are empty; or
     *                                  the session configuration executor is invalid.
     * @throws CameraAccessException    In case the camera device is no longer connected or has
     *                                  encountered a fatal error.
     */
    public static void createCaptureSession(@NonNull CameraDevice device,
            @NonNull SessionConfigurationCompat config) throws CameraAccessException {
        IMPL.createCaptureSession(device, config);
    }

    private static CameraDeviceCompatImpl chooseImplementation() {
        if (Build.VERSION.SDK_INT >= 28) {
            return new CameraDeviceCompatApi28Impl();
        } else if (Build.VERSION.SDK_INT >= 24) {
            return new CameraDeviceCompatApi24Impl();
        } else if (Build.VERSION.SDK_INT >= 23) {
            return new CameraDeviceCompatApi23Impl();
        }

        return new CameraDeviceCompatBaseImpl();
    }

    // Class is not a wrapper. Should not be instantiated.
    private CameraDeviceCompat() {}

    interface CameraDeviceCompatImpl {
        void createCaptureSession(@NonNull CameraDevice device,
                @NonNull SessionConfigurationCompat config) throws CameraAccessException;
    }

    static class StateCallbackExecutorWrapper extends CameraCaptureSession.StateCallback {

        CameraCaptureSession.StateCallback mWrappedCallback;
        private Executor mExecutor;

        StateCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraCaptureSession.StateCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        @Override
        public void onConfigured(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onConfigured(session);
                }
            });
        }

        @Override
        public void onConfigureFailed(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onConfigureFailed(session);
                }
            });
        }

        @Override
        public void onReady(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onReady(session);
                }
            });
        }

        @Override
        public void onActive(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onActive(session);
                }
            });
        }

        @RequiresApi(26)
        @Override
        public void onCaptureQueueEmpty(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureQueueEmpty(session);
                }
            });
        }


        @Override
        public void onClosed(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onClosed(session);
                }
            });
        }

        @RequiresApi(23)
        @Override
        public void onSurfacePrepared(@NonNull final CameraCaptureSession session,
                @NonNull final Surface surface) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onSurfacePrepared(session, surface);
                }
            });
        }
    }
}
