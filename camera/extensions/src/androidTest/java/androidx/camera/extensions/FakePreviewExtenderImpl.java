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

import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.ProcessorImpl;

/**
 * A fake implementation of {@link PreviewExtenderImpl} where the values are settable.
 */
class FakePreviewExtenderImpl implements PreviewExtenderImpl {
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;

    private boolean mAvailable = true;
    SettableCaptureStage mCaptureStage;
    private ProcessorImpl mProcessor;
    private ProcessorType mProcessorType;

    FakePreviewExtenderImpl() {
    }

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
        mCaptureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        mCaptureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_OFF);
        mProcessorType = ProcessorType.PROCESSOR_TYPE_NONE;
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        return mAvailable;
    }

    @Override
    public CaptureStageImpl getCaptureStage() {
        return mCaptureStage;
    }

    @Override
    public ProcessorType getProcessorType() {
        return mProcessorType;
    }

    @Override
    public ProcessorImpl getProcessor() {
        return mProcessor;
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
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onEnableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    @Override
    public CaptureStageImpl onDisableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        return captureStage;
    }

    public <T> void addCaptureStageParameter(CaptureRequest.Key<T> key, T value) {
        mCaptureStage.addCaptureRequestParameters(key, value);
    }

    public void setCaptureProcessor(ProcessorImpl processorImpl) {
        mProcessor = processorImpl;
    }

    public void setExtensionAvailable(boolean available) {
        mAvailable = available;
    }

    public void setProcessorType(ProcessorType processorType) {
        mProcessorType = processorType;
    }
}
