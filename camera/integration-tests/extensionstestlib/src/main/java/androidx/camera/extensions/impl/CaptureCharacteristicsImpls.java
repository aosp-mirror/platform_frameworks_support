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

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the implementation of {@link CaptureCharacteristicsImpl}.
 */
public class CaptureCharacteristicsImpls {
    private static final CaptureCharacteristicsImpl sCaptureCharacteristicsImpl =
            new CaptureCharacteristicsImpl() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(Class<?> klass, String cameraId,
                        CameraCharacteristics cameraCharacteristics, String key) {
                    if (BokehImageCaptureExtenderImpl.class == klass
                            || HdrImageCaptureExtenderImpl.class == klass) {
                        if (key.equals(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP.toString())) {
                            List<Pair<Integer, Size[]>> formatResolutionPairList =
                                    new ArrayList<>();

                            // The sample implementation directly retrieve supported sizes from
                            // StreamConfigurationMap and transform to Pair list.
                            StreamConfigurationMap map = cameraCharacteristics.get(
                                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                            formatResolutionPairList.add(Pair.create(ImageFormat.JPEG,
                                    map.getOutputSizes(ImageFormat.JPEG)));
                            formatResolutionPairList.add(Pair.create(ImageFormat.YUV_420_888,
                                    map.getOutputSizes(ImageFormat.YUV_420_888)));

                            return (T) formatResolutionPairList;
                        }
                    }
                    return null;
                }
            };

    public static CaptureCharacteristicsImpl getCaptureCharacteristicsImpl() {
        return sCaptureCharacteristicsImpl;
    }

    private CaptureCharacteristicsImpls() {
    }
}
