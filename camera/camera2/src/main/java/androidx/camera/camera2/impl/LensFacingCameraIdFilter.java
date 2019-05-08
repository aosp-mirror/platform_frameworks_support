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

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraIdFilter;
import androidx.camera.core.CameraX;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter camera id by lens facing.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class LensFacingCameraIdFilter implements CameraIdFilter {
    private CameraX.LensFacing mLensFacing;

    LensFacingCameraIdFilter(CameraX.LensFacing lensFacing) {
        mLensFacing = lensFacing;
    }

    @Override
    public Set<String> filter(Set<String> cameraIdSet) {
        Set<String> resultCameraIdSet = new LinkedHashSet<>();
        Integer cameraLensFacing =
                mLensFacing == CameraX.LensFacing.BACK ? CameraCharacteristics.LENS_FACING_BACK
                        : CameraCharacteristics.LENS_FACING_FRONT;
        for (String cameraId : cameraIdSet) {
            Integer lensFacing = getCameraCharacteristics(cameraId).get(
                    CameraCharacteristics.LENS_FACING);
            if (lensFacing == cameraLensFacing) {
                resultCameraIdSet.add(cameraId);
                break;
            }
        }

        return resultCameraIdSet;
    }

    public CameraX.LensFacing getLensFacing() {
        return mLensFacing;
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
