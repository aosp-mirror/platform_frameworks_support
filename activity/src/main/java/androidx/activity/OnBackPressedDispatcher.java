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
import androidx.annotation.Nullable;
import androidx.core.app.Subscription;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Dispatcher that can be used to register {@link OnBackPressedCallback} instances for handling
 * the {@link ComponentActivity#onBackPressed()} callback via composition.
 * <pre>
 * public class FormEntryFragment extends Fragment {
 *     {@literal @}Override
 *     public void onAttach({@literal @}NonNull Context context) {
 *         super.onAttach(context);
 *         requireActivity().getOnBackPressedDispatcher().addCallback(this,
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayDeque<OnBackPressedCallback> mOnBackPressedCallbacks = new ArrayDeque<>();

    OnBackPressedDispatcher() {
    }

    /**
     * Add a new {@link OnBackPressedCallback}. Callbacks are invoked in the reverse order in which
     * they are added, so this newly added {@link OnBackPressedCallback} will be the first
     * callback to receive a callback if {@link #onBackPressed()} is called.
     * <p>
     * This method is <strong>not</strong> {@link Lifecycle} aware - if you'd like to ensure that
     * you only get callbacks when at least {@link Lifecycle.State#STARTED started}, use
     * {@link #addCallback(LifecycleOwner, OnBackPressedCallback)}.
     *
     * @param onBackPressedCallback The callback to add
     * @return a {@link Subscription} which can be used to {@link Subscription#cancel() cancel}
     * the callback's subscription and remove it from the set of OnBackPressedCallbacks. The
     * callback won't be called for any future {@link #onBackPressed()} calls, but may still
     * receive a callback if this method is called during the dispatch of an ongoing
     * {@link #onBackPressed()} call.
     *
     * @see #onBackPressed()
     */
    @NonNull
    public Subscription addCallback(@NonNull OnBackPressedCallback onBackPressedCallback) {
        synchronized (mOnBackPressedCallbacks) {
            mOnBackPressedCallbacks.add(onBackPressedCallback);
        }
        return new OnBackPressedSubscription(onBackPressedCallback);
    }

    /**
     * Receive callbacks to a new {@link OnBackPressedCallback} when the given
     * {@link LifecycleOwner} is at least {@link Lifecycle.State#STARTED started}.
     * <p>
     * This will automatically call {@link #addCallback(OnBackPressedCallback)} and
     * {@link Subscription#cancel()} as the lifecycle state changes.
     * As a corollary, if your lifecycle is already at least
     * {@link Lifecycle.State#STARTED started}, calling this method will result in an immediate
     * call to {@link #addCallback(OnBackPressedCallback)}.
     * <p>
     * When the {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}, it will
     * automatically be removed from the list of callbacks. The only time you would need to
     * manually call {@link Subscription#cancel()} on the returned {@link Subscription} is if
     * you'd like to remove the callback prior to destruction of the associated lifecycle.
     *
     * @param owner The LifecycleOwner which controls when the callback should be invoked
     * @param onBackPressedCallback The callback to add
     * @return a {@link Subscription} which can be used to {@link Subscription#cancel() cancel}
     * the callback's subscription and remove the associated
     * {@link androidx.lifecycle.LifecycleObserver} and the OnBackPressedCallback. The
     * callback won't be called for any future {@link #onBackPressed()} calls, but may still
     * receive a callback if this method is called during the dispatch of an ongoing
     * {@link #onBackPressed()} call.
     *
     * @see #onBackPressed()
     */
    @NonNull
    public Subscription addCallback(@NonNull LifecycleOwner owner,
            @NonNull OnBackPressedCallback onBackPressedCallback) {
        Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return new Subscription() {
                @Override
                public void cancel() {
                }

                @Override
                public boolean isCancelled() {
                    return true;
                }
            };
        }
        return new LifecycleOnBackPressedSubscription(lifecycle, onBackPressedCallback);
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

    private class OnBackPressedSubscription implements Subscription {
        private final OnBackPressedCallback mOnBackPressedCallback;
        private boolean mCancelled;

        OnBackPressedSubscription(OnBackPressedCallback onBackPressedCallback) {
            mOnBackPressedCallback = onBackPressedCallback;
        }

        @Override
        public void cancel() {
            synchronized (mOnBackPressedCallbacks) {
                mOnBackPressedCallbacks.remove(mOnBackPressedCallback);
                mCancelled = true;
            }
        }

        @Override
        public boolean isCancelled() {
            return mCancelled;
        }
    }

    private class LifecycleOnBackPressedSubscription implements GenericLifecycleObserver,
            Subscription {
        private final Lifecycle mLifecycle;
        private final OnBackPressedCallback mOnBackPressedCallback;

        @Nullable
        private Subscription mCurrentSubscription;
        private boolean mCancelled = false;

        LifecycleOnBackPressedSubscription(@NonNull Lifecycle lifecycle,
                @NonNull OnBackPressedCallback onBackPressedCallback) {
            mLifecycle = lifecycle;
            mOnBackPressedCallback = onBackPressedCallback;
            lifecycle.addObserver(this);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_START) {
                mCurrentSubscription = addCallback(mOnBackPressedCallback);
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Should always be non-null
                if (mCurrentSubscription != null) {
                    mCurrentSubscription.cancel();
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                cancel();
            }
        }

        @Override
        public void cancel() {
            mLifecycle.removeObserver(this);
            if (mCurrentSubscription != null) {
                mCurrentSubscription.cancel();
                mCurrentSubscription = null;
            }
            mCancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return mCancelled;
        }
    }
}
