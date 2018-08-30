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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Structure for {@link MediaItemDesc2} whose playback would use {@link DataSourceCallback2}.
 * <p>
 * Users should use {@link Builder} to create {@link CallbackMediaItem2}.
 *
 * @see MediaItemDesc2
 * @hide
 */
// TODO(jaewan): Unhide this
@RestrictTo(LIBRARY)
public class CallbackMediaItem2 extends MediaItemDesc2 {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    DataSourceCallback2 mDataSourceCallback2;

    CallbackMediaItem2(Builder builder) {
        super(builder);
        mDataSourceCallback2 = builder.mDataSourceCallback2;
    }

    /**
     * Return the DataSourceCallback2 that implements the callback for this data source.
     * @return the DataSourceCallback2 that implements the callback for this data source,
     */
    public @NonNull DataSourceCallback2 getDataSourceCallback2() {
        return mDataSourceCallback2;
    }

    /**
     * This Builder class simplifies the creation of a {@link CallbackMediaItem2} object.
     */
    public static final class Builder extends MediaItemDesc2.Builder<Builder> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        DataSourceCallback2 mDataSourceCallback2;

        /**
         * Creates a new Builder object.
         * @param dsc2 the DataSourceCallback2 for the media you want to play
         */
        public Builder(@NonNull DataSourceCallback2 dsc2) {
            super(FLAG_PLAYABLE);
            Preconditions.checkNotNull(dsc2);
            mDataSourceCallback2 = dsc2;
        }

        /**
         * @return A new CallbackMediaItem2 with values supplied by the Builder.
         */
        @Override
        public CallbackMediaItem2 build() {
            return new CallbackMediaItem2(this);
        }
    }
}
