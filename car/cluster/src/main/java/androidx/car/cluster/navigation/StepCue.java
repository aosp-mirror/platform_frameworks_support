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
import androidx.car.cluster.navigation.util.Common;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Accessory information to help the driver successfully complete a maneuver (e.g.: "On exit 45").
 */
@VersionedParcelize
public final class StepCue implements VersionedParcelable {
    @ParcelField(1)
    String mMessage;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    StepCue() {
    }

    /**
     * Creates a new {@link StepCue}
     *
     * @param message A message describing this cue, already localized to the current user's
     *                language and locale, or an empty string if no message is associated with this
     *                cue.
     */
    public StepCue(@NonNull String message) {
        mMessage = Preconditions.checkNotNull(message);
    }

    /**
     * A message describing this cue, already localized to the current user's language and locale,
     * or an empty string if no message is associated with this cue.
     */
    @NonNull
    public String getMessage() {
        return Common.nonNullOrEmpty(mMessage);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StepCue stepCue = (StepCue) o;
        return Objects.equals(mMessage, stepCue.mMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMessage);
    }

    @Override
    public String toString() {
        return String.format("{message: %s}", mMessage);
    }
}
