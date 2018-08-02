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
import androidx.car.cluster.navigation.util.Common;
import androidx.car.cluster.navigation.util.EnumWrapper;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Indication on the quality of service provided by the navigation state producer (e.g.: "rerouting"
 * or "service unavailable").
 */
@VersionedParcelize
public final class ProducerStatus implements VersionedParcelable {
    /**
     * Possible producer status codes
     */
    public enum Code {
        /**
         * Producer is in an abnormal state, unknown to the consumer. If available, the status
         * description should be shown to the user. Otherwise, a generic representation of this
         * status should be used instead.
         */
        NOT_OK,
        /**
         * Producer is fully functional and working as expected
         */
        OK,
        /**
         * Producer is unable to obtain geo-positioning.
         */
        GPS_SIGNAL_LOST,
        /**
         * Network access is lost and the navigation service is unable to function without it.
         */
        NETWORK_ACCESS_LOST,
        /**
         * Network is available, but the navigation service is unable to connect to its server.
         */
        UNABLE_TO_CONNECT,
        /**
         * Route is currently being re-calculated.
         */
        REROUTING,
        /**
         * There is no route to the selected destination.
         */
        NO_ROUTE,
        /**
         * Navigation state is unavailable.
         */
        SERVICE_UNAVAILABLE,
    }

    @ParcelField(1)
    EnumWrapper<Code> mCode;
    @ParcelField(2)
    String mDescription;

    /**
     * Creates a default {@link ProducerStatus}
     */
    public ProducerStatus() {
    }

    /**
     * Creates a new {@link ProducerStatus}
     *
     * @param code Code representing the status of the service.
     * @param description Optional status description, localized to the current user's language, or
     *                    empty string if description is not provided. If provided, this description
     *                    will be displayed alongside any other visual representation of this
     *                    status.
     */
    public ProducerStatus(@NonNull Code code, @NonNull String description) {
        mCode = EnumWrapper.of(code);
        mDescription = Preconditions.checkNotNull(description);
    }

    /**
     * Returns a code representing the status.
     */
    @NonNull
    public Code getCode() {
        return EnumWrapper.getValue(mCode, Code.NOT_OK);
    }

    /**
     * Returns an optional status description, localized to the current user's language, or empty
     * string if description is not provided. If provided, this description can be displayed
     * alongside any other visual representation of this status.
     */
    @NonNull
    public String getDescription() {
        return Common.nonNullOrEmpty(mDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProducerStatus producerStatus = (ProducerStatus) o;
        return Objects.equals(mCode, producerStatus.mCode)
                && Objects.equals(mDescription, producerStatus.mDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCode, mDescription);
    }

    @Override
    public String toString() {
        return String.format("{code: %s, description: '%s'}", mCode, mDescription);
    }
}
