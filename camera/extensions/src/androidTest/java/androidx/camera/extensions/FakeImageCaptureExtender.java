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

import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.impl.FakeImageCaptureExtenderImpl;

public class FakeImageCaptureExtender extends ImageCaptureExtender {
    /**
     * Create a new instance of the Fake extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     *                {@link androidx.camera.core.ImageCapture}.
     */
    public static FakeImageCaptureExtender create(ImageCaptureConfig.Builder builder) {
        try {
            return new VendorFakeImageCaptureExtender(builder);
        } catch (NoClassDefFoundError e) {
            return new DefaultFakeImageCaptureExtender();
        }
    }

    /** Empty implementation of Fake extender which does nothing. */
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

    /** Fake extender that calls into the vendor provided implementation. */
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
