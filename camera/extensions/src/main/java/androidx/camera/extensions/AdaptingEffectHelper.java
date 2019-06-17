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

import static androidx.camera.extensions.ExtensionsManager.EffectMode;

import androidx.annotation.NonNull;
import androidx.camera.core.EffectHelper;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

/**
 * The implementation of {@link EffectHelper} for applying effect config.
 */
final class AdaptingEffectHelper implements EffectHelper {
    AdaptingEffectHelper() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyEffectConfig(@NonNull ImageCaptureConfig.Builder builder,
            @NonNull LifecycleOwner lifecycleOwner) {
        ImageCaptureExtender imageCaptureExtender = null;
        EffectMode effectMode = ExtensionsManager.getEffectMode(lifecycleOwner);

        switch (effectMode) {
            case BOKEH:
                imageCaptureExtender = BokehImageCaptureExtender.create(builder);
                break;
            case HDR:
                imageCaptureExtender = HdrImageCaptureExtender.create(builder);
                break;
            case NIGHT:
                imageCaptureExtender = NightImageCaptureExtender.create(builder);
                break;
            case BEAUTY:
                imageCaptureExtender = BeautyImageCaptureExtender.create(builder);
                break;
            case AUTO:
                imageCaptureExtender = AutoImageCaptureExtender.create(builder);
                break;
        }

        if (imageCaptureExtender != null) {
            imageCaptureExtender.enableExtension();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyEffectConfig(@NonNull PreviewConfig.Builder builder,
            @NonNull LifecycleOwner lifecycleOwner) {
        PreviewExtender previewExtender = null;
        EffectMode effectMode = ExtensionsManager.getEffectMode(lifecycleOwner);

        switch (effectMode) {
            case BOKEH:
                previewExtender = BokehPreviewExtender.create(builder);
                break;
            case HDR:
                previewExtender = HdrPreviewExtender.create(builder);
                break;
            case NIGHT:
                previewExtender = NightPreviewExtender.create(builder);
                break;
            case BEAUTY:
                previewExtender = BeautyPreviewExtender.create(builder);
                break;
            case AUTO:
                previewExtender = AutoPreviewExtender.create(builder);
                break;
        }

        if (previewExtender != null) {
            previewExtender.enableExtension();
        }
    }
}
