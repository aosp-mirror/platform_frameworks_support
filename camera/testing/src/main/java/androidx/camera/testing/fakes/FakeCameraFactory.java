/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.LensFacingCameraIdFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link CameraFactory} implementation that contains and produces fake cameras.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraFactory implements CameraFactory {

    private static final String BACK_ID = "0";
    private static final String FRONT_ID = "1";

    private Set<String> mCameraIds;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<String, BaseCamera> mCameraMap = new HashMap<>();

    public FakeCameraFactory() {
        HashSet<String> camIds = new HashSet<>();
        camIds.add(BACK_ID);
        camIds.add(FRONT_ID);

        mCameraIds = Collections.unmodifiableSet(camIds);
    }

    @Override
    public BaseCamera getCamera(String cameraId) {
        if (mCameraIds.contains(cameraId)) {
            BaseCamera camera = mCameraMap.get(cameraId);
            if (camera == null) {
                camera = new FakeCamera();
                mCameraMap.put(cameraId, camera);
            }
            return camera;
        }
        throw new IllegalArgumentException("Unknown camera: " + cameraId);
    }

    /**
     * Inserts a camera with the given camera ID.
     *
     * @param cameraId Identifier to use for the camera.
     * @param camera Camera implementation.
     */
    public void insertCamera(String cameraId, BaseCamera camera) {
        if (!mCameraIds.contains(cameraId)) {
            HashSet<String> newCameraIds = new HashSet<>(mCameraIds);
            newCameraIds.add(cameraId);
            mCameraIds = Collections.unmodifiableSet(newCameraIds);
        }

        mCameraMap.put(cameraId, camera);
    }

    @Override
    public Set<String> getAvailableCameraIds() {
        return mCameraIds;
    }

    @Override
    public boolean hasCameraIdForLensFacing(@NonNull LensFacing lensFacing) {
        for (String cameraId : mCameraMap.keySet()) {
            try {
                if (mCameraMap.get(cameraId).getCameraInfo().getLensFacing() == lensFacing) {
                    return true;
                }
            } catch (CameraInfoUnavailableException e) {
                throw new IllegalArgumentException(
                        "Unable to get camera info.", e);
            }
        }

        return false;
    }

    @Override
    @Nullable
    public String cameraIdForLensFacing(@NonNull LensFacing lensFacing) {
        switch (lensFacing) {
            case FRONT:
                return FRONT_ID;
            case BACK:
                return BACK_ID;
        }

        throw new IllegalArgumentException("Unknown lensFacing: " + lensFacing);
    }

    @Override
    @NonNull
    public LensFacingCameraIdFilter getLensFacingCameraIdFilter(@NonNull LensFacing lensFacing) {
        return new FakeLensFacingCameraIdFilter(lensFacing);
    }

    /** Fake implementation of {@link androidx.camera.core.LensFacingCameraIdFilter}. */
    public final class FakeLensFacingCameraIdFilter implements LensFacingCameraIdFilter {
        LensFacing mLensFacing;

        FakeLensFacingCameraIdFilter(LensFacing lensFacing) {
            mLensFacing = lensFacing;
        }

        @Override
        @NonNull
        public Set<String> filter(@NonNull Set<String> cameraIds) {
            Set<String> resultCameraIdSet = new LinkedHashSet<>();

            for (String cameraId : cameraIds) {
                if (mCameraMap.containsKey(cameraId)) {
                    try {
                        if (mCameraMap.get(cameraId).getCameraInfo().getLensFacing()
                                == mLensFacing) {
                            resultCameraIdSet.add(cameraId);
                        }
                    } catch (CameraInfoUnavailableException e) {
                        throw new IllegalArgumentException(
                                "Unable to get camera info.", e);
                    }
                }
            }

            return resultCameraIdSet;
        }

        @Override
        @NonNull
        public LensFacing getLensFacing() {
            return mLensFacing;
        }
    }
}
