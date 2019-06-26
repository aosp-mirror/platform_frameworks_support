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
 * Provides the implementation of {@link CaptureCharacteristicsImpl}.
 */
public class CaptureCharacteristicsImpls {
    private static final CaptureCharacteristicsImpl sCaptureCharacteristicsImpl =
            new CaptureCharacteristicsImpl() {
                @Override
                public <T> T get(Class<?> klass, String cameraId,
                        CameraCharacteristics cameraCharacteristics, String key) {
                    throw new RuntimeException("Stub, replace with implementation.");
                }
            };

    public static CaptureCharacteristicsImpl getCaptureCharacteristicsImpl() {
        return sCaptureCharacteristicsImpl;
    }

    private CaptureCharacteristicsImpls() {
    }
}
