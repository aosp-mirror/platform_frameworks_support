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

import android.util.Log;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter camera id by lens facing.
 */
public final class LensFacingCameraIdFilter implements CameraIdFilter {
    private static final String TAG = "CameraIdFilter";
    private CameraX.LensFacing mLensFacing;

    public LensFacingCameraIdFilter(CameraX.LensFacing lensFacing) {
        mLensFacing = lensFacing;
    }

    @Override
    public Set<String> filter(Set<String> cameraIdSet) {
        Set<String> resultCameraIdSet = new LinkedHashSet<>();
        Set<String> cameraIdSetWithLensFacing = null;

        try {
            cameraIdSetWithLensFacing = CameraX.getCameraSetWithLensFacing(mLensFacing);
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to retrieve camera id list by lens facing.", e);
        }

        if (cameraIdSetWithLensFacing == null) {
            return resultCameraIdSet;
        }

        for (String cameraId : cameraIdSet) {
            if (cameraIdSetWithLensFacing.contains(cameraId)) {
                resultCameraIdSet.add(cameraId);
            }
        }

        return resultCameraIdSet;
    }

    public CameraX.LensFacing getLensFacing() {
        return mLensFacing;
    }
}
