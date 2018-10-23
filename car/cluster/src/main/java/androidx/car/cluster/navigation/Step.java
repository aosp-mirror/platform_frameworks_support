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
import java.util.Collections;
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
    List<Image> mImageCues;
    @ParcelField(5)
    List<String> mTextCues;

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
            @NonNull List<Image> imageCues, @NonNull List<String> textCues) {
        mDistance = distance;
        mManeuver = maneuver;
        mLanes = Collections.unmodifiableList(new ArrayList<>(lanes));
        mImageCues = Collections.unmodifiableList(new ArrayList<>(imageCues));
        mTextCues = Collections.unmodifiableList(new ArrayList<>(textCues));
    }

    /**
     * Builder for creating a {@link Step}
     */
    public static final class Builder {
        Distance mDistance;
        Maneuver mManeuver;
        List<Lane> mLanes = new ArrayList<>();
        List<Image> mImageCues = new ArrayList<>();
        List<String> mTextCues = new ArrayList<>();

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
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addLane(@NonNull Lane lane) {
            mLanes.add(Preconditions.checkNotNull(lane));
            return this;
        }

        /**
         * Adds an image cue to this step. An image cue is a graphical representation of a sign
         * or marker the driver should look for in order to perform this step. Examples of image
         * cues are: highway badges, road exits, road signs.
         * <p>
         * Image cues should be added from most relevant to least relevant. These images should be
         * optimized to be displayed one next to the other on a single row. This means that they can
         * have variable aspect ratio, but consumers will most likely present them with equal
         * height.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addImageCue(@NonNull Image imageCue) {
            mImageCues.add(Preconditions.checkNotNull(imageCue));
            return this;
        }

        /**
         * Adds a text cue to this step. A text cue is a textual representation of a sign or marker
         * the driver should look for in order to perform this step. Examples of text cues are:
         * "North ramp", "Take exit 2B", "Towards Main St.".
         * <p>
         * Text cues should be added from most relevant to least relevant.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addTextCue(@NonNull String textCue) {
            mTextCues.add(Preconditions.checkNotNull(textCue));
            return this;
        }

        /**
         * Returns a {@link Step} built with the provided information.
         */
        @NonNull
        public Step build() {
            return new Step(mDistance, mManeuver, mLanes, mImageCues, mTextCues);
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
     * the driver should execute this step. Lane configurations are listed from left to right.
     */
    @NonNull
    public List<Lane> getLanes() {
        return Common.nonNullOrEmpty(mLanes);
    }

    /**
     * Returns a list of images containing graphical representations of signs or markers the driver
     * should look for in order to perform this step. Examples of image cues are: highway badges,
     * road exits, road signs.
     * <p>
     * Image cues are listed from most relevant to least relevant.
     */
    @NonNull
    public List<Image> getImageCues() {
        return Common.nonNullOrEmpty(mImageCues);
    }

    /**
     * Returns a list of textual representations of signs or markers the driver should look for in
     * order to perform this step. Examples of image cues are: "North ramp", "Take exit 2B",
     * "Towards Main St.".
     * <p>
     * Text cues are listed from most relevant to least relevant.
     */
    @NonNull
    public List<String> getTextCues() {
        return Common.nonNullOrEmpty(mTextCues);
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
                && Objects.equals(getImageCues(), step.getImageCues())
                && Objects.equals(getTextCues(), step.getTextCues());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getManeuver(), getDistance(), getLanes(), getImageCues(),
                getTextCues());
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s, lanes: %s, imageCues: %s, textCues: %s}",
                mManeuver, mDistance, mLanes, mImageCues, mTextCues);
    }
}
