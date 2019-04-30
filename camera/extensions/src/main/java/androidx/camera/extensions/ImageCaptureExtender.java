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

package androidx.camera.extensions;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Class for using an OEM provided extension on image capture.
 */
abstract class ImageCaptureExtender {
    private ImageCaptureConfig.Builder mBuilder;
    private ImageCaptureExtenderImpl mImpl;

    void init(ImageCaptureConfig.Builder builder, ImageCaptureExtenderImpl implementation) {
        mBuilder = builder;
        mImpl = implementation;
    }

    public boolean isExtensionAvailable() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        return mImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    public void enableExtension() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        mImpl.enableExtension(cameraId, cameraCharacteristics);

        CaptureProcessorImpl captureProcessor = mImpl.getCaptureProcessor();
        if (captureProcessor != null) {
            mBuilder.setCaptureProcessor(new AdaptingCaptureProcessor(captureProcessor));
        }

        if (mImpl.getMaxCaptureStage() > 0) {
            mBuilder.setMaxCaptureStages(mImpl.getMaxCaptureStage());
        }

        ImageCaptureAdapter imageCaptureAdapter = new ImageCaptureAdapter(mImpl);
        new Camera2Config.Extender(mBuilder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureAdapter));
        mBuilder.setUseCaseEventListener(imageCaptureAdapter);
        mBuilder.setCaptureBundle(imageCaptureAdapter);
    }

    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    static class ImageCaptureAdapter extends CameraEventCallback implements UseCase.EventListener,
            CaptureBundle {

        private final ImageCaptureExtenderImpl mImpl;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;

        ImageCaptureAdapter(ImageCaptureExtenderImpl impl) {
            mImpl = impl;
        }

        @Override
        public void onBind(String cameraId) {
            CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                    cameraId);
            mImpl.onInit(cameraId, cameraCharacteristics, CameraX.getContext());
        }

        @Override
        public void onUnbind() {
            boolean callNow = false;
            synchronized (mLock) {
                if (mEnabledSessionCount == 0) {
                    callNow = true;
                }
            }

            if (callNow) {
                callDeInit(CameraXExecutors.mainThreadExecutor());
            }
        }

        private void callDeInit(Executor executor) {
            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mImpl.onDeInit();
                    }
                });
            }
        }

        @Override
        public List<CaptureConfig> onPresetSession() {
            CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
            if (captureStageImpl != null) {
                return Arrays.asList(new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
            }

            return null;
        }

        @Override
        public List<CaptureConfig> onEnableSession() {
            try {
                CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
                if (captureStageImpl != null) {
                    return Arrays.asList(
                            new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount++;
                }
            }
        }

        @Override
        public List<CaptureConfig> onDisableSession() {
            try {
                CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
                if (captureStageImpl != null) {
                    return Arrays.asList(
                            new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
                }

                return null;
            } finally {
                Executor executor = null;
                synchronized (mLock) {
                    mEnabledSessionCount--;
                    if (mEnabledSessionCount == 0) {
                        executor = CameraXExecutors.mainThreadExecutor();
                    }
                }
                callDeInit(executor);
            }
        }

        @Override
        public void onResolutionUpdate(Size size) {
            mImpl.onResolutionUpdate(size);
        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {
            mImpl.onImageFormatUpdate(imageFormat);
        }

        @Override
        public List<CaptureStage> getCaptureStages() {
            List<CaptureStageImpl> captureStages = mImpl.getCaptureStages();
            if (captureStages != null && !captureStages.isEmpty()) {
                ArrayList<CaptureStage> ret = new ArrayList();
                for (CaptureStageImpl s: captureStages) {
                    ret.add(new AdaptingCaptureStage(s));
                }
                return ret;
            }

            return null;
        }
    }

}
