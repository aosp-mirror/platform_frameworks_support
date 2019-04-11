/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.testing;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * The types of requests that {@link TestScheduler} can wait for to be completed (drained).
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class Drainable {
    Drainable() {
        // Restricting access to the constructor, to give Drainable a sealed class
        // like behavior.
    }

    /**
     * Represents a request for all {@link androidx.work.impl.model.WorkSpec}s to be drained.
     */
    static final class DrainAll extends Drainable {
        DrainAll() {
            super();
        }

        @NonNull
        @Override
        public String toString() {
            return "DrainAll";
        }
    }

    /**
     * Represents a request to wait for a {@link androidx.work.impl.model.WorkSpec} id to be
     * drained.
     */
    static final class DrainWorkSpec extends Drainable {
        private final String mId;

        DrainWorkSpec(@NonNull String id) {
            mId = id;
        }

        @NonNull
        public String getId() {
            return mId;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("DrainWorkSpec[%s]", mId);
        }
    }
}
