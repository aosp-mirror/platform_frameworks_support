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
import static org.mockito.Mockito.when;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeviceSpecific;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.TakePictureCustomizer;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2TakePictureCustomizerTest {

    private Camera2TakePictureCustomizer.Factory mFactory;
    private TakePictureCustomizer mTakePictureCustomizer;
    private CaptureConfig.Builder mConfigBuilder;

    @Before
    public void setUp() {
        mFactory = new Camera2TakePictureCustomizer.Factory();
        mTakePictureCustomizer = mFactory.create();
        mConfigBuilder = new CaptureConfig.Builder();
    }

    @Test
    public void applyTemplateTypeStillCapture() {
        mTakePictureCustomizer.apply(mConfigBuilder);
        CaptureConfig config = mConfigBuilder.build();
        assertThat(config.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void setCaptureMode_noPixelHdrPlus_enableZslIsFalse() {
        DeviceSpecific deviceSpecific = mock(DeviceSpecific.class);
        when(deviceSpecific.hasPixelHdrPlus()).thenReturn(false);

        mTakePictureCustomizer
                .setDeviceSpecific(deviceSpecific)
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .apply(mConfigBuilder);

        CaptureConfig config = mConfigBuilder.build();
        Camera2Config camera2Config = new Camera2Config(config.getImplementationOptions());
        assertThat(camera2Config
                .getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void setMinLatency_enableZslIsFalse() {
        DeviceSpecific deviceSpecific = mock(DeviceSpecific.class);
        when(deviceSpecific.hasPixelHdrPlus()).thenReturn(true);

        mTakePictureCustomizer
                .setDeviceSpecific(deviceSpecific)
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .apply(mConfigBuilder);

        CaptureConfig config = mConfigBuilder.build();
        Camera2Config camera2Config = new Camera2Config(config.getImplementationOptions());
        assertThat(camera2Config
                .getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(false);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void setMaxQuality_enableZslIsTrue() {
        DeviceSpecific deviceSpecific = mock(DeviceSpecific.class);
        when(deviceSpecific.hasPixelHdrPlus()).thenReturn(true);

        mTakePictureCustomizer
                .setDeviceSpecific(deviceSpecific)
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .apply(mConfigBuilder);

        CaptureConfig config = mConfigBuilder.build();
        Camera2Config camera2Config = new Camera2Config(config.getImplementationOptions());
        assertThat(camera2Config
                .getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(true);
    }
}
