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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.cluster.navigation.util.Common;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;
import java.util.Objects;

/**
 * Navigation state data to be displayed on the instrument cluster of a car. This is composed by:
 * <ul>
 * <li>a list of destinations.
 * <li>the immediate step or steps in order to drive towards those destinations.
 * </ul>
 * This information can converted it to/from a {@link Parcelable} by using {@link #toParcelable()}
 * and {@link #fromParcelable(Parcelable)}, in order to be used in IPC (see {@link Parcel}).
 * <p>
 * All the information is retained by reference, so any updates made to an instance of this class,
 * or any of the objects referenced by it, will be captured by {@link #fromParcelable(Parcelable)}.
 * </ul>
 */
@VersionedParcelize
public final class NavigationState implements VersionedParcelable {
    @ParcelField(1)
    List<Step> mSteps;
    @ParcelField(2)
    List<Destination> mDestinations;

    /**
     * Creates a default {@link NavigationState}
     */
    public NavigationState() {
    }

    /**
     * Sets the navigation steps, in order of execution. It is up to the producer to decide how
     * many steps in advance will be provided.
     *
     * @return this object for chaining
     */
    @NonNull
    public NavigationState setSteps(@NonNull List<Step> steps) {
        mSteps = Preconditions.checkNotNull(steps);
        return this;
    }

    /**
     * Sets the destination and intermediate stops in the navigation, sorted from nearest to
     * furthest.
     *
     * @return this object for chaining
     */
    @NonNull
    public NavigationState setDestinations(@NonNull List<Destination> destinations) {
        mDestinations = Preconditions.checkNotNull(destinations);
        return this;
    }

    /**
     * Returns the navigation steps, in order of execution. It is up to the producer to decide how
     * many steps in advance will be provided.
     */
    @NonNull
    public List<Step> getSteps() {
        return Common.nonNullOrEmpty(mSteps);
    }

    /**
     * Returns the destination and intermediate stops in the navigation, sorted from nearest to
     * furthest.
     */
    @NonNull
    public List<Destination> getDestinations() {
        return Common.nonNullOrEmpty(mDestinations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NavigationState that = (NavigationState) o;
        return Objects.equals(mSteps, that.mSteps)
                && Objects.equals(mDestinations, that.mDestinations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSteps, mDestinations);
    }

    @Override
    public String toString() {
        return String.format("{steps: %s, destinations: %s}", mSteps,
                mDestinations);
    }

    /**
     * Returns this {@link NavigationState} as a {@link Parcelable}
     */
    @NonNull
    public Parcelable toParcelable() {
        return ParcelUtils.toParcelable(this);
    }

    /**
     * Creates a {@link NavigationState} based on data stored in the given {@link Parcelable}
     */
    public static NavigationState fromParcelable(@Nullable Parcelable parcelable) {
        return parcelable != null ? ParcelUtils.fromParcelable(parcelable) : new NavigationState();
    }
}
