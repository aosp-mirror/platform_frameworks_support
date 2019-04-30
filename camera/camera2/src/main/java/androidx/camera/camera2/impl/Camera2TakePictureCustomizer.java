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

package androidx.camera.camera2.impl;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.TakePictureCustomizer;

/**
 * Camera2 implementation for {@link TakePictureCustomizer}.
 *
 * <p>Appends the camera2 specific settings for taking a picture.
 */
class Camera2TakePictureCustomizer extends TakePictureCustomizer {

    /**
     * {@inheritDoc}
     *
     * <p>Appends the {@link CameraDevice#TEMPLATE_STILL_CAPTURE} and CaptureRequest Key/value pair.
     *
     * @param builder the CaptureConfig.Builder.
     */
    @Override
    public void apply(@NonNull CaptureConfig.Builder builder) {

        builder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);

        applyPixelHdrPlusChangeForCaptureMode(builder);
    }

    // TODO(b/123897971):  move the device specific code once we complete the device workaround
    // module.
    private void applyPixelHdrPlusChangeForCaptureMode(
            @NonNull CaptureConfig.Builder captureConfigBuilder) {
        if (getDeviceSpecific().hasPixelHdrPlus()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Camera2Config.Builder builder = new Camera2Config.Builder();
                switch (getCaptureMode()) {
                    case MAX_QUALITY:
                        // enable ZSL to make sure HDR+ is enabled
                        builder.setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, true);
                        break;
                    case MIN_LATENCY:
                        // disable ZSL to turn off HDR+
                        builder.setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, false);
                        break;
                }
                captureConfigBuilder.addImplementationOptions(builder.build());
            }
        }
    }

    /** Factory for producing the Camera2TakePictureCustomizer. */
    public static class Factory extends TakePictureCustomizer.Factory {

        /**
         * Produces the Camera2TakePictureCustomizer.
         *
         * @return the Camera2TakePictureCustomizer
         */
        @NonNull
        @Override
        protected Camera2TakePictureCustomizer create() {
            return new Camera2TakePictureCustomizer();
        }
    }
}
