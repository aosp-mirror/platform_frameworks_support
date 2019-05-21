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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.HashSet;
import java.util.Set;

/**
 * A set of {@link CameraIdFilter}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraIdFilterSet implements CameraIdFilter {
    private Set<CameraIdFilter> mCameraIdFilterSet = new HashSet<>();

    @Override
    public Set<String> filter(Set<String> cameraIds) {
        for (CameraIdFilter filter : mCameraIdFilterSet) {
            cameraIds = filter.filter(cameraIds);
        }

        return cameraIds;
    }

    /** Adds a {@link androidx.camera.core.CameraIdFilter} to the set. */
    public void addCameraIdFilter(CameraIdFilter cameraIdFilter) {
        mCameraIdFilterSet.add(cameraIdFilter);
    }

    /** Removes a {@link androidx.camera.core.CameraIdFilter} from the set. */
    public void removeCameraIdFilter(CameraIdFilter cameraIdFilter) {
        mCameraIdFilterSet.remove(cameraIdFilter);
    }

    /** Gets a set of {@link androidx.camera.core.CameraIdFilter}. */
    public Set<CameraIdFilter> getCameraIdFilters() {
        return mCameraIdFilterSet;
    }
}
