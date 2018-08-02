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
import androidx.annotation.Nullable;
import androidx.car.cluster.navigation.util.Common;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;
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
    @ParcelField(3)
    List<Lane> mLanes;
    @ParcelField(4)
    List<StepCue> mStepCues;

    /**
     * Creates an empty step
     */
    public Step() {
    }

    /**
     * @see #getManeuver()
     */
    public Step setManeuver(@Nullable Maneuver maneuver) {
        mManeuver = maneuver;
        return this;
    }

    /**
     * @see #getDistance()
     */
    public Step setDistance(@Nullable Distance distance) {
        mDistance = distance;
        return this;
    }

    /**
     * @see #getLanes()
     */
    public Step setLanes(@NonNull List<Lane> lanes) {
        mLanes = Preconditions.checkNotNull(lanes);
        return this;
    }

    /**
     * @see #getStepCues()
     */
    public Step setStepCues(@NonNull List<StepCue> cues) {
        mStepCues = Preconditions.checkNotNull(cues);
        return this;
    }

    /**
     * Distance from the current position to the point where this navigation step should be
     * executed, or null if distance to this step is unknown.
     */
    @Nullable
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * The maneuver to be performed on this step, or null if this step doesn't involve a maneuver.
     */
    @Nullable
    public Maneuver getManeuver() {
        return mManeuver;
    }

    /**
     * Configuration of all road lanes at the point where the driver should execute this step.
     * Lane configurations are listed from left to right.
     */
    @NonNull
    public List<Lane> getLanes() {
        return Common.nonNullOrEmpty(mLanes);
    }

    /**
     * A list of cues to help the user navigate this step. As many cues as possible should be
     * displayed, starting from the first item in the list.
     */
    @NonNull
    public List<StepCue> getStepCues() {
        return Common.nonNullOrEmpty(mStepCues);
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
                && Objects.equals(mDistance, step.mDistance)
                && Objects.equals(mLanes, step.mLanes)
                && Objects.equals(mStepCues, step.mStepCues);
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s, lanes: %s, stepCues: %s}",
                mManeuver, mDistance, mLanes, mStepCues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mManeuver, mDistance, mLanes, mStepCues);
    }
}
