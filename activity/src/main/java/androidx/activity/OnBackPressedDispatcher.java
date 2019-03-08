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

package androidx.activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Dispatcher that can be used to register {@link OnBackPressedCallback} instances for handling
 * the {@link ComponentActivity#onBackPressed()} callback via composition.
 * <pre>
 * public class FormEntryFragment extends Fragment {
 *     {@literal @}Override
 *     public void onAttach({@literal @}NonNull Context context) {
 *         super.onAttach(context);
 *         requireActivity().getOnBackPressedDispatcher().observeOnBackPressed(this,
 *                 new OnBackPressedCallback() {
 *                     {@literal @}Override
 *                     public boolean handleOnBackPressed() {
 *                         showAreYouSureDialog();
 *                         return true;
 *                     }
 *                 });
 *     }
 * }
 * </pre>
 */
public final class OnBackPressedDispatcher {

    private final ArrayDeque<OnBackPressedCallback> mOnBackPressedCallbacks = new ArrayDeque<>();
    final HashMap<OnBackPressedCallback, ArrayList<WeakObserver>> mObservers = new HashMap<>();

    OnBackPressedDispatcher() {
    }

    /**
     * Add a new {@link OnBackPressedCallback}. Callbacks are invoked in the reverse order in which
     * they are added, so this newly added {@link OnBackPressedCallback} will be the first
     * callback to receive a callback if {@link #onBackPressed()} is called.
     * <p>
     * This method is <strong>not</strong> {@link Lifecycle} aware - if you'd like to ensure that
     * you only get callbacks when at least {@link Lifecycle.State#STARTED started}, use
     * {@link #observeOnBackPressed(LifecycleOwner, OnBackPressedCallback)}.
     *
     * @param onBackPressedCallback The callback to add
     *
     * @see #onBackPressed()
     * @see #removeOnBackPressedCallback(OnBackPressedCallback)
     */
    public void addOnBackPressedCallback(@NonNull OnBackPressedCallback onBackPressedCallback) {
        synchronized (mOnBackPressedCallbacks) {
            mOnBackPressedCallbacks.add(onBackPressedCallback);
        }
    }

    /**
     * Receive callbacks to a new {@link OnBackPressedCallback} when the given
     * {@link LifecycleOwner} is at least {@link Lifecycle.State#STARTED started}.
     * <p>
     * This will automatically call {@link #addOnBackPressedCallback(OnBackPressedCallback)} and
     * {@link #removeOnBackPressedCallback(OnBackPressedCallback)} as the lifecycle state changes.
     * As a corollary, if your lifecycle is already at least
     * {@link Lifecycle.State#STARTED started}, calling this method will result in an immediate
     * call to {@link #addOnBackPressedCallback(OnBackPressedCallback)}.
     * <p>
     * When the {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}, it will
     * automatically be removed from the list of callbacks. The only time you would need to
     * manually call {@link #removeOnBackPressedCallback(OnBackPressedCallback)} is if you'd like
     * to remove the callback prior to destruction of the associated lifecycle.
     *
     * @param owner The LifecycleOwner which controls when the callback should be invoked
     * @param onBackPressedCallback The callback to add
     *
     * @see #onBackPressed()
     * @see #removeOnBackPressedCallback(OnBackPressedCallback)
     */
    public void observeOnBackPressed(@NonNull LifecycleOwner owner,
            @NonNull OnBackPressedCallback onBackPressedCallback) {
        synchronized (mOnBackPressedCallbacks) {
            Lifecycle lifecycle = owner.getLifecycle();
            if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
                return;
            }
            WeakObserver observer = new WeakObserver(onBackPressedCallback);
            if (!mObservers.containsKey(onBackPressedCallback)) {
                mObservers.put(onBackPressedCallback, new ArrayList<WeakObserver>());
            }
            ArrayList<WeakObserver> observers = mObservers.get(onBackPressedCallback);
            observers.add(observer);
            lifecycle.addObserver(observer);
        }
    }

    /**
     * Remove a previously added {@link OnBackPressedCallback} instance. The callback won't be
     * called for any future {@link #onBackPressed()} calls, but may still receive a callback
     * if this method is called during the dispatch of an ongoing {@link #onBackPressed()} call.
     *
     * @param onBackPressedCallback The callback to remove
     * @see #addOnBackPressedCallback(OnBackPressedCallback)
     * @see #observeOnBackPressed(LifecycleOwner, OnBackPressedCallback)
     */
    public void removeOnBackPressedCallback(@NonNull OnBackPressedCallback onBackPressedCallback) {
        removeOnBackPressedCallback(onBackPressedCallback, true);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void removeOnBackPressedCallback(@NonNull OnBackPressedCallback onBackPressedCallback,
            boolean removeObservers) {
        synchronized (mOnBackPressedCallbacks) {
            mOnBackPressedCallbacks.remove(onBackPressedCallback);
            if (removeObservers) {
                mObservers.remove(onBackPressedCallback);
            }
        }
    }

    /**
     * Trigger a call to the currently added {@link OnBackPressedCallback callbacks} in reverse
     * order in which they were added. Only if the most recently added callback returns
     * <code>false</code> from its {@link OnBackPressedCallback#handleOnBackPressed()}
     * will any previously added callback be called.
     *
     * @return True if an added {@link OnBackPressedCallback} handled the back button.
     */
    public boolean onBackPressed() {
        synchronized (mOnBackPressedCallbacks) {
            Iterator<OnBackPressedCallback> iterator =
                    mOnBackPressedCallbacks.descendingIterator();
            while (iterator.hasNext()) {
                if (iterator.next().handleOnBackPressed()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class WeakObserver implements GenericLifecycleObserver {
        private final WeakReference<OnBackPressedCallback> mOnBackPressedCallback;

        WeakObserver(@NonNull OnBackPressedCallback onBackPressedCallback) {
            mOnBackPressedCallback = new WeakReference<>(onBackPressedCallback);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            OnBackPressedCallback callback = mOnBackPressedCallback.get();
            if (callback == null || !mObservers.containsKey(callback)) {
                // Callback was removed, so we no longer need to listen for events
                source.getLifecycle().removeObserver(this);
            } else if (event == Lifecycle.Event.ON_START) {
                addOnBackPressedCallback(callback);
            } else if (event == Lifecycle.Event.ON_STOP) {
                removeOnBackPressedCallback(callback, false);
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                removeOnBackPressedCallback(callback, true);
                // Clean up our observer
                source.getLifecycle().removeObserver(this);
            }
        }
    }
}
