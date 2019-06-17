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

import static androidx.camera.extensions.ImageCaptureExtender.OPTION_IMAGE_CAPTURE_EXTENDER_MODE;
import static androidx.camera.extensions.PreviewExtender.OPTION_PREVIEW_EXTENDER_MODE;

import static com.google.common.truth.Truth.assertThat;


import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageInfoProcessor;
import androidx.camera.core.MutableConfig;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link AdaptingEffectEnabler}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class AdaptingEffectEnablerTest {
    @Test
    public void canApplyImageCaptureEffectConfig() {
        AdaptingEffectEnabler
                adaptingEffectEnabler = new AdaptingEffectEnabler();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            if (effectMode == EffectMode.NORMAL) {
                continue;
            }

            ImageCaptureConfig.Builder builder = new ImageCaptureConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);
            EffectMode resultEffectMode = builder.getMutableConfig().retrieveOption(
                    OPTION_IMAGE_CAPTURE_EXTENDER_MODE, null);
            assertThat(resultEffectMode).isEqualTo(effectMode);
        }
    }

    @Test
    public void canRemoveImageCaptureEffectConfig() {
        AdaptingEffectEnabler
                adaptingEffectEnabler = new AdaptingEffectEnabler();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            if (effectMode == EffectMode.NORMAL) {
                continue;
            }

            ImageCaptureConfig.Builder builder = new ImageCaptureConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);

            // Set effect mode and apply effect configs
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);

            MutableConfig config = builder.getMutableConfig();
            CaptureProcessor captureProcessor = config.retrieveOption(
                    ImageCaptureConfig.OPTION_CAPTURE_PROCESSOR, null);
            assertThat(captureProcessor).isNotNull();
            Integer maxCaptureStages = config.retrieveOption(
                    ImageCaptureConfig.OPTION_MAX_CAPTURE_STAGES, null);
            assertThat(maxCaptureStages).isNotNull();
            UseCase.EventListener eventListener = config.retrieveOption(
                    ImageCaptureConfig.OPTION_USE_CASE_EVENT_LISTENER, null);
            assertThat(eventListener).isNotNull();
            CaptureBundle captureBundle = config.retrieveOption(
                    ImageCaptureConfig.OPTION_CAPTURE_BUNDLE, null);
            assertThat(captureBundle).isNotNull();
            EffectMode effectModeResult = config.retrieveOption(OPTION_IMAGE_CAPTURE_EXTENDER_MODE,
                    null);
            assertThat(effectModeResult).isEqualTo(effectMode);

            // Set effect mode back to EffectMode.NORMAL and remove effect configs
            ExtensionsManager.enableExtension(EffectMode.NORMAL, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);

            // Check that effect configs have been removed
            config = builder.getMutableConfig();
            captureProcessor = config.retrieveOption(ImageCaptureConfig.OPTION_CAPTURE_PROCESSOR,
                    null);
            assertThat(captureProcessor).isNull();
            maxCaptureStages = config.retrieveOption(ImageCaptureConfig.OPTION_MAX_CAPTURE_STAGES,
                    null);
            assertThat(maxCaptureStages).isNull();
            eventListener = config.retrieveOption(ImageCaptureConfig.OPTION_USE_CASE_EVENT_LISTENER,
                    null);
            assertThat(eventListener).isNull();
            captureBundle = config.retrieveOption(ImageCaptureConfig.OPTION_CAPTURE_BUNDLE, null);
            assertThat(captureBundle).isNull();
            effectModeResult = config.retrieveOption(OPTION_IMAGE_CAPTURE_EXTENDER_MODE, null);
            assertThat(effectModeResult).isNull();
        }
    }

    @Test
    public void canApplyPreviewEffectConfig() {
        AdaptingEffectEnabler
                adaptingEffectEnabler = new AdaptingEffectEnabler();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            if (effectMode == EffectMode.NORMAL) {
                continue;
            }

            PreviewConfig.Builder builder = new PreviewConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);
            EffectMode resultEffectMode = builder.getMutableConfig().retrieveOption(
                    OPTION_PREVIEW_EXTENDER_MODE, null);
            assertThat(resultEffectMode).isEqualTo(effectMode);
        }
    }

    @Test
    public void canRemovePreviewEffectConfig() {
        AdaptingEffectEnabler
                adaptingEffectEnabler = new AdaptingEffectEnabler();
        FakeLifecycleOwner fakeLifecycleOwner = new FakeLifecycleOwner();

        for (EffectMode effectMode : EffectMode.values()) {
            if (effectMode == EffectMode.NORMAL) {
                continue;
            }

            PreviewConfig.Builder builder = new PreviewConfig.Builder().setLensFacing(
                    CameraX.LensFacing.BACK);

            // Set effect mode and apply effect configs
            ExtensionsManager.enableExtension(effectMode, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);

            // Check that effect configs have been applied
            MutableConfig config = builder.getMutableConfig();
            UseCase.EventListener eventListener = config.retrieveOption(
                    PreviewConfig.OPTION_USE_CASE_EVENT_LISTENER, null);
            assertThat(eventListener).isNotNull();
            EffectMode effectModeResult = config.retrieveOption(OPTION_PREVIEW_EXTENDER_MODE, null);
            assertThat(effectModeResult).isEqualTo(effectMode);

            // Set effect mode back to EffectMode.NORMAL and remove effect configs
            ExtensionsManager.enableExtension(EffectMode.NORMAL, fakeLifecycleOwner);
            adaptingEffectEnabler.applyEffectConfig(builder, fakeLifecycleOwner);

            // Check that effect configs have been removed
            config = builder.getMutableConfig();
            ImageInfoProcessor imageInfoProcessor = config.retrieveOption(
                    PreviewConfig.IMAGE_INFO_PROCESSOR, null);
            assertThat(imageInfoProcessor).isNull();
            CaptureProcessor captureProcessor = config.retrieveOption(
                    PreviewConfig.OPTION_PREVIEW_CAPTURE_PROCESSOR, null);
            assertThat(captureProcessor).isNull();
            eventListener = config.retrieveOption(PreviewConfig.OPTION_USE_CASE_EVENT_LISTENER,
                    null);
            assertThat(eventListener).isNull();
            effectModeResult = config.retrieveOption(OPTION_PREVIEW_EXTENDER_MODE, null);
            assertThat(effectModeResult).isNull();
        }
    }
}
