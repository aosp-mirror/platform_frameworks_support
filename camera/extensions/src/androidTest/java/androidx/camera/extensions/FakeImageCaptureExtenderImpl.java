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

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A fake implementation of {@link ImageCaptureExtenderImpl} where the values are settable.
 */
class FakeImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;
    private static final int MAX_STAGE = 3;

    private boolean mAvailable = true;
    private SettableCaptureStage mCaptureStage;
    private CaptureProcessorImpl mCaptureProcessor = new CaptureProcessorImpl() {
        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {
        }

        @Override
        public void process(Map<Integer, Pair<Image, TotalCaptureResult>> results) {
        }

        @Override
        public void onResolutionUpdate(Size size) {
        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {
        }
    };

    FakeImageCaptureExtenderImpl() {
    }

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
        mCaptureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        mCaptureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_OFF);
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        return mAvailable;
    }

    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(mCaptureStage);
        return captureStages;
    }

    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        return mCaptureProcessor;
    }

    @Override
    public void onInit(String cameraId, CameraCharacteristics cameraCharacteristics,
            Context context) {

    }

    @Override
    public void onDeInit() {

    }

    @Override
    public CaptureStageImpl onPresetSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onDisableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public int getMaxCaptureStage() {
        return MAX_STAGE;
    }

    public <T> void addCaptureStageParameter(CaptureRequest.Key<T> key, T value) {
        mCaptureStage.addCaptureRequestParameters(key, value);
    }

    public void setCaptureProcessor(CaptureProcessorImpl captureProcessorImpl) {
        mCaptureProcessor = captureProcessorImpl;
    }

    public void setExtensionAvailable(boolean available) {
        mAvailable = available;
    }
}
