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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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
    Distance mDistance = null;
    @ParcelField(2)
    Time mEta = null;
    @ParcelField(3)
    Maneuver mManeuver = null;
    @ParcelField(4)
    List<Lane> mLanes = new ArrayList<>();
    @ParcelField(5)
    List<StepCue> mStepCues = new ArrayList<>();

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public Step() {
    }

    /**
     * Creates a new step with the given {@link Maneuver}
     */
    public Step(@Nullable Maneuver maneuver) {
        mManeuver = maneuver;
    }

    /**
     * @see #getDistance()
     */
    public Step setDistance(@Nullable Distance distance) {
        mDistance = distance;
        return this;
    }

    /**
     * @see #getEta()
     */
    public Step setEta(@Nullable ZonedDateTime eta) {
        mEta = eta != null ? new Time(eta) : null;
        return this;
    }

    /**
     * @see #getLanes()
     */
    public Step addLane(@NonNull Lane lane) {
        mLanes.add(Preconditions.checkNotNull(lane));
        return this;
    }

    /**
     * @see #getStepCues()
     */
    public Step addStepCue(@NonNull StepCue cue) {
        mStepCues.add(Preconditions.checkNotNull(cue));
        return this;
    }

    /**
     * Distance from the current position to the point where this navigation step should be
     * executed, or null if distance to this step is unknown.
     */
    public @Nullable Distance getDistance() {
        return mDistance;
    }

    /**
     * Estimated time when this navigation step should be executed, or null if time is unknown.
     */
    public @Nullable ZonedDateTime getEta() {
        return mEta != null ? mEta.getZonedDateTime() : null;
    }

    /**
     * The maneuver to be performed on this step, or null if this step doesn't involve a maneuver.
     */
    public @Nullable Maneuver getManeuver() {
        return mManeuver;
    }

    /**
     * Configuration of all road lanes at the point where the driver should execute this step.
     * Lane configurations are listed from left to right.
     */
    public @NonNull List<Lane> getLanes() {
        return mLanes;
    }

    /**
     * A list of cues to help the user navigate this step. As many cues as possible should be
     * displayed, starting from the first item in the list.
     */
    public @NonNull List<StepCue> getStepCues() {
        return mStepCues;
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
                && Objects.equals(mEta, step.mEta)
                && Objects.equals(mLanes, step.mLanes)
                && Objects.equals(mStepCues, step.mStepCues);
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s, eta: %s, lanes: %s, stepCues: %s}",
                mManeuver, mDistance, mEta, mLanes, mStepCues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mManeuver, mDistance, mEta, mLanes, mStepCues);
    }
}
