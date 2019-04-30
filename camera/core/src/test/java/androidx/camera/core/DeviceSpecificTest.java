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

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DeviceSpecificTest {
    private static final String MANUFACTURE_GOOGLE = "Google";
    private static final String MANUFACTURE_NOT_GOOGLE = "ANY";
    private static final String MODEL_PIXEL_2 = "Pixel 2";
    private static final String MODEL_PIXEL_3 = "Pixel 3";
    private static final String MODEL_NOT_SUPPORT_HDR = "ANY";
    private static final int API_LEVEL_21 = 21;

    private static final DeviceProperties PROPERTIES_PIXEL_2 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_21);

    private static final DeviceProperties PROPERTIES_PIXEL_3 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_3,
            API_LEVEL_21);

    private static final DeviceProperties PROPERTIES_NOT_GOOGLE = DeviceProperties.create(
            MANUFACTURE_NOT_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_21);

    private static final DeviceProperties PROPERTIES_NOT_SUPPORT_MODEL = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_NOT_SUPPORT_HDR,
            API_LEVEL_21);

    @Test
    public void hasPixelHdrPlus_Pixel2() {
        DeviceSpecific deviceSpecific = new DeviceSpecific(PROPERTIES_PIXEL_2);
        assertThat(deviceSpecific.hasPixelHdrPlus()).isTrue();
    }

    @Test
    public void hasPixelHdrPlus_Pixel3() {
        DeviceSpecific deviceSpecific = new DeviceSpecific(PROPERTIES_PIXEL_3);
        assertThat(deviceSpecific.hasPixelHdrPlus()).isTrue();
    }

    @Test
    public void hasPixelHdrPlus_NotGoogle() {
        DeviceSpecific deviceSpecific = new DeviceSpecific(PROPERTIES_NOT_GOOGLE);
        assertThat(deviceSpecific.hasPixelHdrPlus()).isFalse();
    }

    @Test
    public void hasPixelHdrPlus_NotSupportModel() {
        DeviceSpecific deviceSpecific = new DeviceSpecific(PROPERTIES_NOT_SUPPORT_MODEL);
        assertThat(deviceSpecific.hasPixelHdrPlus()).isFalse();
    }
}
