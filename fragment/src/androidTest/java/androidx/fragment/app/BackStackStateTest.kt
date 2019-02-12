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

package androidx.fragment.app

import android.os.Parcel
import androidx.fragment.app.FragmentTestUtil.shutdownFragmentController
import androidx.fragment.app.FragmentTestUtil.startupFragmentController
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BackStackStateTest {

    @get:Rule
    var activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager
            as FragmentManagerImpl

    @Test
    fun testParcel() {
        val fragment = StrictFragment()
        val backStackRecord = BackStackRecord(fragmentManager).apply {
            add(fragment, "tag")
            addToBackStack("back_stack")
            setReorderingAllowed(true)
        }
        val backStackState = BackStackState(backStackRecord)
        val parcel = Parcel.obtain()
        backStackState.writeToParcel(parcel, 0)
        // Reset for reading
        parcel.setDataPosition(0)
        val restoredBackStackState = BackStackState(parcel)
        assertThat(restoredBackStackState.mOps).asList()
            .containsExactlyElementsIn(backStackState.mOps.asList())
        assertThat(restoredBackStackState.mFragmentWhos)
            .containsExactlyElementsIn(backStackState.mFragmentWhos)
        assertThat(restoredBackStackState.mReorderingAllowed)
            .isEqualTo(backStackState.mReorderingAllowed)
    }

    @Test
    @UiThreadTest
    fun testFragmentManagerNullOnDetach() {
        val viewModelStore = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore, activityRule)
        val fm1 = fc1.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()

        fm1.beginTransaction().detach(fragment1).commitNow()

        assertThat(fragment1.fragmentManager).isNull()

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore)
    }

    @Test
    @UiThreadTest
    fun testAttachOnToFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore1, activityRule)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = startupFragmentController(null, viewModelStore2, activityRule)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()
        try {
            fm2.beginTransaction().attach(fragment1).commitNow()
            fail("Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException")
        } catch (e: IllegalStateException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore1)
        shutdownFragmentController(fc2, viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testReplaceOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore1, activityRule)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = startupFragmentController(null, viewModelStore2, activityRule)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()
        try {
            fm2.beginTransaction().replace(android.R.id.content, fragment1).commitNow()
            fail("Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException")
        } catch (e: IllegalStateException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore1)
        shutdownFragmentController(fc2, viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testHideOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore1, activityRule)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = startupFragmentController(null, viewModelStore2, activityRule)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()
        try {
            fm2.beginTransaction().hide(fragment1).commitNow()
            fail("Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException")
        } catch (e: IllegalStateException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore1)
        shutdownFragmentController(fc2, viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testShowOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore1, activityRule)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = startupFragmentController(null, viewModelStore2, activityRule)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()
        try {
            fm2.beginTransaction().show(fragment1).commitNow()
            fail("Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException")
        } catch (e: IllegalStateException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore1)
        shutdownFragmentController(fc2, viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testSetPrimaryNavigationFragmentOnFragmentWithAManager() {
        val viewModelStore1 = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore1, activityRule)
        val fm1 = fc1.supportFragmentManager

        val viewModelStore2 = ViewModelStore()
        val fc2 = startupFragmentController(null, viewModelStore2, activityRule)
        val fm2 = fc2.supportFragmentManager

        val fragment1 = Fragment()
        fragment1.retainInstance = true

        fm1.beginTransaction().attach(fragment1).commitNow()
        try {
            fm2.beginTransaction().setPrimaryNavigationFragment(fragment1).commitNow()
            fail("Fragment associated with another" +
                    " FragmentManager should throw IllegalStateException")
        } catch (e: IllegalStateException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore1)
        shutdownFragmentController(fc2, viewModelStore2)
    }

    @Test
    @UiThreadTest
    fun testDetachFragmentNotAssociatedWithManager() {
        val viewModelStore = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore, activityRule)
        val fm1 = fc1.supportFragmentManager

        // Add the initial state
        val fragment1 = StrictFragment()
        fragment1.retainInstance = true

        try {
            fm1.beginTransaction().detach(fragment1).commitNow()
            fail("Unassociated fragment should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore)
    }

    @Test
    @UiThreadTest
    fun testRemoveFragmentNotAssociatedWithManager() {
        val viewModelStore = ViewModelStore()
        val fc1 = startupFragmentController(null, viewModelStore, activityRule)
        val fm1 = fc1.supportFragmentManager

        // Add the initial state
        val fragment1 = StrictFragment()
        fragment1.retainInstance = true

        try {
            fm1.beginTransaction().remove(fragment1).commitNow()
            fail("Unassociated fragment should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }

        // Bring the state back down to destroyed before we finish the test
        shutdownFragmentController(fc1, viewModelStore)
    }
}
