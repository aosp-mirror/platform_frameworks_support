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

package androidx.car.cluster.navigation.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.Duration;
import java.util.Objects;

/**
 * A {@link VersionedParcelable} wrapper for {@link Duration}
 */
@VersionedParcelize
public class DurationWrapper implements VersionedParcelable {
    @ParcelField(1)
    long mMilliseconds;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    DurationWrapper() {
    }

    private DurationWrapper(@NonNull Duration duration) {
        mMilliseconds = Preconditions.checkNotNull(duration).toMillis();
    }

    /**
     * Returns the wrapped {@link Duration}
     */
    public Duration getDuration() {
        return Duration.ofMillis(mMilliseconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DurationWrapper that = (DurationWrapper) o;
        return mMilliseconds == mMilliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMilliseconds);
    }

    /**
     * Wraps the given {@link Duration} into a {@link VersionedParcelable}
     */
    public static DurationWrapper of(Duration duration) {
        return new DurationWrapper(duration);
    }
}
