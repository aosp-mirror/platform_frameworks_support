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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.concurrent.ScheduledExecutorService;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraControlZoomTest {

    private CameraControlInternal.ControlUpdateListener mControlUpdateListener;

    private CameraManager mCameraManager;
    private CameraControl mCameraControl;
    private CameraControl mCameraControl01;

    private Handler mHandler;
    private static final String CAMERAID_0 = "0";
    private static final String CAMERAID_1 = "1";
    private static final float MAX_ZOOM = 8.0f;
    private static final int SENSOR_WIDTH = 640;
    private static final int SENSOR_HEIGHT = 480;

    @Before
    public void setUp() {
        initCameraCharacteristics();
        mCameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        mHandler = new Handler();
        mControlUpdateListener = mock(CameraControlInternal.ControlUpdateListener.class);
        ScheduledExecutorService executorService = CameraXExecutors.newHandlerExecutor(mHandler);

        mCameraControl = new Camera2CameraControl(mCameraManager,
                CAMERAID_0,
                mControlUpdateListener, executorService, executorService);

        mCameraControl01 = new Camera2CameraControl(mCameraManager,
                CAMERAID_1,
                mControlUpdateListener, executorService, executorService);

        reset(mControlUpdateListener);
    }

    private void initCameraCharacteristics() {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCameraCharacteristics = Shadow.extract(characteristics);
        shadowCameraCharacteristics.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
                MAX_ZOOM);
        shadowCameraCharacteristics.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        ((ShadowCameraManager) Shadow.extract(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE)))
                .addCamera(CAMERAID_0, characteristics);

        CameraCharacteristics characteristics01 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCameraCharacteristics01 = Shadow.extract(
                characteristics01);
        shadowCameraCharacteristics01.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
                null);
        shadowCameraCharacteristics01.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT));
        ((ShadowCameraManager) Shadow.extract(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE)))
                .addCamera(CAMERAID_1, characteristics01);
    }

    private Rect getSensorRect() {
        return new Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT);
    }

    @Test
    public void setZoomAndGetZoom() {
        mCameraControl.setZoom(5.0f);
        float multiplier = mCameraControl.getZoom();
        assertThat(multiplier).isEqualTo(5.0f);
    }

    @Test
    public void setZoomLargerThanMax() {
        float maxZoom = mCameraControl.getMaxZoom();
        mCameraControl.setZoom(maxZoom + 1.0f);
        float multiplier = mCameraControl.getZoom();
        assertThat(multiplier).isEqualTo(maxZoom);
    }

    @Test
    public void setZoomAs1_CropRegionIsSensorRect() throws InterruptedException {
        mCameraControl.setZoom(1.0f);

        ArgumentCaptor<SessionConfig> sessionConfigArgumentCaptor =
                ArgumentCaptor.forClass(SessionConfig.class);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                sessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = sessionConfigArgumentCaptor.getValue();
        Camera2Config repeatingConfig = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(repeatingConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(getSensorRect());

        Camera2Config singleConfig = new Camera2Config(
                ((Camera2CameraControl) mCameraControl).getSharedOptions());
        assertThat(singleConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(getSensorRect());
    }

    @Test
    public void setZoomAs2_CropRegionIsHalfOfSensorRect() throws InterruptedException {
        mCameraControl.setZoom(2.0f);

        ArgumentCaptor<SessionConfig> sessionConfigArgumentCaptor =
                ArgumentCaptor.forClass(SessionConfig.class);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                sessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = sessionConfigArgumentCaptor.getValue();
        Camera2Config repeatingConfig = new Camera2Config(sessionConfig.getImplementationOptions());

        int cropX = SENSOR_WIDTH / 4;
        int cropY = SENSOR_HEIGHT / 4;
        Rect cropRect = new Rect(cropX, cropY, cropX + SENSOR_WIDTH / 2,
                cropY + SENSOR_HEIGHT / 2);
        assertThat(repeatingConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(cropRect);

        Camera2Config singleConfig = new Camera2Config(
                ((Camera2CameraControl) mCameraControl).getSharedOptions());
        assertThat(singleConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(cropRect);
    }

    @Test
    public void setZoomSmallerThanMin() {
        float minZoom = mCameraControl.getMinZoom();
        mCameraControl.setZoom(minZoom - 2.0f);
        float multiplier = mCameraControl.getZoom();
        assertThat(multiplier).isEqualTo(minZoom);
    }

    @Test
    public void getMaxZoom() {
        float max = mCameraControl.getMaxZoom();
        assertThat(max).isEqualTo(MAX_ZOOM);
    }

    @Test
    public void getMaxZoomNotSupported() {
        float max = mCameraControl01.getMaxZoom();
        assertThat(max).isEqualTo(ZoomControl.UNSUPPORTED_ZOOM);
    }

    @Test
    public void getMinZoom() {
        float min = mCameraControl01.getMinZoom();
        assertThat(min).isEqualTo(ZoomControl.MIN_ZOOM);
    }
}
