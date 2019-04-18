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

import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

/**
 * Provides abstract methods that the OEM needs to implement to enable extensions for image capture.
 */
public interface ImageCaptureExtenderImpl {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * Enable the extension if available. If not available then acts a no-op.
     *
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void enableExtension(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * The processing that will be done on a set of captures to create and image with the effect.
     */
    CaptureProcessorImpl getCaptureProcessor();

    /**
     * Notify to initial of the extension.
     * @param cameraId The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    void onInit(String cameraId, CameraCharacteristics cameraCharacteristics);

    /**
     * Notify to de-initial of the extension.
     */
    void onDeInit();

    /**
     * This method would be invoked before every session start.
     * @return The request information to customize the session.
     */
    CaptureStageImpl onPresetSession();

    /**
     * This method would be invoked after every session start.
     * @return The request information to customize the session.
     */
    CaptureStageImpl onEnableSession();

    /**
     * This method would be invoked before every session close.
     * @return The request information to customize the session.
     */
    CaptureStageImpl onDisableSession();

    /**
     * This method would be invoked every resolution update.
     * @param size for the surface.
     * @param imageFormat for the surface.
     */
    void onResolutionUpdate(Size size, int imageFormat);

}
