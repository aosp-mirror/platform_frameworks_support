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

import androidx.camera.core.PreviewConfig;

/**
 * A fake implementation of {@link FakePreviewExtender}.
 */
class FakePreviewExtender extends PreviewExtender {
    private static final String TAG = "FakePreviewExtender";

    static FakePreviewExtender create(PreviewConfig.Builder builder) {
        try {
            return new FakePreviewExtender.VendorFakePreviewExtender(builder);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "No preview extender found. Falling back to default.");
        }

        return new FakePreviewExtender.DefaultFakePreviewExtender();
    }

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

    static class VendorFakePreviewExtender extends FakePreviewExtender {
        private final FakePreviewExtenderImpl mImpl;

        VendorFakePreviewExtender(PreviewConfig.Builder builder) {
            mImpl = new FakePreviewExtenderImpl();
            init(builder, mImpl, null);
        }
    }

    private FakePreviewExtender() {
    }
}
