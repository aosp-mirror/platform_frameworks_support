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

package androidx.navigation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Representation of an entry in the back stack of a {@link NavController}.
 */
final class NavBackStackEntry {
    private final NavDestination mDestination;
    private final Bundle mArgs;

<<<<<<< HEAD   (b71d1d Merge "Merge empty history for sparse-5418436-L4700000028919)
=======
    // Internal unique name for this navBackStackEntry;
    @NonNull
    final UUID mId;

>>>>>>> BRANCH (36b8b1 Merge "Merge cherrypicks of [937288, 937289] into sparse-542)
    NavBackStackEntry(@NonNull NavDestination destination, @Nullable Bundle args) {
        this(UUID.randomUUID(), destination, args);
    }

    NavBackStackEntry(@NonNull UUID uuid, @NonNull NavDestination destination,
            @Nullable Bundle args) {
        mId = uuid;
        mDestination = destination;
        mArgs = args;
    }

    /**
     * Gets the destination associated with this entry
     * @return The destination that is currently visible to users
     */
    @NonNull
    public NavDestination getDestination() {
        return mDestination;
    }

    /**
     * Gets the arguments used for this entry
     * @return The arguments used when this entry was created
     */
    @Nullable
    public Bundle getArguments() {
        return mArgs;
    }
}
