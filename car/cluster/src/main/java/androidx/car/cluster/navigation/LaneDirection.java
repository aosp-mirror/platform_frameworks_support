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
public class LaneDirection implements VersionedParcelable {
    /**
     * Shape describes the type and amount of turn made by a lane. For example, it is be used by the
     * client to choose what sort of icon to display to the user to represent the lane.
     */
    public enum Shape {
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

    @ParcelField(1)
    EnumWrapper<Shape> mShape = EnumWrapper.of();
    @ParcelField(2)
    EnumWrapper<Side> mSide = EnumWrapper.of();

    /**
     * Lane type, or null if the type is not known to the consumer.
     */
    public @Nullable Shape getShape() {
        return mShape.getValue(Shape.class);
    }

    /**
     * Turn side, or null if the turn side is not known to the consumer.
     */
    public @Nullable Side getSide() {
        return mSide.getValue(Side.class);
    }

    /**
     * Used by {@link VersionedParcelable}
     */
    LaneDirection() {
    }

    /**
     * Creates a new lane direction
     */
    public LaneDirection(@NonNull Shape shape, @NonNull Side side) {
        mShape = EnumWrapper.ofNonNull(shape);
        mSide = EnumWrapper.ofNonNull(side);
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
                && Objects.equals(mSide, laneDirection.mSide);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShape, mSide);
    }
}
