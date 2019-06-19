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

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCaptureConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ImageCaptureExtenderTest {
    private static final String CAMERA_ID = "0";
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    private ImageCaptureConfig.Builder mBuilder = new ImageCaptureConfig.Builder();
    private ImageCaptureExtender mImageCaptureExtender;
    private FakeImageCaptureExtenderImpl mFakeImageCaptureExtenderImpl =
            new FakeImageCaptureExtenderImpl();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraX.init(context, appConfig);

        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);

        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING,
                LENS_FACING == CameraX.LensFacing.BACK ? CameraCharacteristics.LENS_FACING_BACK
                        : CameraCharacteristics.LENS_FACING_FRONT);

        ((ShadowCameraManager)
                Shadow.extract(context.getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA_ID, characteristics);

        mBuilder.setLensFacing(LENS_FACING);
        mImageCaptureExtender = FakeImageCaptureExtender.create(mBuilder);
        mImageCaptureExtender.init(mBuilder, mFakeImageCaptureExtenderImpl);
    }

    @Test
    public void canCheckIsExtensionAvailable() {
        // Extension availability is defaulted to true
        assertThat(mImageCaptureExtender.isExtensionAvailable()).isTrue();
        // Deactivate extension
        mFakeImageCaptureExtenderImpl.setExtensionAvailable(false);
        assertThat(mImageCaptureExtender.isExtensionAvailable()).isFalse();
    }

    @Test
    public void canEnableExtension() {
        mImageCaptureExtender.enableExtension();
        ImageCaptureConfig config = mBuilder.build();
        assertThat(config.getCaptureProcessor()).isEqualTo(
                mFakeImageCaptureExtenderImpl.getCaptureProcessor());
        assertThat(config.getMaxCaptureStages()).isEqualTo(
                mFakeImageCaptureExtenderImpl.getMaxCaptureStage());
        assertThat(config.getCaptureBundle().getCaptureStages()).isEqualTo(
                mFakeImageCaptureExtenderImpl.getCaptureStages());
    }
}
