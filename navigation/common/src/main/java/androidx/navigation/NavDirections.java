<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
/*
 * Copyright 2017 The Android Open Source Project
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
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;

/**
 * An interface that describes a navigation operation: action's id and arguments
 */
public interface NavDirections {

    /**
     * Returns a action id to navigate with.
     *
     * @return id of an action
     */
    @IdRes
    int getActionId();

    /**
     * Returns arguments to pass to the destination
     */
    @NonNull
    Bundle getArguments();
}
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)
