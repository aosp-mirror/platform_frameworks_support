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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraX;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
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
public class CameraUtilTest {
    private static final String CAMERA_ID = "0";
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    private static FakeCameraFactory sCameraFactory = new FakeCameraFactory();
    private CameraCharacteristics mCharacteristics;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(sCameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);
        CameraX.init(context, appConfigBuilder.build());

        mCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(mCharacteristics);

        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING,
                lensFacingToLensFacingInteger(LENS_FACING));

        ((ShadowCameraManager)
                Shadow.extract(context.getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA_ID, mCharacteristics);
    }

    @Test
    public void canGetCameraId() {
        String cameraId = CameraUtil.getCameraId(LENS_FACING);
        CameraCharacteristics characteristics = CameraUtil.getCameraCharacteristics(cameraId);
        CameraX.LensFacing lensFacing = lensFacingIntegerToLensFacing(
                characteristics.get(CameraCharacteristics.LENS_FACING));
        assertThat(cameraId).isEqualTo(CAMERA_ID);
        assertThat(lensFacing).isEqualTo(LENS_FACING);
    }

    @Test
    public void canGetCameraCharacteristics() {
        assertThat(CameraUtil.getCameraCharacteristics(CAMERA_ID)).isEqualTo(mCharacteristics);
    }

    @NonNull
    private CameraX.LensFacing lensFacingIntegerToLensFacing(@Nullable Integer lensFacingInteger) {
        if (lensFacingInteger == null) {
            throw new IllegalArgumentException("Lens facing integer can't be null.");
        }
        return lensFacingInteger == CameraCharacteristics.LENS_FACING_BACK ? CameraX.LensFacing.BACK
                : CameraX.LensFacing.FRONT;
    }

    @NonNull
    private Integer lensFacingToLensFacingInteger(CameraX.LensFacing lensFacing) {
        return lensFacing == CameraX.LensFacing.BACK ? CameraCharacteristics.LENS_FACING_BACK
                : CameraCharacteristics.LENS_FACING_FRONT;
    }
}
