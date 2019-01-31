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

package androidx.savedstate;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

/**
 * An API for {@link SavedStateRegistryOwner} implementations to control {@link SavedStateRegistry},
 * it should call {@link #performRestore(Bundle)} to restore state {@link SavedStateRegistry}
 * and {@link #performSave(Bundle)} to gather SavedState from {@link SavedStateRegistry}
 */
public final class SavedStateRegistryMaster {
    private SavedStateRegistry mRegistry = new SavedStateRegistry();

    private SavedStateRegistryMaster() {}

    /**
     * Returns controlled {@link SavedStateRegistry}
     */
    public SavedStateRegistry getSavedStateRegistry() {
        return mRegistry;
    }

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry} to restore saved state.
     *
     * @param savedState restored state
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    public void performRestore(@Nullable Bundle savedState) {
        mRegistry.performRestore(savedState);
    }

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry}
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    public void performSave(@NonNull Bundle outBundle) {
        mRegistry.performSave(outBundle);
    }

    /**
     * Creates a {@link SavedStateRegistryMaster}.
     * <p>
     * It should be called during construction time of {@link SavedStateRegistryOwner}
     */
    @NonNull
    public static  SavedStateRegistryMaster create(SavedStateRegistryOwner owner) {
        if (owner.getLifecycle().getCurrentState() != Lifecycle.State.INITIALIZED) {
            throw new IllegalStateException("Restarter must be created only during "
                    + "owner's initialization stage");
        }
        owner.getLifecycle().addObserver(new Recreator(owner));
        return new SavedStateRegistryMaster();
    }
}
