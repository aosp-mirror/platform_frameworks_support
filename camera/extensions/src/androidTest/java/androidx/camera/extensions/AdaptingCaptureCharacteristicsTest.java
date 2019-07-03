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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;
import android.util.Size;

import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.CaptureCharacteristicsImpl;
import androidx.camera.extensions.impl.CaptureCharacteristicsImpls;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
/**
 * Unit tests for {@link androidx.camera.extensions.AdaptingCaptureCharacteristics}.
 * */
public class AdaptingCaptureCharacteristicsTest {
    private String mCameraId;
    private CameraCharacteristics mCameraCharacteristics;
    private CaptureCharacteristicsImpl mCaptureCharacteristicsImpl;
    private static final List<Class<?>> sImageCaptureImplClasses = new ArrayList<>(
            Arrays.asList(AutoImageCaptureExtenderImpl.class,
                    BeautyImageCaptureExtenderImpl.class,
                    BokehImageCaptureExtenderImpl.class,
                    HdrImageCaptureExtenderImpl.class,
                    NightImageCaptureExtenderImpl.class));

    private static final List<Class<?>> sPreviewImplClasses = new ArrayList<>(
            Arrays.asList(AutoPreviewExtenderImpl.class,
                    BeautyPreviewExtenderImpl.class,
                    BokehPreviewExtenderImpl.class,
                    HdrPreviewExtenderImpl.class,
                    NightPreviewExtenderImpl.class));

    @Before
    public void setUp() throws CameraInfoUnavailableException, CameraAccessException {
        assumeTrue(CameraUtil.deviceHasCamera());

        mCameraId = CameraX.getCameraWithLensFacing(CameraX.LensFacing.BACK);
        Context context = ApplicationProvider.getApplicationContext();
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        mCameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
        mCaptureCharacteristicsImpl = CaptureCharacteristicsImpls.getCaptureCharacteristicsImpl();
    }

    @Test
    public void canGetSupportedResolutionForImageCaptureImplClasses() {
        for (Class<?> klass : sImageCaptureImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            StreamConfigurationMap map = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] oriJpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            Size[] oriYUVSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            Size[] resultJpegSizes = null;
            Size[] resultYUVSizes = null;

            List<Pair<Integer, Size[]>> formatResolutionPairList =
                    adaptingCaptureCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP.getName());

            // Test lib sample implementation returns same size list as result.
            for (Pair<Integer, Size[]> formatResolutionPair : formatResolutionPairList) {
                if (formatResolutionPair.first == ImageFormat.JPEG) {
                    resultJpegSizes = formatResolutionPair.second;
                } else if (formatResolutionPair.first == ImageFormat.YUV_420_888) {
                    resultYUVSizes = formatResolutionPair.second;
                }
            }

            assertThat(resultJpegSizes).isEqualTo(oriJpegSizes);
            assertThat(resultYUVSizes).isEqualTo(oriYUVSizes);
        }
    }

    @Test
    public void canGetSupportedResolutionForPreviewImplClasses() {
        for (Class<?> klass : sPreviewImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            StreamConfigurationMap map = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] oriPrivateSizes = map.getOutputSizes(ImageFormat.PRIVATE);
            Size[] resultPrivateSizes = null;

            List<Pair<Integer, Size[]>> formatResolutionPairList =
                    adaptingCaptureCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP.getName());

            // Test lib sample implementation returns same size list as result.
            for (Pair<Integer, Size[]> formatResolutionPair : formatResolutionPairList) {
                if (formatResolutionPair.first == ImageFormat.PRIVATE) {
                    resultPrivateSizes = formatResolutionPair.second;
                }
            }

            assertThat(resultPrivateSizes).isEqualTo(oriPrivateSizes);
        }
    }

    @Test
    public void canGetMaxZoomForImageCaptureImplClasses() {
        for (Class<?> klass : sImageCaptureImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            // Test lib sample implementation returns same max zoom value as result.
            Float oriMaxZoom = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            Float resultMaxZoom = adaptingCaptureCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM.getName());

            assertThat(resultMaxZoom).isEqualTo(oriMaxZoom);
        }
    }

    @Test
    public void canGetMaxZoomForPreviewImplClasses() {
        for (Class<?> klass : sPreviewImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            // Test lib sample implementation returns same max zoom value as result.
            Float oriMaxZoom = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            Float resultMaxZoom = adaptingCaptureCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM.getName());

            assertThat(resultMaxZoom).isEqualTo(oriMaxZoom);
        }
    }

    @Test
    public void canGetAvailAperturesForImageCaptureImplClasses() {
        for (Class<?> klass : sImageCaptureImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            // Test lib sample implementation returns same available apertures as result.
            float[] oriAvailApertures = mCameraCharacteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            float[] resultAvailApertures = adaptingCaptureCharacteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES.getName());

            assertThat(resultAvailApertures).isEqualTo(oriAvailApertures);
        }
    }

    @Test
    public void canGetAvailAperturesForPreviewImplClasses() {
        for (Class<?> klass : sPreviewImplClasses) {
            AdaptingCaptureCharacteristics adaptingCaptureCharacteristics =
                    new AdaptingCaptureCharacteristics(mCaptureCharacteristicsImpl, klass,
                            mCameraId, mCameraCharacteristics);

            // Test lib sample implementation returns same available apertures as result.
            float[] oriAvailApertures = mCameraCharacteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            float[] resultAvailApertures = adaptingCaptureCharacteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES.getName());

            assertThat(resultAvailApertures).isEqualTo(oriAvailApertures);
        }
    }
}
