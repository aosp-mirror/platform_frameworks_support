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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;

import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfo;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashSet;
import java.util.Set;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class LensFacingCameraIdFilterTest {
    private FakeCameraFactory mCameraFactory = new FakeCameraFactory();
    private Set<String> mCameraIds = new HashSet<>();
    private String mCameraId0 = "0";
    private String mCameraId1 = "1";

    @Before
    public void setUp() {
        mCameraIds.add(mCameraId0);
        mCameraIds.add(mCameraId1);
        mCameraFactory.insertCamera(mCameraId0,
                new FakeCamera(new FakeCameraInfo(0, CameraX.LensFacing.BACK),
                        mock(CameraControlInternal.class)));
        mCameraFactory.insertCamera(mCameraId1,
                new FakeCamera(new FakeCameraInfo(0, CameraX.LensFacing.FRONT),
                        mock(CameraControlInternal.class)));

        Context context = ApplicationProvider.getApplicationContext();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                new ConfigProvider<FakeUseCaseConfig>() {
                    @Override
                    public FakeUseCaseConfig getConfig(CameraX.LensFacing lensFacing) {
                        return new FakeUseCaseConfig.Builder().build();
                    }
                });
        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(mCameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        CameraX.init(context, appConfigBuilder.build());
    }

    @Test
    public void canFilterBackCamera() {
        LensFacingCameraIdFilter lensFacingCameraIdFilter =
                mCameraFactory.getLensFacingCameraIdFilter(CameraX.LensFacing.BACK);
        mCameraIds = lensFacingCameraIdFilter.filter(mCameraIds);
        assertThat(mCameraIds).contains(mCameraId0);
        assertThat(mCameraIds).doesNotContain(mCameraId1);
    }

    @Test
    public void canFilterFrontCamera() {
        LensFacingCameraIdFilter lensFacingCameraIdFilter =
                mCameraFactory.getLensFacingCameraIdFilter(CameraX.LensFacing.FRONT);
        mCameraIds = lensFacingCameraIdFilter.filter(mCameraIds);
        assertThat(mCameraIds).contains(mCameraId1);
        assertThat(mCameraIds).doesNotContain(mCameraId0);
    }
}
