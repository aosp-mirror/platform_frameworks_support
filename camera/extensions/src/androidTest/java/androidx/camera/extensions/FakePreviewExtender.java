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


import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.FakePreviewExtenderImpl;

/**
 * Load the OEM extension Preview implementation for fake effect type.
 */
public class FakePreviewExtender extends PreviewExtender {
    private static final String TAG = "FakePreviewExtender";

    /**
     * Create a new instance of the fake extender.
     *
     * @param builder Builder that will be used to create the configurations for the
     * {@link androidx.camera.core.Preview}.
     */
    public static FakePreviewExtender create(PreviewConfig.Builder builder) {
        try {
            return new VendorFakePreviewExtender(builder);
        } catch (NoClassDefFoundError e) {
            return new DefaultFakePreviewExtender();
        }
    }

    /** Empty implementation of fake extender which does nothing. */
    static class DefaultFakePreviewExtender extends FakePreviewExtender {
        DefaultFakePreviewExtender() {
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
    static class VendorFakePreviewExtender extends FakePreviewExtender {
        private final FakePreviewExtenderImpl mImpl;

        VendorFakePreviewExtender(PreviewConfig.Builder builder) {
            mImpl = new FakePreviewExtenderImpl();
            init(builder, mImpl);
        }
    }

    private FakePreviewExtender() {}
}
