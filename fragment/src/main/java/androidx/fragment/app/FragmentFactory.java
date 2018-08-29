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

package androidx.fragment.app;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface used to control the instantiation of {@link Fragment} instances.
 * Implementations can be registered with a {@link FragmentManager} via
 * {@link FragmentManager#setFragmentFactory(FragmentFactory)}.
 *
 * @see FragmentManager#setFragmentFactory(FragmentFactory)
 */
public class FragmentFactory {
    /**
     * Create a new instance of a Fragment with the given class name. This uses
     * {@link Fragment#parseFragmentClass(ClassLoader, String)} and the empty
     * constructor of the resulting Class by default.
     *
     * @param classLoader The default classloader to use for instantiation
     * @param className The class name of the fragment to instantiate.
     * @return Returns a new fragment instance.
     * @throws InstantiationException If there is a failure in instantiating
     * the given fragment class.  This is a runtime exception; it is not
     * normally expected to happen.
     */
    @NonNull
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        try {
            Class<? extends Fragment> cls = Fragment.parseFragmentClass(classLoader, className);
            return cls.getConstructor().newInstance();
        } catch (java.lang.InstantiationException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className
                    + ": calling Fragment constructor caused an exception", e);
        }
    }
}
