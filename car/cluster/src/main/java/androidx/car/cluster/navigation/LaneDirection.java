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
import androidx.annotation.RestrictTo;
import androidx.car.cluster.navigation.util.EnumWrapper;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * One of the possible directions a driver can go when using a particular lane at a particular
 * step in the navigation.
 */
@VersionedParcelize
public final class LaneDirection implements VersionedParcelable {
    /**
     * Shape describes the type and amount of turn made by a lane. For example, it is be used by the
     * client to choose what sort of icon to display to the user to represent the lane.
     */
    public enum Shape {
        /**
         * The type is unknown to the consumer, in which case the consumer shouldn't show any
         * lane information at all.
         */
        UNKNOWN,
        /**
         * When there is no turn involved. For example, this occurs when lane guidance is specifying
         * to continue on a highway instead of taking one or more exit lanes.
         */
        STRAIGHT,
        /** Slight turn (10-45 degrees) */
        SLIGHT_TURN,
        /** Regular turn (45-135 degrees). */
        NORMAL_TURN,
        /** Sharp turn (135-175 degrees). */
        SHARP_TURN,
        /** A turn onto the opposite side of the same street (175-180 degrees). */
        U_TURN,
    }

    /**
     * Possible turning direction.
     */
    public enum Side {
        /**
         * The side is unknown to the consumer, in which case the consumer shouldn't show any
         * lane information at all.
         */
        UNKNOWN,
        /**
         * Driving or turning side is not relevant (e.g.: for {@link Maneuver.Type#STRAIGHT},
         * turning direction is not relevant).
         */
        NO_SIDE,
        /**
         * Left-hand driving (e.g.: Australian driving side), or left turn.
         */
        LEFT,
        /**
         * Right-hand driving (e.g.: USA driving side), or right turn.
         */
        RIGHT,
    }


    @ParcelField(1)
    EnumWrapper<Shape> mShape;
    @ParcelField(2)
    EnumWrapper<Side> mSide;
    @ParcelField(3)
    boolean mHighlighted;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    LaneDirection() {
    }

    /**
     * Creates a new lane direction
     */
    public LaneDirection(@NonNull Shape shape, @NonNull Side side, boolean highlighted) {
        mShape = EnumWrapper.of(shape);
        mSide = EnumWrapper.of(side);
        mHighlighted = highlighted;
    }

    /**
     * Returns the lane type.
     */
    @NonNull
    public Shape getShape() {
        return EnumWrapper.getValue(mShape, Shape.UNKNOWN);
    }

    /**
     * Returns the turn side.
     */
    @NonNull
    public Side getSide() {
        return EnumWrapper.getValue(mSide, Side.UNKNOWN);
    }

    /**
     * Returns whether this is a directions the driver could take on this lane in order to stay in
     * the navigation route.
     */
    public boolean isHighlighted() {
        return mHighlighted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LaneDirection laneDirection = (LaneDirection) o;
        return Objects.equals(mShape, laneDirection.mShape)
                && Objects.equals(mSide, laneDirection.mSide)
                && mHighlighted == laneDirection.mHighlighted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mSide, mHighlighted);
    }

    @Override
    public String toString() {
        return String.format("{shape: %s, side: %s, highlighted}", mShape, mSide, mHighlighted);
    }
}
