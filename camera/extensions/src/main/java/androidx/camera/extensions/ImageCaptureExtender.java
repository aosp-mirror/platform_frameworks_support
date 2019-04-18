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

import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.SessionEventListener;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;

import java.util.List;
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
        List<CaptureStageImpl> captureStages = null;
        if (captureProcessor != null) {
            mBuilder.setCaptureProcessor(new AdaptingCaptureProcessor(captureProcessor));
            captureStages = captureProcessor.getCaptureStages();
        }

        if (captureStages != null && !captureStages.isEmpty()) {
            CaptureBundle captureBundle = new CaptureBundle();

            for (CaptureStageImpl captureStage : captureStages) {
                captureBundle.addCaptureStage(new AdaptingCaptureStage(captureStage));
            }
            mBuilder.setCaptureBundle(captureBundle);
        }

        mBuilder.setSessionEventListener(new ImageCaptureAdapter(mImpl));
    }

    /**
     * An implementation to adapt the OEM provided extension to CameraX.
     */
    static class ImageCaptureAdapter implements SessionEventListener {

        private final ImageCaptureExtenderImpl mImpl;

        ImageCaptureAdapter(ImageCaptureExtenderImpl impl) {
            mImpl = impl;
        }

        @Override
        public void onInit(String cameraId) {
            CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                    cameraId);
            mImpl.onInit(cameraId, cameraCharacteristics);
        }

        @Override
        public void onDeInit() {
            mImpl.onDeInit();
        }

        @Override
        public CaptureStage onPresetSession() {
            CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
            if (captureStageImpl != null) {
                return new AdaptingCaptureStage(captureStageImpl);
            }

            return null;
        }

        @Override
        public CaptureStage onEnableSession() {
            CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
            if (captureStageImpl != null) {
                return new AdaptingCaptureStage(captureStageImpl);
            }

            return null;
        }

        @Override
        public CaptureStage onDisableSession() {
            CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
            if (captureStageImpl != null) {
                return new AdaptingCaptureStage(captureStageImpl);
            }

            return null;
        }

        @Override
        public void onResolutionUpdate(Size size, int imageFormat) {
            mImpl.onResolutionUpdate(size, imageFormat);
        }
    }

}
