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

/**
 * Provides abstract methods that the OEM needs to implement to get customized capture
 * characteristics.
 */
public interface CaptureCharacteristicsImpl {
    /**
     * Gets customized capture characteristics value
     *
     * @param klass                 The class to query the capture characteristics for.
     * @param cameraId              The camera2 id string of the camera.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @param key                   The key of the capture characteristics field to read.
     * @return The value of that key, or null if the field is not set.
     */
    <T> T get(Class<?> klass, String cameraId, CameraCharacteristics cameraCharacteristics,
            String key);
}
