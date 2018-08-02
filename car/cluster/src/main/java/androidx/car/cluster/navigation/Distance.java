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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.cluster.navigation.util.EnumWrapper;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Distance along the planned route between relevant points in the navigation
 */
@VersionedParcelize
public final class Distance implements VersionedParcelable {
    /**
     * Possible units used to display this distance.
     */
    public enum Unit {
        /**
         * Display distance is unknown, not provided by the producer, or the unit is unknown to the
         * consumer.
         */
        UNKNOWN,
        METERS,
        KILOMETERS,
        MILES,
        FEET,
        YARDS,
    }

    @ParcelField(1)
    int mMeters;
    @ParcelField(2)
    int mDisplayE3;
    @ParcelField(3)
    EnumWrapper<Unit> mDisplayUnit = EnumWrapper.empty();

    /**
     * Used by {@link VersionedParcelable}

     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Distance() {
    }

    /**
     * Creates a distance.
     *
     * @param meters distance in meters.
     * @param displayE3 distance measured in {@code displayUnit}, multiplied by 1000.
     * @param displayUnit unit to be used when displaying this distance
     */
    public Distance(int meters, int displayE3, @NonNull Unit displayUnit) {
        mMeters = meters;
        mDisplayE3 = displayE3;
        mDisplayUnit = EnumWrapper.of(displayUnit);
    }

    /**
     * Distance in meters.
     */
    public int getMeters() {
        return mMeters;
    }

    /**
     * Distance measure in the unit indicated at {@link #getDisplayUnit()}, multiplied by 1000
     * (e.g.: a distance of 1.2 km would be represented as displayE3 = 1200 and mDisplayUnit =
     * KILOMETERS). This field is only relevant if {@link #getDisplayUnit()} is not null.
     * <p>
     * This distance is for display only and it should mirror the distance displayed by the
     * producer in its own UI.
     */
    public int getDisplayE3() {
        return mDisplayE3;
    }

    /**
     * Distance unit (adjusted to the current user's locale and/or location). This field would
     * mirror the distance unit displayed by the producer in its own UI.
     */
    @NonNull
    public Unit getDisplayUnit() {
        return mDisplayUnit.getValue(Unit.UNKNOWN);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Distance distance = (Distance) o;
        return mMeters == distance.mMeters
                && mDisplayE3 == distance.mDisplayE3
                && Objects.equals(mDisplayUnit, distance.mDisplayUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMeters, mDisplayUnit, mDisplayE3);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{meters: %d, displayUnit: %s, displayE3: %d", mMeters, mDisplayUnit,
                mDisplayE3);
    }
}
