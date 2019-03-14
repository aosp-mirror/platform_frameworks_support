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

import static androidx.fragment.app.FragmentTestUtil.HostCallbacks;
import static androidx.fragment.app.FragmentTestUtil.restartFragmentController;
import static androidx.fragment.app.FragmentTestUtil.shutdownFragmentController;
import static androidx.fragment.app.FragmentTestUtil.startupFragmentController;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.test.EmptyFragmentTestActivity;
import androidx.fragment.test.R;
import androidx.lifecycle.ViewModelStore;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class FragmentLifecycleTestJava {

    @Rule
    public ActivityTestRule<EmptyFragmentTestActivity> mActivityRule =
            new ActivityTestRule<EmptyFragmentTestActivity>(EmptyFragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void setInitialSavedState() throws Throwable {
        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // Add a StateSaveFragment
        StateSaveFragment fragment = new StateSaveFragment("Saved", "");
        fm.beginTransaction().add(fragment, "tag").commit();
        executePendingTransactions(fm);

        // Change the user visible hint before we save state
        fragment.setUserVisibleHint(false);

        // Save its state and remove it
        Fragment.SavedState state = fm.saveFragmentInstanceState(fragment);
        fm.beginTransaction().remove(fragment).commit();
        executePendingTransactions(fm);

        // Create a new instance, calling setInitialSavedState
        fragment = new StateSaveFragment("", "");
        fragment.setInitialSavedState(state);

        // Add the new instance
        fm.beginTransaction().add(fragment, "tag").commit();
        executePendingTransactions(fm);

        assertEquals("setInitialSavedState did not restore saved state",
                "Saved", fragment.getSavedState());
        assertEquals("setInitialSavedState did not restore user visible hint",
                false, fragment.getUserVisibleHint());
    }

    @Test
    @UiThreadTest
    public void setInitialSavedStateWithSetUserVisibleHint() throws Throwable {
        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // Add a StateSaveFragment
        StateSaveFragment fragment = new StateSaveFragment("Saved", "");
        fm.beginTransaction().add(fragment, "tag").commit();
        executePendingTransactions(fm);

        // Save its state and remove it
        Fragment.SavedState state = fm.saveFragmentInstanceState(fragment);
        fm.beginTransaction().remove(fragment).commit();
        executePendingTransactions(fm);

        // Create a new instance, calling setInitialSavedState
        fragment = new StateSaveFragment("", "");
        fragment.setInitialSavedState(state);

        // Change the user visible hint after we call setInitialSavedState
        fragment.setUserVisibleHint(false);

        // Add the new instance
        fm.beginTransaction().add(fragment, "tag").commit();
        executePendingTransactions(fm);

        assertEquals("setInitialSavedState did not restore saved state",
                "Saved", fragment.getSavedState());
        assertEquals("setUserVisibleHint should override setInitialSavedState",
                false, fragment.getUserVisibleHint());
    }

    @Test
    @UiThreadTest
    public void testSavedInstanceStateAfterRestore() {

        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);
        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        // Add the initial state
        final StrictFragment parentFragment = new StrictFragment();
        parentFragment.setRetainInstance(true);
        final StrictFragment childFragment = new StrictFragment();
        fm1.beginTransaction().add(parentFragment, "parent").commitNow();
        final FragmentManager childFragmentManager = parentFragment.getChildFragmentManager();
        childFragmentManager.beginTransaction().add(childFragment, "child").commitNow();

        // Confirm the initial state
        assertWithMessage("Initial parent saved instance state should be null")
                .that(parentFragment.mSavedInstanceState)
                .isNull();
        assertWithMessage("Initial child saved instance state should be null")
                .that(childFragment.mSavedInstanceState)
                .isNull();

        // Bring the state back down to destroyed, simulating an activity restart
        fc1.dispatchPause();
        final Parcelable savedState = fc1.saveAllState();
        fc1.dispatchStop();
        fc1.dispatchDestroy();

        // Create the new controller and restore state
        final FragmentController fc2 =
                startupFragmentController(mActivityRule.getActivity(), savedState, viewModelStore);
        final FragmentManager fm2 = fc2.getSupportFragmentManager();

        final StrictFragment restoredParentFragment = (StrictFragment) fm2
                .findFragmentByTag("parent");
        assertNotNull("Parent fragment was not restored", restoredParentFragment);
        final StrictFragment restoredChildFragment = (StrictFragment) restoredParentFragment
                .getChildFragmentManager().findFragmentByTag("child");
        assertNotNull("Child fragment was not restored", restoredChildFragment);

        assertWithMessage("Parent fragment saved instance state should still be null "
                + "since it is a retained Fragment")
                .that(restoredParentFragment.mSavedInstanceState)
                .isNull();
        assertWithMessage("Child fragment saved instance state should be non-null")
                .that(restoredChildFragment.mSavedInstanceState)
                .isNotNull();

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);
    }

    @Test
    @UiThreadTest
    public void restoreNestedFragmentsOnBackStack() {

        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        // Add the initial state
        final StrictFragment parentFragment = new StrictFragment();
        final StrictFragment childFragment = new StrictFragment();
        fm1.beginTransaction().add(parentFragment, "parent").commitNow();
        final FragmentManager childFragmentManager = parentFragment.getChildFragmentManager();
        childFragmentManager.beginTransaction().add(childFragment, "child").commitNow();

        // Now add a Fragment to the back stack
        final StrictFragment replacementChildFragment = new StrictFragment();
        childFragmentManager.beginTransaction()
                .remove(childFragment)
                .add(replacementChildFragment, "child")
                .addToBackStack("back_stack").commit();
        childFragmentManager.executePendingTransactions();

        // Move the activity to resumed
        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Now bring the state back down
        fc1.dispatchPause();
        final Parcelable savedState = fc1.saveAllState();
        fc1.dispatchStop();
        fc1.dispatchDestroy();

        // Create the new controller and restore state
        final FragmentController fc2 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm2 = fc2.getSupportFragmentManager();

        fc2.attachHost(null);
        fc2.restoreSaveState(savedState);
        fc2.dispatchCreate();

        final StrictFragment restoredParentFragment = (StrictFragment) fm2
                .findFragmentByTag("parent");
        assertNotNull("Parent fragment was not restored", restoredParentFragment);
        final StrictFragment restoredChildFragment = (StrictFragment) restoredParentFragment
                .getChildFragmentManager().findFragmentByTag("child");
        assertNotNull("Child fragment was not restored", restoredChildFragment);

        fc2.dispatchActivityCreated();
        fc2.noteStateNotSaved();
        fc2.execPendingActions();
        fc2.dispatchStart();
        fc2.dispatchResume();
        fc2.execPendingActions();

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);
    }

    @Test
    @UiThreadTest
    public void restoreRetainedInstanceFragments() throws Throwable {
        // Create a new FragmentManager in isolation, nest some assorted fragments
        // and then restore them to a second new FragmentManager.

        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        // Configure fragments.

        // This retained fragment will be added, then removed. After being removed, it
        // should no longer be retained by the FragmentManager
        final StateSaveFragment removedFragment = new StateSaveFragment("Removed",
                "UnsavedRemoved");
        removedFragment.setRetainInstance(true);
        fm1.beginTransaction().add(removedFragment, "tag:removed").commitNow();
        fm1.beginTransaction().remove(removedFragment).commitNow();

        // This retained fragment will be added, then detached. After being detached, it
        // should continue to be retained by the FragmentManager
        final StateSaveFragment detachedFragment = new StateSaveFragment("Detached",
                "UnsavedDetached");
        removedFragment.setRetainInstance(true);
        fm1.beginTransaction().add(detachedFragment, "tag:detached").commitNow();
        fm1.beginTransaction().detach(detachedFragment).commitNow();

        // Grandparent fragment will not retain instance
        final StateSaveFragment grandparentFragment = new StateSaveFragment("Grandparent",
                "UnsavedGrandparent");
        assertNotNull("grandparent fragment saved state not initialized",
                grandparentFragment.getSavedState());
        assertNotNull("grandparent fragment unsaved state not initialized",
                grandparentFragment.getUnsavedState());
        fm1.beginTransaction().add(grandparentFragment, "tag:grandparent").commitNow();

        // Parent fragment will retain instance
        final StateSaveFragment parentFragment = new StateSaveFragment("Parent", "UnsavedParent");
        assertNotNull("parent fragment saved state not initialized",
                parentFragment.getSavedState());
        assertNotNull("parent fragment unsaved state not initialized",
                parentFragment.getUnsavedState());
        parentFragment.setRetainInstance(true);
        grandparentFragment.getChildFragmentManager().beginTransaction()
                .add(parentFragment, "tag:parent").commitNow();
        assertSame("parent fragment is not a child of grandparent",
                grandparentFragment, parentFragment.getParentFragment());

        // Child fragment will not retain instance
        final StateSaveFragment childFragment = new StateSaveFragment("Child", "UnsavedChild");
        assertNotNull("child fragment saved state not initialized",
                childFragment.getSavedState());
        assertNotNull("child fragment unsaved state not initialized",
                childFragment.getUnsavedState());
        parentFragment.getChildFragmentManager().beginTransaction()
                .add(childFragment, "tag:child").commitNow();
        assertSame("child fragment is not a child of grandpanret",
                parentFragment, childFragment.getParentFragment());

        // Saved for comparison later
        final FragmentManager parentChildFragmentManager = parentFragment.getChildFragmentManager();

        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Bring the state back down to destroyed, simulating an activity restart
        fc1.dispatchPause();
        final Parcelable savedState = fc1.saveAllState();
        fc1.dispatchStop();
        fc1.dispatchDestroy();

        // Create the new controller and restore state
        final FragmentController fc2 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm2 = fc2.getSupportFragmentManager();

        fc2.attachHost(null);
        fc2.restoreSaveState(savedState);
        fc2.dispatchCreate();

        // Confirm that the restored fragments are available and in the expected states
        final StateSaveFragment restoredRemovedFragment = (StateSaveFragment)
                fm2.findFragmentByTag("tag:removed");
        assertNull(restoredRemovedFragment);
        assertTrue("Removed Fragment should be destroyed", removedFragment.mCalledOnDestroy);

        final StateSaveFragment restoredDetachedFragment = (StateSaveFragment)
                fm2.findFragmentByTag("tag:detached");
        assertNotNull(restoredDetachedFragment);

        final StateSaveFragment restoredGrandparent = (StateSaveFragment) fm2.findFragmentByTag(
                "tag:grandparent");
        assertNotNull("grandparent fragment not restored", restoredGrandparent);

        assertNotSame("grandparent fragment instance was saved",
                grandparentFragment, restoredGrandparent);
        assertEquals("grandparent fragment saved state was not equal",
                grandparentFragment.getSavedState(), restoredGrandparent.getSavedState());
        assertNotEquals("grandparent fragment unsaved state was unexpectedly preserved",
                grandparentFragment.getUnsavedState(), restoredGrandparent.getUnsavedState());

        final StateSaveFragment restoredParent = (StateSaveFragment) restoredGrandparent
                .getChildFragmentManager().findFragmentByTag("tag:parent");
        assertNotNull("parent fragment not restored", restoredParent);

        assertSame("parent fragment instance was not saved", parentFragment, restoredParent);
        assertEquals("parent fragment saved state was not equal",
                parentFragment.getSavedState(), restoredParent.getSavedState());
        assertEquals("parent fragment unsaved state was not equal",
                parentFragment.getUnsavedState(), restoredParent.getUnsavedState());
        assertNotSame("parent fragment has the same child FragmentManager",
                parentChildFragmentManager, restoredParent.getChildFragmentManager());

        final StateSaveFragment restoredChild = (StateSaveFragment) restoredParent
                .getChildFragmentManager().findFragmentByTag("tag:child");
        assertNotNull("child fragment not restored", restoredChild);

        assertNotSame("child fragment instance state was saved", childFragment, restoredChild);
        assertEquals("child fragment saved state was not equal",
                childFragment.getSavedState(), restoredChild.getSavedState());
        assertNotEquals("child fragment saved state was unexpectedly equal",
                childFragment.getUnsavedState(), restoredChild.getUnsavedState());

        fc2.dispatchActivityCreated();
        fc2.noteStateNotSaved();
        fc2.execPendingActions();
        fc2.dispatchStart();
        fc2.dispatchResume();
        fc2.execPendingActions();

        // Test that the fragments are in the configuration we expect

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);

        assertTrue("grandparent not destroyed", restoredGrandparent.mCalledOnDestroy);
        assertTrue("parent not destroyed", restoredParent.mCalledOnDestroy);
        assertTrue("child not destroyed", restoredChild.mCalledOnDestroy);
    }

    @Test
    @UiThreadTest
    public void restoreRetainedInstanceFragmentWithTransparentActivityConfigChange() {
        // Create a new FragmentManager in isolation, add a retained instance Fragment,
        // then mimic the following scenario:
        // 1. Activity A adds retained Fragment F
        // 2. Activity A starts translucent Activity B
        // 3. Activity B start opaque Activity C
        // 4. Rotate phone
        // 5. Finish Activity C
        // 6. Finish Activity B

        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        // Add the retained Fragment
        final StateSaveFragment retainedFragment = new StateSaveFragment("Retained",
                "UnsavedRetained");
        retainedFragment.setRetainInstance(true);
        fm1.beginTransaction().add(retainedFragment, "tag:retained").commitNow();

        // Move the activity to resumed
        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Launch the transparent activity on top
        fc1.dispatchPause();

        // Launch the opaque activity on top
        final Parcelable savedState = fc1.saveAllState();
        fc1.dispatchStop();

        // Finish the opaque activity, making our Activity visible i.e., started
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();

        // Finish the transparent activity, causing a config change
        fc1.dispatchStop();
        fc1.dispatchDestroy();

        // Create the new controller and restore state
        final FragmentController fc2 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm2 = fc2.getSupportFragmentManager();

        fc2.attachHost(null);
        fc2.restoreSaveState(savedState);
        fc2.dispatchCreate();

        final StateSaveFragment restoredFragment = (StateSaveFragment) fm2
                .findFragmentByTag("tag:retained");
        assertNotNull("retained fragment not restored", restoredFragment);
        assertEquals("The retained Fragment shouldn't be recreated",
                retainedFragment, restoredFragment);

        fc2.dispatchActivityCreated();
        fc2.noteStateNotSaved();
        fc2.execPendingActions();
        fc2.dispatchStart();
        fc2.dispatchResume();
        fc2.execPendingActions();

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);
    }

    @Test
    @UiThreadTest
    public void saveAnimationState() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        FragmentController fc = startupFragmentController(mActivityRule.getActivity(), null,
                viewModelStore);
        FragmentManager fm = fc.getSupportFragmentManager();

        fm.beginTransaction()
                .setCustomAnimations(0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(android.R.id.content, SimpleFragment.create(R.layout.fragment_a))
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        // Causes save and restore of fragments and back stack
        fc = restartFragmentController(mActivityRule.getActivity(), fc, viewModelStore);
        fm = fc.getSupportFragmentManager();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, 0, 0)
                .replace(android.R.id.content, SimpleFragment.create(R.layout.fragment_b))
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0);

        // Causes save and restore of fragments and back stack
        fc = restartFragmentController(mActivityRule.getActivity(), fc, viewModelStore);
        fm = fc.getSupportFragmentManager();

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0);

        fm.popBackStackImmediate();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        shutdownFragmentController(fc, viewModelStore);
    }

    /**
     * Test to ensure that when dispatch* is called that the fragment manager
     * doesn't cause the contained fragment states to change even if no state changes.
     */
    @Test
    @UiThreadTest
    public void noPrematureStateChange() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        FragmentController fc = startupFragmentController(mActivityRule.getActivity(), null,
                viewModelStore);
        FragmentManager fm = fc.getSupportFragmentManager();

        fm.beginTransaction()
                .add(new StrictFragment(), "1")
                .commitNow();

        fc = restartFragmentController(mActivityRule.getActivity(), fc, viewModelStore);

        fm = fc.getSupportFragmentManager();

        StrictFragment fragment1 = (StrictFragment) fm.findFragmentByTag("1");
        assertWithMessage("Fragment should be resumed after restart")
                .that(fragment1.mCalledOnResume)
                .isTrue();
        fragment1.mCalledOnResume = false;
        fc.dispatchResume();

        assertWithMessage("Fragment should not get onResume() after second dispatchResume()")
                .that(fragment1.mCalledOnResume)
                .isFalse();
    }

    @Test
    @UiThreadTest
    public void testIsStateSaved() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        FragmentController fc = startupFragmentController(mActivityRule.getActivity(), null,
                viewModelStore);
        FragmentManager fm = fc.getSupportFragmentManager();

        Fragment f = new StrictFragment();
        fm.beginTransaction()
                .add(f, "1")
                .commitNow();

        assertFalse("fragment reported state saved while resumed", f.isStateSaved());

        fc.dispatchPause();
        fc.saveAllState();

        assertTrue("fragment reported state not saved after saveAllState", f.isStateSaved());

        fc.dispatchStop();

        assertTrue("fragment reported state not saved after stop", f.isStateSaved());

        viewModelStore.clear();
        fc.dispatchDestroy();

        assertFalse("fragment reported state saved after destroy", f.isStateSaved());
    }

    /*
     * Test that target fragments are in a useful state when we restore them, even if they're
     * on the back stack.
     */

    @Test
    @UiThreadTest
    public void targetFragmentRestoreLifecycleStateBackStack() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        final Fragment target = new TargetFragment();
        fm1.beginTransaction().add(target, "target").commitNow();

        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);

        fm1.beginTransaction()
                .remove(target)
                .add(referrer, "referrer")
                .addToBackStack(null)
                .commit();

        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Simulate an activity restart
        final FragmentController fc2 =
                restartFragmentController(mActivityRule.getActivity(), fc1, viewModelStore);

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);
    }

    @Test
    @UiThreadTest
    public void targetFragmentRestoreLifecycleStateManagerOrder() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        final Fragment target1 = new TargetFragment();
        final Fragment referrer1 = new ReferrerFragment();
        referrer1.setTargetFragment(target1, 0);

        fm1.beginTransaction().add(target1, "target1").add(referrer1, "referrer1").commitNow();

        final Fragment target2 = new TargetFragment();
        final Fragment referrer2 = new ReferrerFragment();
        referrer2.setTargetFragment(target2, 0);

        // Order shouldn't matter.
        fm1.beginTransaction().add(referrer2, "referrer2").add(target2, "target2").commitNow();

        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.dispatchStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Simulate an activity restart
        final FragmentController fc2 =
                restartFragmentController(mActivityRule.getActivity(), fc1, viewModelStore);

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc2, viewModelStore);
    }

    @Test
    @UiThreadTest
    public void targetFragmentClearedWhenSetToNull() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        referrer.setTargetFragment(null, 0);

        assertWithMessage("Target Fragment should cleared after setTargetFragment with null")
                .that(referrer.getTargetFragment())
                .isNull();

        fm.beginTransaction()
                .remove(referrer)
                .commitNow();

        assertWithMessage("Target Fragment should still be cleared after being removed")
                .that(referrer.getTargetFragment())
                .isNull();

        shutdownFragmentController(fc, viewModelStore);
    }

    /**
     * Test the availability of getTargetFragment() when the target Fragment is already
     * attached to a FragmentManager, but the referrer Fragment is not attached.
     */
    @Test
    @UiThreadTest
    public void targetFragmentOnlyTargetAdded() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        // Add just the target Fragment to the FragmentManager
        fm.beginTransaction().add(target, "target").commitNow();

        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction()
                .remove(referrer)
                .commitNow();

        assertWithMessage("Target Fragment should be accessible after being removed")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        shutdownFragmentController(fc, viewModelStore);
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * not retained and the referrer fragment is not retained.
     */
    @Test
    @UiThreadTest
    public void targetFragmentNonRetainedNonRetained() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction()
                .remove(referrer)
                .commitNow();

        assertWithMessage("Target Fragment should be accessible after being removed")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        shutdownFragmentController(fc, viewModelStore);

        assertWithMessage("Target Fragment should be accessible after destruction")
                .that(referrer.getTargetFragment())
                .isSameAs(target);
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * retained and the referrer fragment is not retained.
     */
    @Test
    @UiThreadTest
    public void targetFragmentRetainedNonRetained() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        target.setRetainInstance(true);
        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction()
                .remove(referrer)
                .commitNow();

        assertWithMessage("Target Fragment should be accessible after being removed")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        shutdownFragmentController(fc, viewModelStore);

        assertWithMessage("Target Fragment should be accessible after destruction")
                .that(referrer.getTargetFragment())
                .isSameAs(target);
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * not retained and the referrer fragment is retained.
     */
    @Test
    @UiThreadTest
    public void targetFragmentNonRetainedRetained() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        final Fragment referrer = new ReferrerFragment();
        referrer.setTargetFragment(target, 0);
        referrer.setRetainInstance(true);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        // Save the state
        fc.dispatchPause();
        fc.saveAllState();
        fc.dispatchStop();
        fc.dispatchDestroy();

        assertWithMessage("Target Fragment should be accessible after target Fragment destruction")
                .that(referrer.getTargetFragment())
                .isSameAs(target);
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * retained and the referrer fragment is also retained.
     */
    @Test
    @UiThreadTest
    public void targetFragmentRetainedRetained() {
        ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc =
                startupFragmentController(mActivityRule.getActivity(), null, viewModelStore);

        final FragmentManager fm = fc.getSupportFragmentManager();

        final Fragment target = new TargetFragment();
        target.setRetainInstance(true);
        final Fragment referrer = new ReferrerFragment();
        referrer.setRetainInstance(true);
        referrer.setTargetFragment(target, 0);

        assertWithMessage("Target Fragment should be accessible before being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow();

        assertWithMessage("Target Fragment should be accessible after being added")
                .that(referrer.getTargetFragment())
                .isSameAs(target);

        // Save the state
        fc.dispatchPause();
        fc.saveAllState();
        fc.dispatchStop();
        fc.dispatchDestroy();

        assertWithMessage("Target Fragment should be accessible after FragmentManager destruction")
                .that(referrer.getTargetFragment())
                .isSameAs(target);
    }

    @Test
    public void targetFragmentNoCycles() throws Throwable {
        final Fragment one = new Fragment();
        final Fragment two = new Fragment();
        final Fragment three = new Fragment();

        try {
            one.setTargetFragment(two, 0);
            two.setTargetFragment(three, 0);
            three.setTargetFragment(one, 0);
            assertTrue("creating a fragment target cycle did not throw IllegalArgumentException",
                    false);
        } catch (IllegalArgumentException e) {
            // Success!
        }
    }

    @Test
    public void targetFragmentSetClear() throws Throwable {
        final Fragment one = new Fragment();
        final Fragment two = new Fragment();

        one.setTargetFragment(two, 0);
        one.setTargetFragment(null, 0);
    }

    /**
     * When a fragment has been optimized out, it state should still be saved during
     * save and restore instance state.
     */
    @Test
    @UiThreadTest
    public void saveRemovedFragment() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        SaveStateFragment fragment1 = SaveStateFragment.create(1);
        fm.beginTransaction()
                .add(android.R.id.content, fragment1, "1")
                .addToBackStack(null)
                .commit();
        SaveStateFragment fragment2 = SaveStateFragment.create(2);
        fm.beginTransaction()
                .replace(android.R.id.content, fragment2, "2")
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        fm = fc.getSupportFragmentManager();
        fragment2 = (SaveStateFragment) fm.findFragmentByTag("2");
        assertNotNull(fragment2);
        assertEquals(2, fragment2.getValue());
        fm.popBackStackImmediate();
        fragment1 = (SaveStateFragment) fm.findFragmentByTag("1");
        assertNotNull(fragment1);
        assertEquals(1, fragment1.getValue());
    }

    private void assertAnimationsMatch(FragmentManager fm, int enter, int exit, int popEnter,
            int popExit) {
        FragmentManagerImpl fmImpl = (FragmentManagerImpl) fm;
        BackStackRecord record = fmImpl.mBackStack.get(fmImpl.mBackStack.size() - 1);

        Assert.assertEquals(enter, record.mEnterAnim);
        Assert.assertEquals(exit, record.mExitAnim);
        Assert.assertEquals(popEnter, record.mPopEnterAnim);
        Assert.assertEquals(popExit, record.mPopExitAnim);
    }

    private void executePendingTransactions(final FragmentManager fm) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.executePendingTransactions();
            }
        });
    }

    public static class StateSaveFragment extends StrictFragment {
        private static final String STATE_KEY = "state";

        private String mSavedState;
        private String mUnsavedState;

        public StateSaveFragment() {
        }

        public StateSaveFragment(String savedState, String unsavedState) {
            mSavedState = savedState;
            mUnsavedState = unsavedState;
        }

        public String getSavedState() {
            return mSavedState;
        }

        public String getUnsavedState() {
            return mUnsavedState;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mSavedState = savedInstanceState.getString(STATE_KEY);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(STATE_KEY, mSavedState);
        }
    }

    public static class SimpleFragment extends Fragment {
        private int mLayoutId;
        private static final String LAYOUT_ID = "layoutId";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mLayoutId = savedInstanceState.getInt(LAYOUT_ID, mLayoutId);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(LAYOUT_ID, mLayoutId);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(mLayoutId, container, false);
        }

        public static SimpleFragment create(int layoutId) {
            SimpleFragment fragment = new SimpleFragment();
            fragment.mLayoutId = layoutId;
            return fragment;
        }
    }

    public static class TargetFragment extends Fragment {
        public boolean calledCreate;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            calledCreate = true;
        }
    }

    public static class ReferrerFragment extends Fragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Fragment target = getTargetFragment();
            assertNotNull("target fragment was null during referrer onCreate", target);

            if (!(target instanceof TargetFragment)) {
                throw new IllegalStateException("target fragment was not a TargetFragment");
            }

            assertTrue("target fragment has not yet been created",
                    ((TargetFragment) target).calledCreate);
        }
    }

    public static class SaveStateFragment extends Fragment {
        private static final String VALUE_KEY = "SaveStateFragment.mValue";
        private int mValue;

        public static SaveStateFragment create(int value) {
            SaveStateFragment saveStateFragment = new SaveStateFragment();
            saveStateFragment.mValue = value;
            return saveStateFragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(VALUE_KEY, mValue);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mValue = savedInstanceState.getInt(VALUE_KEY, mValue);
            }
        }

        public int getValue() {
            return mValue;
        }
    }
}
