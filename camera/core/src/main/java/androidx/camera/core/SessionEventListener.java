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

import android.util.Size;

import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * CameraX session event interface.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SessionEventListener {

    /**
     * Notify to initial of the extension.
     * @param cameraId that current used.
     */
    void onInit(String cameraId);

    /**
     * Notify to de-initial of the extension.
     * @param executor to execute the deInit.
     */
    void onDeInit(Executor executor);

    /**
     * This callback will be invoked when the configured surface resolution is updated.
     *
     * @param size for the surface.
     */
    void onResolutionUpdate(Size size);

    /**
     * This callback will be invoked when the image format is updated.
     *
     * @param imageFormat for the surface.
     */
    void onImageFormatUpdate(int imageFormat);

}
