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

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ReportFragment;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for activities that enables composition of higher level components.
 * <p>
 * Rather than all functionality being built directly into this class, only the minimal set of
 * lower level building blocks are included. Higher level components can then be used as needed
 * without enforcing a deep Activity class hierarchy or strong coupling between components.
 */
public class ComponentActivity extends androidx.core.app.ComponentActivity
        implements LifecycleOwner {

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final CopyOnWriteArrayList<LifecycleAwareOnBackPressedHandler> mOnBackPressedHandlers =
            new CopyOnWriteArrayList<>();

    @Override
    @SuppressWarnings("RestrictedApi")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
    }

    @CallSuper
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. Any {@link OnBackPressedHandler} added via
     * {@link #addOnBackPressedHandler(LifecycleOwner, OnBackPressedHandler)} will be given a
     * chance to handle the back button before the default behavior of
     * {@link android.app.Activity#onBackPressed()} is invoked.
     *
     * @see #addOnBackPressedHandler(LifecycleOwner, OnBackPressedHandler)
     */
    @Override
    public void onBackPressed() {
        for (LifecycleAwareOnBackPressedHandler onBackPressedHandler : mOnBackPressedHandlers) {
            if (onBackPressedHandler.dispatchOnBackPressed()) {
                return;
            }
        }
        // If none of the registered OnBackPressedHandlers handled the back button,
        // delegate to the super implementation
        super.onBackPressed();
    }

    /**
     * Add a new {@link OnBackPressedHandler}. Handlers are invoked in order of recency, so
     * this newly added {@link OnBackPressedHandler} will be the first handler to receive a
     * callback if {@link #onBackPressed()} is called. Only if this handler returns
     * <code>false</code> from its {@link OnBackPressedHandler#onBackPressed()} will any
     * previously added handlers be called.
     * <p>
     * The {@link OnBackPressedHandler#onBackPressed()} callback will only be called if the
     * given {@link LifecycleOwner} is at least {@link Lifecycle.State#STARTED}. When the
     * {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}, it will automatically
     * be removed from the list of handlers. The only time you would need to manually call
     * {@link #removeOnBackPressedHandler(OnBackPressedHandler)} is if you'd like to remove the
     * handler prior to destruction of the associated lifecycle.
     *
     * @param owner The LifecycleOwner which controls when the handler should be invoked
     * @param onBackPressedHandler The handler to add
     *
     * @see #onBackPressed()
     * @see #removeOnBackPressedHandler(OnBackPressedHandler)
     */
    public void addOnBackPressedHandler(@NonNull LifecycleOwner owner,
            @NonNull OnBackPressedHandler onBackPressedHandler) {
        if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            // Already destroyed, nothing to do
            return;
        }
        // Add new handlers to the front of the list so that
        // the most recently added handlers get priority
        mOnBackPressedHandlers.add(0, new LifecycleAwareOnBackPressedHandler(
                owner.getLifecycle(), onBackPressedHandler));
    }

    /**
     * Remove a previously
     * {@link #addOnBackPressedHandler(LifecycleOwner, OnBackPressedHandler) added}
     * {@link OnBackPressedHandler} instance. The handler won't be called for any future
     * {@link #onBackPressed()} calls, but may still receive a callback if this method is called
     * during the dispatch of an ongoing {@link #onBackPressed()} call.
     * <p>
     * This call is usually not necessary as handlers will be automatically removed when their
     * associated {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}.
     *
     * @param onBackPressedHandler The handler to remove
     * @see #addOnBackPressedHandler(LifecycleOwner, OnBackPressedHandler)
     */
    public void removeOnBackPressedHandler(@NonNull OnBackPressedHandler onBackPressedHandler) {
        Iterator<LifecycleAwareOnBackPressedHandler> iterator =
                mOnBackPressedHandlers.iterator();
        LifecycleAwareOnBackPressedHandler handlerToRemove = null;
        while (iterator.hasNext()) {
            LifecycleAwareOnBackPressedHandler handler = iterator.next();
            if (handler.getOnBackPressedHandler().equals(onBackPressedHandler)) {
                handlerToRemove = handler;
                break;
            }
        }
        if (handlerToRemove != null) {
            handlerToRemove.onRemoved();
            mOnBackPressedHandlers.remove(handlerToRemove);
        }
    }

    private class LifecycleAwareOnBackPressedHandler implements GenericLifecycleObserver {
        private final Lifecycle mLifecycle;
        private final OnBackPressedHandler mOnBackPressedHandler;

        LifecycleAwareOnBackPressedHandler(@NonNull Lifecycle lifecycle,
                @NonNull OnBackPressedHandler onBackPressedHandler) {
            mLifecycle = lifecycle;
            mOnBackPressedHandler = onBackPressedHandler;
            mLifecycle.addObserver(this);
        }

        Lifecycle getLifecycle() {
            return mLifecycle;
        }

        OnBackPressedHandler getOnBackPressedHandler() {
            return mOnBackPressedHandler;
        }

        boolean dispatchOnBackPressed() {
            if (mLifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                return mOnBackPressedHandler.onBackPressed();
            }
            return false;
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                synchronized (mOnBackPressedHandlers) {
                    mLifecycle.removeObserver(this);
                    mOnBackPressedHandlers.remove(this);
                }
            }
        }

        public void onRemoved() {
            mLifecycle.removeObserver(this);
        }
    }
}
