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
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * One of the possible directions a driver can go when using a particular lane at a particular
 * step in the navigation. This can be used by the consumer to choose what icon or icons to combine
 * in order to display a lane configuration to the user.
 */
@VersionedParcelize
public final class LaneDirection implements VersionedParcelable {
    /**
     * Turn amount and direction.
     */
    public enum Shape {
        /**
         * The shape is unknown to the consumer, in which case the consumer shouldn't show any
         * lane information at all.
         */
        UNKNOWN,
        /**
         * No turn.
         */
        STRAIGHT,
        /**
         * Slight turn (10-45 degrees).
         */
        SLIGHT_LEFT,
        /**
         * @see #SLIGHT_LEFT
         */
        SLIGHT_RIGHT,
        /**
         * Regular turn (45-135 degrees).
         */
        NORMAL_LEFT,
        /**
         * @see #NORMAL_LEFT
         */
        NORMAL_RIGHT,
        /**
         * Sharp turn (135-175 degrees).
         */
        SHARP_LEFT,
        /**
         * @see #SHARP_LEFT
         */
        SHARP_RIGHT,
        /**
         * A turn onto the opposite side of the same street (175-180 degrees).
         */
        U_TURN_LEFT,
        /**
         * @see #U_TURN_LEFT
         */
        U_TURN_RIGHT,
    }


    @ParcelField(1)
    EnumWrapper<Shape> mShape;
    @ParcelField(2)
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
    public LaneDirection(@NonNull Shape shape, boolean highlighted) {
        mShape = EnumWrapper.of(shape);
        mHighlighted = highlighted;
    }

    /**
     * Returns shape of this lane direction.
     */
    @NonNull
    public Shape getShape() {
        return EnumWrapper.getValue(mShape, Shape.UNKNOWN);
    }

    /**
     * Returns whether this is the direction the driver should take in order to stay in the
     * navigation route.
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
        return Objects.equals(getShape(), laneDirection.getShape())
                && isHighlighted() == laneDirection.isHighlighted();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShape(), isHighlighted());
    }

    @Override
    public String toString() {
        return String.format("{shape: %s, highlighted: %s}", mShape, mHighlighted);
    }
}
