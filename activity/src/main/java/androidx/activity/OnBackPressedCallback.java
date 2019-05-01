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

package androidx.activity;

import androidx.lifecycle.LifecycleOwner;

/**
 * Interface for handling {@link ComponentActivity#onBackPressed()} callbacks without
 * strongly coupling that implementation to a subclass of {@link ComponentActivity}.
 *
 * @see ComponentActivity#addOnBackPressedCallback(LifecycleOwner, OnBackPressedCallback)
 * @see ComponentActivity#removeOnBackPressedCallback(OnBackPressedCallback)
 */
public interface OnBackPressedCallback {
    /**
     * Callback for handling the {@link ComponentActivity#onBackPressed()} event.
     *
     * @return True if you handled the {@link ComponentActivity#onBackPressed()} event. No
     * further {@link OnBackPressedCallback} instances will be called if you return true.
     */
<<<<<<< HEAD   (e53308 Merge "Merge empty history for sparse-5498091-L6460000030224)
    boolean handleOnBackPressed();
=======
    public OnBackPressedCallback(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Set the enabled state of the callback. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     * <p>
     * Note that the enabled state is an additional layer on top of the
     * {@link androidx.lifecycle.LifecycleOwner} passed to
     * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
     * which controls when the callback is added and removed to the dispatcher.
     *
     * @param enabled whether the callback should be considered enabled
     */
    @MainThread
    public final void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Checks whether this callback should be considered enabled. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     *
     * @return Whether this callback should be considered enabled.
     */
    @MainThread
    public final boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Removes this callback from any {@link OnBackPressedDispatcher} it is currently
     * added to.
     */
    @MainThread
    public final void remove() {
        for (Cancellable cancellable: mCancellables) {
            cancellable.cancel();
        }
    }

    /**
     * Callback for handling the {@link OnBackPressedDispatcher#onBackPressed()} event.
     */
    @MainThread
    public abstract void handleOnBackPressed();

    void addCancellable(@NonNull Cancellable cancellable) {
        mCancellables.add(cancellable);
    }

    void removeCancellable(@NonNull Cancellable cancellable) {
        mCancellables.remove(cancellable);
    }
>>>>>>> BRANCH (3a06c2 Merge "Merge cherrypicks of [954920] into sparse-5520679-L60)
}
