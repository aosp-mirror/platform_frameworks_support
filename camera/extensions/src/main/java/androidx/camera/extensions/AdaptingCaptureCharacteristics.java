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

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.camera.core.CaptureCharacteristics;
import androidx.camera.extensions.impl.CaptureCharacteristicsImpl;

/** A {@link CaptureCharacteristics} that calls a vendor provided implementation. */
final class AdaptingCaptureCharacteristics implements CaptureCharacteristics {

    private final CaptureCharacteristicsImpl mImpl;
    private final Class<?> mKlass;
    private final String mCameraId;
    private final CameraCharacteristics mCameraCharacteristics;

    AdaptingCaptureCharacteristics(CaptureCharacteristicsImpl impl, Class<?> klass, String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        mImpl = impl;
        mKlass = klass;
        mCameraId = cameraId;
        mCameraCharacteristics = cameraCharacteristics;
    }

    /**
     * Gets customized capture characteristics value
     *
     * @param key The key of the capture characteristics field to read.
     * @return The value of that key, or null if the field is not set.
     */
    @Override
    public <T> T get(@NonNull String key) {
        return mImpl.get(mKlass, mCameraId, mCameraCharacteristics, key);
    }
}
