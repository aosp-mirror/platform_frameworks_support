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
    Distance mDistance;
    @ParcelField(2)
    Maneuver mManeuver;
    @ParcelField(3)
    List<Lane> mLanes;
    @ParcelField(4)
    Image mLanesImage;
    @ParcelField(5)
    RichText mCue;

    /**
     * Used by {@link VersionedParcelable}

     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Step() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Step(@Nullable Distance distance, @Nullable Maneuver maneuver, @NonNull List<Lane> lanes,
            @Nullable Image lanesImage, @Nullable RichText cue) {
        Preconditions.checkArgument((lanes != null && !lanes.isEmpty() && lanesImage != null)
                || ((lanes == null || lanes.isEmpty()) && lanesImage == null), "Lanes "
                + "configuration metadata and image must be both provided, or not provided at "
                + "all.");
        mDistance = distance;
        mManeuver = maneuver;
        mLanes = new ArrayList<>(lanes);
        mLanesImage = lanesImage;
        mCue = cue;
    }

    /**
     * Builder for creating a {@link Step}
     */
    public static final class Builder {
        Distance mDistance;
        Maneuver mManeuver;
        List<Lane> mLanes = new ArrayList<>();
        Image mLanesImage;
        RichText mCue;

        /**
         * Sets the distance from the current position to the point where this navigation step
         * should be executed, or null if this step doesn't involve a maneuver.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setManeuver(@Nullable Maneuver maneuver) {
            mManeuver = maneuver;
            return this;
        }

        /**
         * Sets the maneuver to be performed on this step, or null if distance to this step is not
         * provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setDistance(@Nullable Distance distance) {
            mDistance = distance;
            return this;
        }

        /**
         * Adds a road lane configuration to this step. Lanes should be added from left to right.
         * <p>
         * If {@link Lane} metadata is provided, then a lane configuration image must also be
         * provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addLane(@NonNull Lane lane) {
            mLanes.add(Preconditions.checkNotNull(lane));
            return this;
        }

        /**
         * Sets an image that represents a complete lanes configuration. This means that the image,
         * if provided, must represent all lanes at this navigation step, one next to the other in
         * a single row, highlighting the lane or lanes where the driver should be at this point,
         * and the direction or directions the driver should follow to stay in the navigation route.
         * <p>
         * If lane configuration image is provided, then {@link Lane} metadata must also be
         * provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setLanesImage(@Nullable Image lanesImage) {
            mLanesImage = lanesImage;
            return this;
        }

        /**
         * Sets auxiliary instructions on how complete this navigation step, described as a
         * {@link RichText} object containing text (e.g.: "towards", "Wallaby way") and images
         * (e.g.: road badge of a highway). Most important elements of this description should be
         * located at the beginning of the sequence, allowing consumers to truncate this sequence if
         * rendering space is not enough.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setCue(@Nullable RichText cue) {
            mCue = cue;
            return this;
        }

        /**
         * Returns a {@link Step} built with the provided information.
         */
        @NonNull
        public Step build() {
            return new Step(mDistance, mManeuver, mLanes, mLanesImage, mCue);
        }
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

    /**
     * Returns an unmodifiable list containing the configuration of road lanes at the point where
     * the driver should execute this step, or an empty list if lane configuration metadata is not
     * available. Lane configurations are listed from left to right.
     */
    @NonNull
    public List<Lane> getLanes() {
        return Common.immutableOrEmpty(mLanes);
    }

    /**
     * Returns an image representing the lanes configuration at this point in the navigation, or
     * null if the lanes configuration image was not provided. This image will represent all lanes
     * one next to the other in a single row, highlighting the lane or lanes where the driver should
     * be at this point, and the direction or directions the driver should follow to stay in the
     * navigation route.
     */
    @Nullable
    public Image getLanesImage() {
        return mLanesImage;
    }

    /**
     * Returns auxiliary instructions on how complete this navigation step, or null if additional
     * instructions are not available. These instructions are described as a {@link RichText} object
     * containing text (e.g.: "towards") and images (e.g.: road badge of a highway). Most important
     * elements of this description will be located at the beginning of the sequence, allowing
     * consumers to truncate this sequence if rendering space is not enough.
     */
    @Nullable
    public RichText getCue() {
        return mCue;
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
        return Objects.equals(getManeuver(), step.getManeuver())
                && Objects.equals(getDistance(), step.getDistance())
                && Objects.equals(getLanes(), step.getLanes())
                && Objects.equals(getLanesImage(), step.getLanesImage())
                && Objects.equals(getCue(), step.getCue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getManeuver(), getDistance(), getLanes(), getLanesImage(), getCue());
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s, lanes: %s, lanesImage: %s, cue: %s}",
                mManeuver, mDistance, mLanes, mLanesImage, mCue);
    }
}
