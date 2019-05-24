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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.util.Rational;
import android.view.Surface;

import androidx.camera.camera2.impl.ImageAnalysisConfigProvider;
import androidx.camera.camera2.impl.ImageCaptureConfigProvider;
import androidx.camera.camera2.impl.PreviewConfigProvider;
import androidx.camera.camera2.impl.VideoCaptureConfigProvider;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCaptureConfig;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DefaultConfigProviderTest {
    private static final Rational DEFAULT_ASPECT_RATIO_4_3 = new Rational(4, 3);
    private static final Rational DEFAULT_ASPECT_RATIO_3_4 = new Rational(3, 4);
    private static final Rational DEFAULT_ASPECT_RATIO_16_9 = new Rational(16, 9);
    private static final Rational DEFAULT_ASPECT_RATIO_9_16 = new Rational(9, 16);

    private final int[] mSupportedRotations =
            new int[]{Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
                    Surface.ROTATION_270};

    private CameraFactory mCameraFactory;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        AppConfig appConfig = Camera2AppConfig.create(mContext);
        mCameraFactory = appConfig.getCameraFactory(/*valueIfMissing=*/ null);
    }

    @Test
    public void checkImageAnalysisDefaultConfig() {
        for (int targetRotation : mSupportedRotations) {
            ImageAnalysisConfigProvider imageAnalysisConfigProvider =
                    new ImageAnalysisConfigProvider(mCameraFactory);
            ImageAnalysisConfig imageAnalysisConfig = imageAnalysisConfigProvider.getConfig(null,
                    targetRotation);

            int resultRotation = imageAnalysisConfig.getTargetRotation();

            // Checks the result rotation is the same as the one set to get default config
            assertThat(resultRotation).isEqualTo(targetRotation);

            // ImageAnalysis's default aspect ratio is 3:4 or 4:3 depending on display rotation
            Rational targetAspectRatio =
                    (targetRotation == Surface.ROTATION_0 || targetRotation == Surface.ROTATION_180)
                            ? DEFAULT_ASPECT_RATIO_3_4 : DEFAULT_ASPECT_RATIO_4_3;
            Rational resultAspectRatio = imageAnalysisConfig.getTargetAspectRatio();

            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio);
        }
    }

    @Test
    public void checkImageCaptureDefaultConfig() {
        for (int targetRotation : mSupportedRotations) {
            ImageCaptureConfigProvider imageCaptureConfigProvider = new ImageCaptureConfigProvider(
                    mCameraFactory);
            ImageCaptureConfig imageCaptureConfig = imageCaptureConfigProvider.getConfig(null,
                    targetRotation);

            int resultRotation = imageCaptureConfig.getTargetRotation();

            // Checks the result rotation is the same as the one set to get default config
            assertThat(resultRotation).isEqualTo(targetRotation);

            // ImageCapture's default aspect ratio is 3:4 or 4:3 depending on display rotation
            Rational targetAspectRatio =
                    (targetRotation == Surface.ROTATION_0 || targetRotation == Surface.ROTATION_180)
                            ? DEFAULT_ASPECT_RATIO_3_4 : DEFAULT_ASPECT_RATIO_4_3;
            Rational resultAspectRatio = imageCaptureConfig.getTargetAspectRatio();

            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio);
        }
    }

    @Test
    public void checkPreviewDefaultConfig() {
        for (int targetRotation : mSupportedRotations) {
            PreviewConfigProvider previewConfigProvider = new PreviewConfigProvider(mCameraFactory);
            PreviewConfig previewConfig = previewConfigProvider.getConfig(null, targetRotation);

            int resultRotation = previewConfig.getTargetRotation();

            // Checks the result rotation is the same as the one set to get default config
            assertThat(resultRotation).isEqualTo(targetRotation);

            // Preview have no default aspect ratio, therefore, the input aspect ratio will be
            // returned.
            Rational targetAspectRatio = new Rational(1, 1);
            Rational resultAspectRatio = previewConfig.getTargetAspectRatio(targetAspectRatio);

            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio);
        }
    }

    @Test
    public void checkVideoCaptureDefaultConfig() {
        for (int targetRotation : mSupportedRotations) {
            VideoCaptureConfigProvider videoCaptureConfigProvider = new VideoCaptureConfigProvider(
                    mCameraFactory);
            VideoCaptureConfig videoCaptureConfig = videoCaptureConfigProvider.getConfig(null,
                    targetRotation);

            int resultRotation = videoCaptureConfig.getTargetRotation();

            // Checks the result rotation is the same as the one set to get default config
            assertThat(resultRotation).isEqualTo(targetRotation);

            // VideoCapture's default aspect ratio is 3:4 or 4:3 depending on display rotation
            Rational targetAspectRatio =
                    (targetRotation == Surface.ROTATION_0 || targetRotation == Surface.ROTATION_180)
                            ? DEFAULT_ASPECT_RATIO_9_16 : DEFAULT_ASPECT_RATIO_16_9;
            Rational resultAspectRatio = videoCaptureConfig.getTargetAspectRatio();

            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio);
        }
    }

}
