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

import androidx.annotation.Nullable;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * An action that the driver should take in order to remain on the current navigation route. For
 * example: turning onto a street, taking a highway exit and merging onto a different highway,
 * continuing straight through a roundabout, etc.
 */
@VersionedParcelize
public final class Step implements VersionedParcelable {
    @ParcelField(1)
    Distance mDistance;
    @ParcelField(2)
    Maneuver mManeuver;

    /**
     * Creates an empty step
     */
    public Step() {
    }

    /**
     * Sets the distance from the current position to the point where this navigation step should be
     * executed, or null if this step doesn't involve a maneuver.
     */
    public Step setManeuver(@Nullable Maneuver maneuver) {
        mManeuver = maneuver;
        return this;
    }

    /**
     * Sets the maneuver to be performed on this step, or null if distance to this step is not
     * provided.
     */
    public Step setDistance(@Nullable Distance distance) {
        mDistance = distance;
        return this;
    }

    /**
     * Returns the distance from the current position to the point where this navigation step should
     * be executed, or null if distance to this step was not provided.
     */
    @Nullable
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Returns the maneuver to be performed on this step, or null if this step doesn't involve a
     * maneuver.
     */
    @Nullable
    public Maneuver getManeuver() {
        return mManeuver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Step step = (Step) o;
        return Objects.equals(mManeuver, step.mManeuver)
                && Objects.equals(mDistance, step.mDistance);
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s}", mManeuver, mDistance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mManeuver, mDistance);
    }
}
