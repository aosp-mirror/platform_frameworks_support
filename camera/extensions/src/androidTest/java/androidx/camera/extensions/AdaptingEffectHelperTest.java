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


import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link androidx.camera.extensions.AdaptingEffectHelper}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class AdaptingEffectHelperTest {
    @Test
    public void canApplyImageCaptureEffectConfig() {
        AdaptingEffectHelper adaptingEffectHelper = new AdaptingEffectHelper();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            ImageCaptureConfig.Builder builder = new ImageCaptureConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectHelper.applyEffectConfig(builder, fakeLifecycleOwner);
            EffectMode resultEffectMode = builder.getMutableConfig().retrieveOption(
                    ImageCaptureExtender.OPTION_IMAGE_CAPTURE_EXTENDER_MODE, null);
            if (effectMode == EffectMode.NORMAL) {
                assertThat(resultEffectMode).isNull();
            } else {
                assertThat(resultEffectMode).isEqualTo(effectMode);
            }
        }
    }

    @Test
    public void canApplyPreviewEffectConfig() {
        AdaptingEffectHelper adaptingEffectHelper = new AdaptingEffectHelper();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            PreviewConfig.Builder builder = new PreviewConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectHelper.applyEffectConfig(builder, fakeLifecycleOwner);
            EffectMode resultEffectMode = builder.getMutableConfig().retrieveOption(
                    PreviewExtender.OPTION_PREVIEW_EXTENDER_MODE, null);
            if (effectMode == EffectMode.NORMAL) {
                assertThat(resultEffectMode).isNull();
            } else {
                assertThat(resultEffectMode).isEqualTo(effectMode);
            }
        }
    }
}
