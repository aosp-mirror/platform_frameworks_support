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

package androidx.camera.extensions.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions in the view
 * finder.
 */
public interface PreviewExtenderImpl {
    /** The different types of the preview processing. */
    enum PreviewProcessorType {
        /** Processing which only updates the {@link CaptureStageImpl}. */
        PREVIEW_PROCESSOR_TYPE_REQUEST_UPDATE_ONLY,
        PREVIEW_PROCESSOR_TYPE_NONE
    }

    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId              The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * Enable the extension if available. If not available then acts a no-op.
     *
     * @param cameraId              The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void enableExtension(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * The set of parameters required to produce the effect on the preview stream.
     *
     * <p> This will be the initial set of parameters used for the preview
     * {@link android.hardware.camera2.CaptureRequest}. Once the {@link RequestUpdateProcessorImpl}
     * from {@link #getRequestUpdatePreviewProcessor()} has been called, this should be updated to
     * reflect the new {@link CaptureStageImpl}. If the processing step returns a {@code null},
     * meaning the required parameters has not changed, then calling this will return the previous
     * non-null value.
     */
    CaptureStageImpl getCaptureStage();

    /**
     * Notify to initial of the extension. It would be called after bindToLifeCycle. This is
     * where the use case is started and would be able to allocate resources here. After onInit() is
     * called, the camera ID, cameraCharacteristics and context will not change until onDeInit()
     * has been called.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @param context The {@link Context} used for CameraX.
     */
    void onInit(String cameraId, CameraCharacteristics cameraCharacteristics, Context context);

    /**
     * Notify to de-initialize of the extension. This callback would be invoked after unbind.
     * After onDeInit() was called, it is expected that the camera ID, cameraCharacteristics will
     * no longer hold, this should be where to clear all resources allocated for this use case.
     */
    void onDeInit();

    /**
     * This would be invoked before create capture session of Camera2. The returned parameter in
     * CaptureStage will be passed to the camera device as part of the capture session
     * initialization via setSessionParameters(). The valid parameter is a subset of the
     * available capture request parameters.
     *
     * @return The request information to set the session wide camera parameters.
     */
    CaptureStageImpl onPresetSession();

    /**
     * This would be invoked once after a Camera2 capture session was created. The returned
     * parameter in CaptureStage will be used to generate a single request to the current
     * configured camera device. The generated request would be submitted to camera before process
     * other single request.
     *
     * @return The request information to create a single capture request to camera device.
     */
    CaptureStageImpl onEnableSession();

    /**
     * This would be invoked once before the Camera2 capture session was going to close. The
     * returned parameter in CaptureStage will be used to generate a single request to the current
     * configured camera device. The generated request would be submitted to camera before the
     * capture session was closed.
     *
     * @return The request information to customize the session.
     */
    CaptureStageImpl onDisableSession();

    /**
     * This callback would be invoked when CameraX going to change the configured surface with
     * a resolution.
     *
     * @param size for the surface.
     */
    void onResolutionUpdate(Size size);

    /**
     * This callback would be invoked when the image format was updated.
     *
     * @param imageFormat for the surface.
     */
    void onImageFormatUpdate(int imageFormat);

    /** The type of preview processing to use. */
    PreviewProcessorType getPreviewProcessorType();

    /** Returns a processor which only updates the {@link CaptureStageImpl}. */
    RequestUpdateProcessorImpl getRequestUpdatePreviewProcessor();
}
