/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import androidx.annotation.NonNull;
import androidx.car.cluster.navigation.util.Common;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;
import java.util.Objects;

/**
 * Configuration of a single lane of a road at a particular point in the navigation. It describes
 * all possible directions the driver could go from this lane, and indicates which direction the
 * driver should take to stay in the navigation route.
 */
@VersionedParcelize
public final class Lane implements VersionedParcelable {
    @ParcelField(1)
    List<LaneDirection> mDirections;

    /**
     * Creates an empty lane.
     */
    public Lane() {
    }

    /**
     * Sets all possible directions a driver can take from this lane, including the directions the
     * driver could take on this lane in order to stay in the navigation route, if any.
     */
    public Lane setDirections(@NonNull List<LaneDirection> directions) {
        mDirections = Preconditions.checkNotNull(directions);
        return this;
    }

    /**
     * Returns all possible directions a driver can take from this lane, including the directions
     * the driver could take on this lane in order to stay in the navigation route, if any.
     */
    @NonNull
    public List<LaneDirection> getDirections() {
        return Common.nonNullOrEmpty(mDirections);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Lane lane = (Lane) o;
        return Objects.equals(mDirections, lane.mDirections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDirections);
    }

    @Override
    public String toString() {
        return String.format("{direction: %s}", mDirections);
    }
}
