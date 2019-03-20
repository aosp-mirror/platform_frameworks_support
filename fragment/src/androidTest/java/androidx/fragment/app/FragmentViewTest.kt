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

import org.junit.Assert.fail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.ContentView
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FragmentViewTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Test that adding a fragment adds the Views in the proper order. Popping the back stack
    // should remove the correct Views.
    @Test
    fun addFragments() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        // Add another on top
        val fragment2 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment2).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1, fragment2)

        // Now add two in one transaction:
        val fragment3 = StrictViewFragment()
        val fragment4 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment3)
            .add(R.id.fragmentContainer, fragment4)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3, fragment4)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1, fragment2)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(container.childCount).isEqualTo(1)
        FragmentTestUtil.assertChildren(container, fragment1)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)
    }

    // Add fragments to multiple containers in the same transaction. Make sure that
    // they pop correctly, too.
    @Test
    fun addTwoContainers() {
        FragmentTestUtil.setContentView(activityRule, R.layout.double_container)
        val container1 =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer1) as ViewGroup
        val container2 =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer2) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer1, fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container1, fragment1)

        val fragment2 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer2, fragment2).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container2, fragment2)

        val fragment3 = StrictViewFragment()
        val fragment4 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment3)
            .add(R.id.fragmentContainer2, fragment4)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container1, fragment1, fragment3)
        FragmentTestUtil.assertChildren(container2, fragment2, fragment4)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container1, fragment1)
        FragmentTestUtil.assertChildren(container2, fragment2)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container1, fragment1)
        FragmentTestUtil.assertChildren(container2)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(container1.childCount).isEqualTo(0)
    }

    // When you add a fragment that's has already been added, it should throw.
    @Test
    fun doubleAdd() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        instrumentation.runOnMainSync {
            try {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment1)
                    .addToBackStack(null)
                    .commit()
                fm.executePendingTransactions()
                fail("Adding a fragment that is already added should be an error")
            } catch (e: IllegalStateException) {
                assertThat(e)
                    .hasMessageThat().contains("Fragment already added: $fragment1")
            }
        }
    }

    // Make sure that removed fragments remove the right Views. Popping the back stack should
    // add the Views back properly
    @Test
    fun removeFragments() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()
        val fragment3 = StrictViewFragment()
        val fragment4 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .add(R.id.fragmentContainer, fragment3, "3")
            .add(R.id.fragmentContainer, fragment4, "4")
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3, fragment4)

        // Remove a view
        fm.beginTransaction().remove(fragment4).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        assertThat(container.childCount).isEqualTo(3)
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3)

        // remove another one
        fm.beginTransaction().remove(fragment2).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1, fragment3)

        // Now remove the remaining:
        fm.beginTransaction()
            .remove(fragment3)
            .remove(fragment1)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        val replacement1 = fm.findFragmentByTag("1")
        val replacement3 = fm.findFragmentByTag("3")
        FragmentTestUtil.assertChildren(container, replacement1, replacement3)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        val replacement2 = fm.findFragmentByTag("2")
        FragmentTestUtil.assertChildren(container, replacement1, replacement3, replacement2)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        val replacement4 = fm.findFragmentByTag("4")
        FragmentTestUtil.assertChildren(
            container, replacement1, replacement3, replacement2,
            replacement4
        )
    }

    // Removing a hidden fragment should remove the View and popping should bring it back hidden
    @Test
    fun removeHiddenView() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "1").hide(fragment1).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)
        assertThat(fragment1.isHidden).isTrue()

        fm.beginTransaction().remove(fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        val replacement1 = fm.findFragmentByTag("1")!!
        FragmentTestUtil.assertChildren(container, replacement1)
        assertThat(replacement1.isHidden).isTrue()
        assertThat(replacement1.requireView().visibility).isEqualTo(View.GONE)
    }

    // Removing a detached fragment should do nothing to the View and popping should bring
    // the Fragment back detached
    @Test
    fun removeDetatchedView() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .detach(fragment1)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)
        assertThat(fragment1.isDetached).isTrue()

        fm.beginTransaction().remove(fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        val replacement1 = fm.findFragmentByTag("1")!!
        FragmentTestUtil.assertChildren(container)
        assertThat(replacement1.isDetached).isTrue()
    }

    // Unlike adding the same fragment twice, you should be able to add and then remove and then
    // add the same fragment in one transaction.
    @Test
    fun addRemoveAdd() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .remove(fragment)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)
    }

    // Removing a fragment that isn't in should not throw
    @Test
    fun removeNotThere() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().remove(fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Hide a fragment and its View should be GONE. Then pop it and the View should be VISIBLE
    @Test
    fun hideFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.beginTransaction().hide(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
    }

    // Hiding a hidden fragment should not throw
    @Test
    fun doubleHide() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .hide(fragment)
            .hide(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Hiding a non-existing fragment should not throw
    @Test
    fun hideUnAdded() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .hide(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Show a hidden fragment and its View should be VISIBLE. Then pop it and the View should be
    // GONE.
    @Test
    fun showFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)

        fm.beginTransaction().show(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
    }

    // Showing a shown fragment should not throw
    @Test
    fun showShown() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .show(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Showing a non-existing fragment should not throw
    @Test
    fun showUnAdded() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .show(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Detaching a fragment should remove the View from the hierarchy. Then popping it should
    // bring it back VISIBLE
    @Test
    fun detachFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.beginTransaction().detach(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isDetached).isTrue()

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
    }

    // Detaching a hidden fragment should remove the View from the hierarchy. Then popping it should
    // bring it back hidden
    @Test
    fun detachHiddenFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)

        fm.beginTransaction().detach(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.isDetached).isTrue()

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
    }

    // Detaching a detached fragment should not throw
    @Test
    fun detachDetatched() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .detach(fragment)
            .detach(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Detaching a non-existing fragment should not throw
    @Test
    fun detachUnAdded() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .detach(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Attaching a fragment should add the View back into the hierarchy. Then popping it should
    // remove it again
    @Test
    fun attachFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).detach(fragment).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isDetached).isTrue()

        fm.beginTransaction().attach(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isDetached).isTrue()
    }

    // Attaching a hidden fragment should add the View as GONE the hierarchy. Then popping it should
    // remove it again.
    @Test
    fun attachHiddenFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .hide(fragment)
            .detach(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isDetached).isTrue()
        assertThat(fragment.isHidden).isTrue()

        fm.beginTransaction().attach(fragment).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.isHidden).isTrue()
        assertThat(fragment.isDetached).isFalse()
        assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
        assertThat(fragment.isDetached).isTrue()
        assertThat(fragment.isHidden).isTrue()
    }

    // Attaching an attached fragment should not throw
    @Test
    fun attachAttached() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .attach(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Attaching a non-existing fragment should not throw
    @Test
    fun attachUnAdded() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .attach(fragment)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
    }

    // Simple replace of one fragment in a container. Popping should replace it back again
    @Test
    fun replaceOne() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "1").commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment1)

        val fragment2 = StrictViewFragment()
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment2)
        assertThat(fragment2.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        val replacement1 = fm.findFragmentByTag("1")!!
        assertThat(replacement1).isNotNull()
        FragmentTestUtil.assertChildren(container, replacement1)
        assertThat(replacement1.isHidden).isFalse()
        assertThat(replacement1.isAdded).isTrue()
        assertThat(replacement1.isDetached).isFalse()
        assertThat(replacement1.requireView().visibility).isEqualTo(View.VISIBLE)
    }

    // Replace of multiple fragments in a container. Popping should replace it back again
    @Test
    fun replaceTwo() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .hide(fragment2)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment1, fragment2)

        val fragment3 = StrictViewFragment()
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment3)
        assertThat(fragment3.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        val replacement1 = fm.findFragmentByTag("1")!!
        val replacement2 = fm.findFragmentByTag("2")!!
        assertThat(replacement1).isNotNull()
        assertThat(replacement2).isNotNull()
        FragmentTestUtil.assertChildren(container, replacement1, replacement2)
        assertThat(replacement1.isHidden).isFalse()
        assertThat(replacement1.isAdded).isTrue()
        assertThat(replacement1.isDetached).isFalse()
        assertThat(replacement1.requireView().visibility).isEqualTo(View.VISIBLE)

        // fragment2 was hidden, so it should be returned hidden
        assertThat(replacement2.isHidden).isTrue()
        assertThat(replacement2.isAdded).isTrue()
        assertThat(replacement2.isDetached).isFalse()
        assertThat(replacement2.requireView().visibility).isEqualTo(View.GONE)
    }

    // Replace of empty container. Should act as add and popping should just remove the fragment
    @Test
    fun replaceZero() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment)
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container)
    }

    // Replace a fragment that exists with itself
    @Test
    fun replaceExisting() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment1, fragment2)

        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment1)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        val replacement1 = fm.findFragmentByTag("1")
        val replacement2 = fm.findFragmentByTag("2")

        assertThat(replacement1).isSameAs(fragment1)
        FragmentTestUtil.assertChildren(container, replacement1, replacement2)
    }

    // Have two replace operations in the same transaction to ensure that they
    // don't interfere with each other
    @Test
    fun replaceReplace() {
        FragmentTestUtil.setContentView(activityRule, R.layout.double_container)
        val container1 =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer1) as ViewGroup
        val container2 =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer2) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()
        val fragment3 = StrictViewFragment()
        val fragment4 = StrictViewFragment()
        val fragment5 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment1)
            .add(R.id.fragmentContainer2, fragment2)
            .replace(R.id.fragmentContainer1, fragment3)
            .replace(R.id.fragmentContainer2, fragment4)
            .replace(R.id.fragmentContainer1, fragment5)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        assertChildren(container1, fragment5)
        assertChildren(container2, fragment4)

        fm.popBackStack()
        FragmentTestUtil.executePendingTransactions(activityRule)

        assertChildren(container1)
        assertChildren(container2)
    }

    // Test to prevent regressions in FragmentManager fragment replace method. See b/24693644
    @Test
    fun testReplaceFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager
        val fragmentA = StrictViewFragment(R.layout.text_a)

        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragmentA)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        assertThat(findViewById(R.id.textA)).isNotNull()
        assertThat(findViewById(R.id.textB)).isNull()
        assertThat(findViewById(R.id.textC)).isNull()

        val fragmentB = StrictViewFragment(R.layout.text_b)
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragmentB)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(findViewById(R.id.textA)).isNotNull()
        assertThat(findViewById(R.id.textB)).isNotNull()
        assertThat(findViewById(R.id.textC)).isNull()

        val fragmentC = StrictViewFragment(R.layout.text_c)
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragmentC)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(findViewById(R.id.textA)).isNull()
        assertThat(findViewById(R.id.textB)).isNull()
        assertThat(findViewById(R.id.textC)).isNotNull()
    }

    // Test that adding a fragment with invisible or gone views does not end up with the view
    // being visible
    @Test
    fun addInvisibleAndGoneFragments() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = InvisibleFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        assertThat(fragment1.requireView().visibility).isEqualTo(View.INVISIBLE)

        val fragment2 = InvisibleFragment()
        fragment2.visibility = View.GONE
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment2)

        assertThat(fragment2.requireView().visibility).isEqualTo(View.GONE)
    }

    // Test to ensure that popping and adding a fragment properly track the fragments added
    // and removed.
    @Test
    fun popAdd() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = StrictViewFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        val fragment2 = StrictViewFragment()
        val fragment3 = StrictViewFragment()
        instrumentation.runOnMainSync {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
            fm.executePendingTransactions()
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment3)
    }

    // Ensure that ordered transactions are executed individually rather than together.
    // This forces references from one fragment to another that should be executed earlier
    // to work.
    @Test
    fun orderedOperationsTogether() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment(R.layout.scene1)
        val fragment2 = StrictViewFragment(R.layout.fragment_a)

        activityRule.runOnUiThread {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .setReorderingAllowed(false)
                .addToBackStack(null)
                .commit()
            fm.beginTransaction()
                .add(R.id.squareContainer, fragment2)
                .setReorderingAllowed(false)
                .addToBackStack(null)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment1)
        assertThat(findViewById(R.id.textA)).isNotNull()
    }

    // Ensure that there is no problem if the child fragment manager is used before
    // the View has been added.
    @Test
    fun childFragmentManager() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ParentFragment()

        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .commit()

        FragmentTestUtil.executePendingTransactions(activityRule)

        FragmentTestUtil.assertChildren(container, fragment1)
        val innerContainer =
            fragment1.requireView().findViewById<ViewGroup>(R.id.fragmentContainer1)

        val fragment2 = fragment1.childFragmentManager.findFragmentByTag("inner")
        FragmentTestUtil.assertChildren(innerContainer, fragment2)
    }

    // Popping the backstack with ordered fragments should execute the operations together.
    // When a non-backstack fragment will be raised, it should not be destroyed.
    @Test
    fun popToNonBackStackFragment() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = SimpleViewFragment()

        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .commit()

        FragmentTestUtil.executePendingTransactions(activityRule)

        val fragment2 = SimpleViewFragment()

        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack("two")
            .commit()

        FragmentTestUtil.executePendingTransactions(activityRule)

        val fragment3 = SimpleViewFragment()

        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack("three")
            .commit()

        FragmentTestUtil.executePendingTransactions(activityRule)

        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
        assertThat(fragment2.onCreateViewCount).isEqualTo(1)
        assertThat(fragment3.onCreateViewCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(
            activityRule, "two",
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val container =
            activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup

        FragmentTestUtil.assertChildren(container, fragment1)

        assertThat(fragment1.onCreateViewCount).isEqualTo(2)
        assertThat(fragment2.onCreateViewCount).isEqualTo(1)
        assertThat(fragment3.onCreateViewCount).isEqualTo(1)
    }

    private fun findViewById(viewId: Int): View? {
        return activityRule.activity.findViewById(viewId)
    }

    private fun assertChildren(container: ViewGroup, vararg fragments: Fragment) {
        val numFragments = fragments.size
        assertWithMessage("There aren't the correct number of fragment Views in its container")
            .that(container.childCount)
            .isEqualTo(numFragments)
        for (i in 0 until numFragments) {
            assertWithMessage("Wrong Fragment View order for [$i]")
                .that(fragments[i].view)
                .isEqualTo(container.getChildAt(i))
        }
    }

    class InvisibleFragment : StrictViewFragment() {
        var visibility = View.INVISIBLE

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.visibility = visibility
            super.onViewCreated(view, savedInstanceState)
        }
    }

    class ParentFragment : StrictViewFragment(R.layout.double_container) {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val fragment2 = StrictViewFragment(R.layout.fragment_a)

            childFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer1, fragment2, "inner")
                .addToBackStack(null)
                .commit()
            childFragmentManager.executePendingTransactions()
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }

    @ContentView(R.layout.fragment_a)
    class SimpleViewFragment : Fragment() {
        var onCreateViewCount: Int = 0

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            onCreateViewCount++
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }
}
