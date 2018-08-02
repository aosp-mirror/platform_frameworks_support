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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.cluster.navigation.util.Common;
import androidx.car.cluster.navigation.util.Time;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Final or intermediate stop in a navigation route.
 */
@VersionedParcelize
public final class Destination implements VersionedParcelable {
    @ParcelField(1)
    String mTitle;
    @ParcelField(2)
    String mAddress;
    @ParcelField(3)
    Distance mDistance;
    @ParcelField(4)
    Time mEta;
    @ParcelField(5)
    Location mLocation;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Destination() {
    }

    /**
     * Creates a destination
     *
     * @param title Name of the destination (formatted for the current user's locale)
     */
    public Destination(@NonNull String title) {
        mTitle = Preconditions.checkNotNull(title);
    }

    /**
     * Sets the destination address (formatted for the current user's locale), or empty if there
     * is no address associated with this destination.
     *
     * @return this object for chaining
     */
    @NonNull
    public Destination setAddress(@NonNull String address) {
        mAddress = Preconditions.checkNotNull(address);
        return this;
    }

    /**
     * Sets the distance from the current position to this destination, or null if distance is
     * unknown.
     *
     * @return this object for chaining
     */
    @NonNull
    public Destination setDistance(@Nullable Distance distance) {
        mDistance = distance;
        return this;
    }

    /**
     * Sets the estimated time of arrival to this destination, or null if estimated time of
     * arrival is unknown.
     *
     * @return this object for chaining
     */
    @NonNull
    public Destination setEta(@Nullable ZonedDateTime eta) {
        mEta = eta != null ? new Time(eta) : null;
        return this;
    }

    /**
     * Sets the geo-location of this destination, or null if location is unknown.
     *
     * @return this object for chaining
     */
    @NonNull
    public Destination setLocation(@Nullable Location location) {
        mLocation = location;
        return this;
    }

    /**
     * Returns the name of the destination (formatted for the current user's locale), or empty if
     * destination name is unknown.
     */
    @NonNull
    public String getTitle() {
        return Common.nonNullOrEmpty(mTitle);
    }

    /**
     * Returns the destination address (formatted for the current user's locale), or empty if there
     * is no address associated with this destination.
     */
    @NonNull
    public String getAddress() {
        return Common.nonNullOrEmpty(mAddress);
    }

    /**
     * Returns the distance from the current position to this destination, or null if distance was
     * not provided or is unknown.
     */
    @Nullable
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Returns the estimated time of arrival to this destination, or null if it was not provided or
     * is unknown.
     */
    @Nullable
    public ZonedDateTime getEta() {
        return mEta != null ? mEta.getTime() : null;
    }

    /**
     * Returns the geo-location of this destination, or null if it was not provided or is unknown.
     */
    @Nullable
    public Location getLocation() {
        return mLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Destination that = (Destination) o;
        return Objects.equals(mTitle, that.mTitle)
                && Objects.equals(mAddress, that.mAddress)
                && Objects.equals(mDistance, that.mDistance)
                && Objects.equals(mLocation, that.mLocation)
                && Objects.equals(mEta, that.mEta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mAddress, mDistance, mLocation, mEta);
    }

    @Override
    public String toString() {
        return String.format("{title: %s, address: %s, distance: %s, location: %s, eta: %s}",
                mTitle, mAddress, mDistance, mLocation, mEta);
    }
}
