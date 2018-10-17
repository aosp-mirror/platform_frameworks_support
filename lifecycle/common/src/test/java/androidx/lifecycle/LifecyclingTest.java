/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.lifecycle.observers.DerivedSequence1;
import androidx.lifecycle.observers.DerivedSequence2;
import androidx.lifecycle.observers.DerivedWithNewMethods;
import androidx.lifecycle.observers.DerivedWithNoNewMethods;
import androidx.lifecycle.observers.DerivedWithOverridenMethodsWithLfAnnotation;
import androidx.lifecycle.observers.InterfaceImpl1;
import androidx.lifecycle.observers.InterfaceImpl2;
import androidx.lifecycle.observers.InterfaceImpl3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LifecyclingTest {

    @Test
    public void testDerivedWithNewLfMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(new DerivedWithNewMethods());
        assertThat(callback, instanceOf(ReflectiveGenericLifecycleObserver.class));
    }

    @Test
    public void testDerivedWithNoNewLfMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(new DerivedWithNoNewMethods());
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    @Test
    public void testDerivedWithOverridenMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(
                new DerivedWithOverridenMethodsWithLfAnnotation());
        // that is not effective but...
        assertThat(callback, instanceOf(ReflectiveGenericLifecycleObserver.class));
    }

    @Test
    public void testInterfaceImpl1NoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(new InterfaceImpl1());
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    @Test
    public void testInterfaceImpl2NoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(new InterfaceImpl2());
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
    }

    @Test
    public void testInterfaceImpl3NoGeneratedAdapter() {
        LifecycleEventObserver callback = Lifecycling.getCallback(new InterfaceImpl3());
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
    }

    @Test
    public void testDerivedSequence() {
        LifecycleEventObserver callback2 = Lifecycling.getCallback(new DerivedSequence2());
        assertThat(callback2, instanceOf(ReflectiveGenericLifecycleObserver.class));
        LifecycleEventObserver callback1 = Lifecycling.getCallback(new DerivedSequence1());
        assertThat(callback1, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    // MUST BE HERE TILL Lifecycle 3.0.0 release for back-compatibility with other modules
    @Test
    public void testDeprecatedGenericLifecycleObserver() {
        GenericLifecycleObserver genericLifecycleObserver = new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            }
        };
        LifecycleEventObserver observer = Lifecycling.getCallback(genericLifecycleObserver);
        assertThat(observer, is(observer));
    }
}
