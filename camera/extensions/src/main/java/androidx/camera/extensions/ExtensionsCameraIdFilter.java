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

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import androidx.camera.core.CameraIdFilter;
import androidx.camera.core.CameraX;
import androidx.camera.extensions.impl.PreviewExtenderImpl;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter camera id by extension availability.
 */
public final class ExtensionsCameraIdFilter implements CameraIdFilter {
    PreviewExtenderImpl mPreviewExtenderImpl;

    ExtensionsCameraIdFilter(PreviewExtenderImpl previewExtenderImpl) {
        PreviewExtenderImpl = previewExtenderImpl;
    }

    @Override
    public Set<String> filter(Set<String> cameraIdSet) {
        Set<String> resultCameraIdSet = new LinkedHashSet<>();
        for (String cameraId : cameraIdSet) {
            if (mPreviewExtenderImpl.isExtensionAvailable(cameraId,
                    getCameraCharacteristics(cameraId))) {
                resultCameraIdSet.add(cameraId);
            }
        }

        return resultCameraIdSet;
    }

    static CameraCharacteristics getCameraCharacteristics(String cameraId) {
        Context context = CameraX.getContext();
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }

        return cameraCharacteristics;
    }
}
