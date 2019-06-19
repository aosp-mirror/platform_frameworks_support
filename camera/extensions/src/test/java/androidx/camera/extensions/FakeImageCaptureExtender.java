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

import android.util.Log;

import androidx.camera.core.ImageCaptureConfig;

/**
 * A fake implementation of {@link ImageCaptureExtender}.
 */
class FakeImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "FakeImageCaptureExtender";

    static FakeImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        try {
            return new FakeImageCaptureExtender.VendorFakeImageCaptureExtender(builder);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "No image capture extender found. Falling back to default.");
        }

        return new FakeImageCaptureExtender.DefaultFakeImageCaptureExtender();
    }

    static class DefaultFakeImageCaptureExtender extends FakeImageCaptureExtender {
        DefaultFakeImageCaptureExtender() {
        }

        @Override
        public boolean isExtensionAvailable() {
            return false;
        }

        @Override
        public void enableExtension() {
        }
    }

    static class VendorFakeImageCaptureExtender extends FakeImageCaptureExtender {
        private final FakeImageCaptureExtenderImpl mImpl;

        VendorFakeImageCaptureExtender(ImageCaptureConfig.Builder builder) {
            mImpl = new FakeImageCaptureExtenderImpl();
            init(builder, mImpl);
        }
    }

    private FakeImageCaptureExtender() {
    }
}
