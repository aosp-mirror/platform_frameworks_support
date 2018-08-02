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
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A representation of a time and timezone that can be serialized as a {@link VersionedParcelable}.
 */
@VersionedParcelize
public class Time implements VersionedParcelable {
    @ParcelField(1)
    long mSecondsSinceEpoch = 0;
    @ParcelField(2)
    String mZoneId = null;

    /**
     * Used by {@link VersionedParcelable}
     */
    Time() {
    }

    /**
     * Creates a {@link Time} that wraps the given {@link ZonedDateTime}
     */
    public Time(@NonNull ZonedDateTime time) {
        mSecondsSinceEpoch = time.toEpochSecond();
        mZoneId = time.getZone().getId();
    }

    /**
     * Creates a {@link Time} based on the provided {@link Instant} and a timezone
     * identified by the provided {@link ZoneId}.
     */
    public Time(@NonNull Instant time, @Nullable ZoneId zoneId) {
        mSecondsSinceEpoch = time.getEpochSecond();
        mZoneId = zoneId != null ? zoneId.getId() : null;
    }

    /**
     * Creates a {@link Time} based on the provided {@link Instant} but without timezone
     * information.
     */
    public Time(@NonNull Instant time) {
        this(time, null);
    }

    /**
     * An {@link Instant}
     */
    public @NonNull Instant getTime() {
        return Instant.ofEpochSecond(mSecondsSinceEpoch);
    }

    /**
     * A {@link ZoneId} representing the timezone the producer suggested to be used for display,
     * or null if the producer didn't suggest any particular timezone.
     */
    public @Nullable ZoneId getZoneId() {
        return mZoneId != null ? ZoneId.of(mZoneId) : null;
    }

    /**
     * A {@link ZonedDateTime} representing the wrapped time and timezone. If the producer of this
     * value didn't provide timezone information, then the default system timezone would be used.
     * <p>
     * See {@link ZoneId#systemDefault()}
     */
    public @NonNull ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.ofInstant(getTime(), mZoneId != null ? ZoneId.of(mZoneId)
                : ZoneId.systemDefault());
    }
}
