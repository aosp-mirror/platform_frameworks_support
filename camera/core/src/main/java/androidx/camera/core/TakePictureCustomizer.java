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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A customizer for adding special settings to the {@link CaptureConfig} of picture taking.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class TakePictureCustomizer {

    /** The capture mode. */
    private ImageCapture.CaptureMode mCaptureMode = ImageCapture.CaptureMode.MIN_LATENCY;

    /** The device specific properties. */
    private DeviceSpecific mDeviceSpecific = DeviceSpecific.INSTANCE;

    /**
     * Sets the {@link DeviceSpecific}.
     *
     * @param deviceSpecific the device specific properties.
     * @return the current TakePictureCustomizer.
     */
    public TakePictureCustomizer setDeviceSpecific(@NonNull DeviceSpecific deviceSpecific) {
        mDeviceSpecific = deviceSpecific;
        return this;
    }

    /**
     * Returns the {@link DeviceSpecific}.
     *
     * @return the DeviceSpecific.
     */
    @NonNull
    public DeviceSpecific getDeviceSpecific() {
        return mDeviceSpecific;
    }

    /**
     * Sets the {@link ImageCapture.CaptureMode}.
     *
     * @param captureMode the CaptureMode.
     * @return the current TakePictureCustomizer.
     */
    public TakePictureCustomizer setCaptureMode(@NonNull ImageCapture.CaptureMode captureMode) {
        mCaptureMode = captureMode;
        return this;
    }

    /**
     * Returns the {@link ImageCapture.CaptureMode}.
     *
     * @return the CaptureMode.
     */
    @NonNull
    public ImageCapture.CaptureMode getCaptureMode() {
        return mCaptureMode;
    }

    /**
     * According to current settings, appends special setting to the input CaptureConfig.
     *
     * @param builder the CaptureConfig.Builder.
     */
    public abstract void apply(@NonNull CaptureConfig.Builder builder);

    /** Factory for producing the TakePictureCustomizer. */
    public abstract static class Factory {

        /**
         * Produces the TakePictureCustomizer.
         *
         * @return the TakePictureCustomizer.
         */
        @NonNull
        protected abstract TakePictureCustomizer create();
    }

    /** A default empty implementation. */
    public static Factory getDefaultEmptyFactory() {
        return new Factory() {
            @NonNull
            @Override
            protected TakePictureCustomizer create() {
                return new TakePictureCustomizer() {
                    @Override
                    public void apply(@NonNull CaptureConfig.Builder builder) {
                        // Do nothing
                    }
                };
            }
        };
    }
}
