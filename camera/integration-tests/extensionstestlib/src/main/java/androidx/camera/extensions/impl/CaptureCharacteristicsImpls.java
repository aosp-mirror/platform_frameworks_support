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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides the implementation of {@link CaptureCharacteristicsImpl}.
 */
public class CaptureCharacteristicsImpls {
    static final List<Class<?>> sImageCaptureImplClasses = new ArrayList<>(
            Arrays.asList(AutoImageCaptureExtenderImpl.class,
                    BeautyImageCaptureExtenderImpl.class,
                    BokehImageCaptureExtenderImpl.class,
                    HdrImageCaptureExtenderImpl.class,
                    NightImageCaptureExtenderImpl.class));

    static final List<Class<?>> sPreviewImplClasses = new ArrayList<>(
            Arrays.asList(AutoPreviewExtenderImpl.class,
                    BeautyPreviewExtenderImpl.class,
                    BokehPreviewExtenderImpl.class,
                    HdrPreviewExtenderImpl.class,
                    NightPreviewExtenderImpl.class));

    private static final CaptureCharacteristicsImpl sCaptureCharacteristicsImpl =
            new VendorCaptureCharacteristicsImpl();

    @NonNull
    public static CaptureCharacteristicsImpl getCaptureCharacteristicsImpl() {
        return sCaptureCharacteristicsImpl;
    }

    private CaptureCharacteristicsImpls() {
    }

    static class VendorCaptureCharacteristicsImpl implements CaptureCharacteristicsImpl {
        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(@NonNull Class<?> klass, @NonNull String cameraId,
                @NonNull CameraCharacteristics cameraCharacteristics, @NonNull String key) {
            // The sample implementation directly retrieve original characteristics value to return
            if (key.equals(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP.getName())) {
                if (!sImageCaptureImplClasses.contains(klass) && !sPreviewImplClasses.contains(
                        klass)) {
                    return null;
                }

                List<Pair<Integer, Size[]>> formatResolutionPairList =
                        new ArrayList<>();

                StreamConfigurationMap map = cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (sImageCaptureImplClasses.contains(klass)) {
                    formatResolutionPairList.add(Pair.create(ImageFormat.JPEG,
                            map.getOutputSizes(ImageFormat.JPEG)));
                    formatResolutionPairList.add(Pair.create(ImageFormat.YUV_420_888,
                            map.getOutputSizes(ImageFormat.YUV_420_888)));

                } else if (sPreviewImplClasses.contains(klass)) {
                    formatResolutionPairList.add(Pair.create(ImageFormat.PRIVATE,
                            map.getOutputSizes(ImageFormat.PRIVATE)));
                }
                return (T) formatResolutionPairList;
            } else if (key.equals(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM.getName())) {
                return (T) cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            } else if (key.equals(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES.getName())) {
                return (T) cameraCharacteristics.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            }
            return null;
        }
    }
}
