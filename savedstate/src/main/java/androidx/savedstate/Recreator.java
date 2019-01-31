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

package androidx.savedstate;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;

class Recreator implements GenericLifecycleObserver {

    static final String CLASSES_KEY = "key";
    static final String COMPONENT_KEY = "androidx.savedstate.Restarter";

    private final SavedStateRegistryOwner mOwner;

    Recreator(SavedStateRegistryOwner owner) {
        mOwner = owner;
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (event != Lifecycle.Event.ON_CREATE) {
            throw new AssertionError("Next event must be ON_CREATE");
        }
        source.getLifecycle().removeObserver(this);
        Bundle bundle = mOwner.getSavedStateRegistry()
                .consumeRestoredStateForKey(COMPONENT_KEY);
        if (bundle != null) {
            explodeClasses(bundle.getStringArrayList(CLASSES_KEY));
        }
    }

    private void explodeClasses(ArrayList<String> classes) {
        if (classes == null) {
            return;
        }
        for (String className : classes) {
            reflectiveNew(className);
        }
    }

    private void reflectiveNew(String className) {
        Object newInstance;
        try {
            Class<?> clazz = Class.forName(className);
            newInstance = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + className, e);
        }
        if (newInstance instanceof SavedStateRegistry.AutoRecreated) {
            //noinspection unchecked
            ((SavedStateRegistry.AutoRecreated) newInstance).onRecreated(mOwner);
        } else {
            throw new IllegalStateException("Clazz " + className + " must be a subclass of "
                    + SavedStateRegistry.AutoRecreated.class);
        }
    }

    static final class SavedStateProvider implements SavedStateRegistry.SavedStateProvider {
        static final String CLASSES_KEY = "key";
        @SuppressWarnings("WeakerAccess") // synthetic access
        final ArrayList<String> mClasses = new ArrayList<>();

        SavedStateProvider(final SavedStateRegistry registry) {
            registry.registerSavedStateProvider(COMPONENT_KEY, this);
        }

        @NonNull
        @Override
        public Bundle saveState() {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(CLASSES_KEY, mClasses);
            return bundle;
        }

        void add(String className) {
            mClasses.add(className);
        }
    }
}
