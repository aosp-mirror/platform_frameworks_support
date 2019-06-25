/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.util.Log;

import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
/**
 * Load the OEM extension implementation for HDR effect type.
 */
public class HdrImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "HdrImageCaptureExtender";

    /**
     * Create a new instance of the HDR extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.ImageCapture}.
     */
    public static HdrImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        if (ExtensionVersion.isExtensionVersionSupported()) {
            try {
                return new VendorHdrImageCaptureExtender(builder);
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "No HDR image capture extender found. Falling back to default.");
            }
        }

        return new DefaultHdrImageCaptureExtender();
    }

    /** Empty implementation of HDR extender which does nothing. */
    static class DefaultHdrImageCaptureExtender extends HdrImageCaptureExtender {
        DefaultHdrImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable() {
            return false;
        }

        @Override
        public void enableExtension() {
        }
    }

    /** HDR extender that calls into the vendor provided implementation. */
    static class VendorHdrImageCaptureExtender extends HdrImageCaptureExtender {
        private final HdrImageCaptureExtenderImpl mImpl;

        VendorHdrImageCaptureExtender(ImageCaptureConfig.Builder builder) {
            mImpl = new HdrImageCaptureExtenderImpl();
            init(builder, mImpl);
        }
    }

    private HdrImageCaptureExtender() {}
}
