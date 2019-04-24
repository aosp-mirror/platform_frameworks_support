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

<<<<<<< HEAD   (ae0664 Merge "Merge empty history for sparse-5426435-L2400000029299)
import androidx.lifecycle.LifecycleOwner;
=======
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
>>>>>>> BRANCH (9dc980 Merge "Merge cherrypicks of [950856] into sparse-5498091-L95)

/**
<<<<<<< HEAD   (ae0664 Merge "Merge empty history for sparse-5426435-L2400000029299)
 * Interface for handling {@link ComponentActivity#onBackPressed()} callbacks without
=======
 * Class for handling {@link OnBackPressedDispatcher#onBackPressed()} callbacks without
>>>>>>> BRANCH (9dc980 Merge "Merge cherrypicks of [950856] into sparse-5498091-L95)
 * strongly coupling that implementation to a subclass of {@link ComponentActivity}.
 * <p>
 * This class maintains its own {@link #isEnabled() enabled state}. Only when this callback
 * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
 * <p>
 * Note that the enabled state is an additional layer on top of the
 * {@link androidx.lifecycle.LifecycleOwner} passed to
 * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
 * which controls when the callback is added and removed to the dispatcher.
 * <p>
 * By calling {@link #remove()}, this callback will be removed from any
 * {@link OnBackPressedDispatcher} it has been added to. It is strongly recommended
 * to instead disable this callback to handle temporary changes in state.
 *
 * @see ComponentActivity#addOnBackPressedCallback(LifecycleOwner, OnBackPressedCallback)
 * @see ComponentActivity#removeOnBackPressedCallback(OnBackPressedCallback)
 */
public abstract class OnBackPressedCallback {

    private boolean mEnabled;
    private ArrayList<Cancellable> mCancellables = new ArrayList<>();

    /**
     * Create a {@link OnBackPressedCallback}.
     *
     * @param enabled The default enabled state for this callback.
     * @see #setEnabled(boolean)
     */
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
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Checks whether this callback should be considered enabled. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     *
     * @return Whether this callback should be considered enabled.
     */
    @MainThread
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Removes this callback from any {@link OnBackPressedDispatcher} it is currently
     * added to.
     */
    @MainThread
    public void remove() {
        for (Cancellable cancellable: mCancellables) {
            cancellable.cancel();
        }
    }

    /**
<<<<<<< HEAD   (ae0664 Merge "Merge empty history for sparse-5426435-L2400000029299)
     * Callback for handling the {@link ComponentActivity#onBackPressed()} event.
     *
     * @return True if you handled the {@link ComponentActivity#onBackPressed()} event. No
     * further {@link OnBackPressedCallback} instances will be called if you return true.
=======
     * Callback for handling the {@link OnBackPressedDispatcher#onBackPressed()} event.
>>>>>>> BRANCH (9dc980 Merge "Merge cherrypicks of [950856] into sparse-5498091-L95)
     */
    @MainThread
    public abstract void handleOnBackPressed();

    void addCancellable(@NonNull Cancellable cancellable) {
        mCancellables.add(cancellable);
    }

    void removeCancellable(@NonNull Cancellable cancellable) {
        mCancellables.remove(cancellable);
    }
}
