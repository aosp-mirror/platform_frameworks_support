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

import androidx.camera.core.CaptureRequestInfo;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.SessionEventListener;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;


/**
 * Class for using an OEM provided extension on preview.
 */
public class PreviewExtenderAdapter implements SessionEventListener, CaptureRequestInfo {

    private PreviewExtenderImpl mImpl;

    public PreviewExtenderAdapter(PreviewExtenderImpl impl) {
        mImpl = impl;
    }

    @Override
    public void onInit(String cameraId) {
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraCharacteristics(cameraId);
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

    @Override
    public CaptureStage getCaptureStage() {
        CaptureStageImpl captureStageImpl = mImpl.getCaptureStage();
        if (captureStageImpl != null) {
            return new AdaptingCaptureStage(captureStageImpl);
        }

        return null;
    }
}
