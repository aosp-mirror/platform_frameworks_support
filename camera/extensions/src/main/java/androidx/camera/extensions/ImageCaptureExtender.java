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

import androidx.annotation.GuardedBy;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Config;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.ExtensionsErrorListener.ExtensionsErrorCode;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class for using an OEM provided extension on image capture.
 */
abstract class ImageCaptureExtender {
    static final Config.Option<String> OPTION_IMAGE_CAPTURE_EXTENDER_TYPE = Config.Option.create(
            "camerax.extensions.imageCaptureExtender.type", String.class);

    private ImageCaptureConfig.Builder mBuilder;
    private ImageCaptureExtenderImpl mImpl;
    private String mDependenceExtenderClass = null;

    void init(ImageCaptureConfig.Builder builder, ImageCaptureExtenderImpl implementation) {
        mBuilder = builder;
        mImpl = implementation;
        mDependenceExtenderClass = getClass().toString().replace("ImageCapture", "Preview");
    }

    /**
     * Indicates whether extension function can support with {@link ImageCaptureConfig.Builder}
     *
     * @return True if the specific extension function is supported for the camera device.
     */
    public boolean isExtensionAvailable() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        return mImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    /**
     * Enables the derived {@link ImageCaptureExtender} feature.
     *
     * <p>{@link ImageCaptureExtender} has dependence on {@link PreviewExtender}.
     * {@link ImageCaptureExtender} will receive PREVIEW_EXTENSION_REQUIRED error if
     * corresponding {@link PreviewExtender} is not enabled together.
     */
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
        mBuilder.getMutableConfig().insertOption(OPTION_IMAGE_CAPTURE_EXTENDER_TYPE,
                getClass().toString());
    }

    void checkPreviewEnabled() {
        ExtensionsErrorListener extensionsErrorListener =
                ExtensionsManager.getExtensionsErrorListener();
        if (extensionsErrorListener != null) {
            Collection<UseCase> activeUseCases = CameraX.getActiveUseCases();
            boolean bIsPreviewExtenderEnabled = false;

            for (UseCase useCase : activeUseCases) {
                String previewExtenderType = useCase.getUseCaseConfig().retrieveOption(
                        PreviewExtender.OPTION_PREVIEW_EXTENDER_TYPE, null);
                if (mDependenceExtenderClass != null && mDependenceExtenderClass.equals(
                        previewExtenderType)) {
                    bIsPreviewExtenderEnabled = true;
                    break;
                }
            }

            if (!bIsPreviewExtenderEnabled) {
                extensionsErrorListener.onError(ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
            }
        }
    }

    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    class ImageCaptureAdapter extends CameraEventCallback implements UseCase.EventListener,
            CaptureBundle {

        private final ImageCaptureExtenderImpl mImpl;
        private final AtomicBoolean mActive = new AtomicBoolean(true);
        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private volatile boolean mUnbind = false;

        ImageCaptureAdapter(ImageCaptureExtenderImpl impl) {
            mImpl = impl;
        }

        @Override
        public void onBind(String cameraId) {
            if (mActive.get()) {
                CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                        cameraId);
                mImpl.onInit(cameraId, cameraCharacteristics, CameraX.getContext());
            }
        }

        @Override
        public void onUnbind() {
            synchronized (mLock) {
                mUnbind = true;
                if (mEnabledSessionCount == 0) {
                    callDeInit();
                }
            }
        }

        private void callDeInit() {
            if (mActive.get()) {
                mImpl.onDeInit();
                mActive.set(false);
            }
        }

        @Override
        public CaptureConfig onPresetSession() {
            if (mActive.get()) {
                checkPreviewEnabled();
                CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
                if (captureStageImpl != null) {
                    return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                }
            }
            return null;
        }

        @Override
        public CaptureConfig onEnableSession() {
            try {
                if (mActive.get()) {
                    CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
                    if (captureStageImpl != null) {
                        return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                    }
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount++;
                }
            }
        }

        @Override
        public CaptureConfig onDisableSession() {
            try {
                if (mActive.get()) {
                    CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
                    if (captureStageImpl != null) {
                        return new AdaptingCaptureStage(captureStageImpl).getCaptureConfig();
                    }
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount--;
                    if (mEnabledSessionCount == 0 && mUnbind) {
                        callDeInit();
                    }
                }
            }
        }

        @Override
        public List<CaptureStage> getCaptureStages() {
            if (mActive.get()) {
                List<CaptureStageImpl> captureStages = mImpl.getCaptureStages();
                if (captureStages != null && !captureStages.isEmpty()) {
                    ArrayList<CaptureStage> ret = new ArrayList();
                    for (CaptureStageImpl s : captureStages) {
                        ret.add(new AdaptingCaptureStage(s));
                    }
                    return ret;
                }
            }

            return null;
        }
    }

}
