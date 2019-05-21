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

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * A set of {@link CameraIdFilter}.
 */
public final class CameraIdFilterSet implements CameraIdFilter {
    private Set<CameraIdFilter> mCameraIdFilterSet = new HashSet<>();

    @Override
    @NonNull
    public Set<String> filter(@NonNull Set<String> cameraIds) {
        for (CameraIdFilter filter : mCameraIdFilterSet) {
            cameraIds = filter.filter(cameraIds);
        }

        return cameraIds;
    }

    /** Adds a {@link androidx.camera.core.CameraIdFilter} to the set. */
    public void addCameraIdFilter(@NonNull CameraIdFilter cameraIdFilter) {
        if (cameraIdFilter instanceof CameraIdFilterSet) {
            mCameraIdFilterSet.addAll(((CameraIdFilterSet) cameraIdFilter).getCameraIdFilters());
        } else {
            mCameraIdFilterSet.add(cameraIdFilter);
        }
    }

    /** Removes a {@link androidx.camera.core.CameraIdFilter} from the set. */
    public void removeCameraIdFilter(@NonNull CameraIdFilter cameraIdFilter) {
        mCameraIdFilterSet.remove(cameraIdFilter);
    }

    /** Replace all same {@link CameraIdFilter} in the set with the new implementation. */
    public void replaceCameraIdFilter(@NonNull CameraIdFilter cameraIdFilter) {
        Set<CameraIdFilter> setToRemove = new HashSet<>();
        for (CameraIdFilter filter : mCameraIdFilterSet) {
            if (filter.getClass().equals(cameraIdFilter.getClass())) {
                setToRemove.add(filter);
            }
        }
        for (CameraIdFilter filter : setToRemove) {
            mCameraIdFilterSet.remove(filter);
        }
        mCameraIdFilterSet.add(cameraIdFilter);
    }

    /** Gets a set of {@link androidx.camera.core.CameraIdFilter}. */
    @NonNull
    public Set<CameraIdFilter> getCameraIdFilters() {
        return mCameraIdFilterSet;
    }
}
