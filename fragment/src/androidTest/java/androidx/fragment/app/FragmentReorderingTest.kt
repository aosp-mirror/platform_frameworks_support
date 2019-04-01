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

import android.app.Instrumentation
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FragmentReorderingTest {
    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var container: ViewGroup
    private lateinit var fm: FragmentManager
    private lateinit var instrumentation: Instrumentation

    @Before
    fun setup() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
        container = activityRule.activity.findViewById<View>(R.id.fragmentContainer) as ViewGroup
        fm = activityRule.activity.supportFragmentManager
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    // Test that when you add and replace a fragment that only the replace's add
    // actually creates a View.
    @Test
    fun addReplace() {
        val fragment1 = CountCallsFragment()
        val fragment2 = StrictViewFragment()
        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
        FragmentTestUtil.assertChildren(container, fragment2)

        instrumentation.runOnMainSync {
            fm.popBackStack()
            fm.popBackStack()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container)
    }

    // Test that it is possible to merge a transaction that starts with pop and adds
    // the same view back again.
    @Test
    fun startWithPop() {
        // Start with a single fragment on the back stack
        val fragment1 = CountCallsFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        // Now pop and add
        instrumentation.runOnMainSync {
            fm.popBackStack()
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment1)
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(activityRule)
        FragmentTestUtil.assertChildren(container)
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
    }

    // Popping the back stack in the middle of other operations doesn't fool it.
    @Test
    fun middlePop() {
        val fragment1 = CountCallsFragment()
        val fragment2 = CountCallsFragment()
        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.popBackStack()
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment2)
        assertThat(fragment1.onAttachCount).isEqualTo(0)
        assertThat(fragment2.onCreateViewCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(activityRule)
        FragmentTestUtil.assertChildren(container)
        assertThat(fragment2.onDetachCount).isEqualTo(1)
    }

    // ensure that removing a view after adding it is optimized into no
    // View being created. Hide still gets notified.
    @Test
    fun removeRedundantRemove() {
        val fragment1 = CountCallsFragment()
        var id = -1
        instrumentation.runOnMainSync {
            id = fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .remove(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container)
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onDetachCount).isEqualTo(0)
        assertThat(fragment1.onAttachCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(
            activityRule,
            id,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        FragmentTestUtil.assertChildren(container)
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.onShowCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(1)
        assertThat(fragment1.onAttachCount).isEqualTo(1)
    }

    // Ensure that removing and adding the same view results in no operation
    @Test
    fun removeRedundantAdd() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .remove(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        FragmentTestUtil.assertChildren(container, fragment1)
        // should be optimized out
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)

        fm.popBackStack(id, 0)
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)
        // optimize out going back, too
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
    }

    // detaching, then attaching results in on change. Hide still functions
    @Test
    fun removeRedundantAttach() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        FragmentTestUtil.assertChildren(container, fragment1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .detach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .attach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        FragmentTestUtil.assertChildren(container, fragment1)
        // can optimize out the detach/attach
        assertThat(fragment1.onDestroyViewCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(0)

        fm.popBackStack(id, 0)
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        // optimized out again, but not the show
        assertThat(fragment1.onDestroyViewCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.onShowCount).isEqualTo(1)
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(0)
    }

    // attaching, then detaching shouldn't result in a View being created
    @Test
    fun removeRedundantDetach() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .detach(fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)

        // the add detach is not fully optimized out
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(0)
        assertThat(fragment1.isDetached).isTrue()
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
        FragmentTestUtil.assertChildren(container)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .attach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .detach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        FragmentTestUtil.assertChildren(container)
        // can optimize out the attach/detach, and the hide call
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.isHidden).isTrue()
        assertThat(fragment1.onShowCount).isEqualTo(0)

        fm.popBackStack(id, 0)
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container)

        // we can optimize out the attach/detach on the way back
        assertThat(fragment1.onAttachCount).isEqualTo(1)
        assertThat(fragment1.onDetachCount).isEqualTo(0)
        assertThat(fragment1.onShowCount).isEqualTo(1)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        assertThat(fragment1.isHidden).isFalse()
    }

    // show, then hide should optimize out
    @Test
    fun removeRedundantHide() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .hide(fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
        FragmentTestUtil.assertChildren(container, fragment1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .show(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .remove(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        FragmentTestUtil.assertChildren(container, fragment1)
        // optimize out hide/show
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(activityRule, id, 0)
        FragmentTestUtil.assertChildren(container, fragment1)

        // still optimized out
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .show(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        // The show/hide can be optimized out and nothing should change.
        FragmentTestUtil.assertChildren(container, fragment1)
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(activityRule, id, 0)
        FragmentTestUtil.assertChildren(container, fragment1)

        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .show(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .detach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .attach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        // the detach/attach should not affect the show/hide, so show/hide should cancel each other
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)

        FragmentTestUtil.popBackStackImmediate(activityRule, id, 0)
        FragmentTestUtil.assertChildren(container, fragment1)

        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(1)
    }

    // hiding and showing the same view should optimize out
    @Test
    fun removeRedundantShow() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(0)
        FragmentTestUtil.assertChildren(container, fragment1)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .detach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .attach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .show(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.executePendingTransactions()
        }

        FragmentTestUtil.assertChildren(container, fragment1)
        // can optimize out the show/hide
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(0)

        FragmentTestUtil.popBackStackImmediate(
            activityRule, id,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        assertThat(fragment1.onShowCount).isEqualTo(0)
        assertThat(fragment1.onHideCount).isEqualTo(0)
    }

    // The View order shouldn't be messed up by reordering -- a view that
    // is optimized to not remove/add should be in its correct position after
    // the transaction completes.
    @Test
    fun viewOrder() {
        val fragment1 = CountCallsFragment()
        val id = fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule)
        FragmentTestUtil.assertChildren(container, fragment1)

        val fragment2 = CountCallsFragment()

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()

            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment2, fragment1)

        FragmentTestUtil.popBackStackImmediate(activityRule, id, 0)
        FragmentTestUtil.assertChildren(container, fragment1)
    }

    // Popping an added transaction results in no operation
    @Test
    fun addPopBackStack() {
        val fragment1 = CountCallsFragment()
        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.popBackStack()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container)

        // Was never instantiated because it was popped before anything could happen
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
    }

    // A non-back-stack transaction doesn't interfere with back stack add/pop
    // optimization.
    @Test
    fun popNonBackStack() {
        val fragment1 = CountCallsFragment()
        val fragment2 = CountCallsFragment()
        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .setReorderingAllowed(true)
                .commit()
            fm.popBackStack()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment2)

        // It should be optimized with the replace, so no View creation
        assertThat(fragment1.onCreateViewCount).isEqualTo(0)
    }

    // When reordering is disabled, the transaction prior to the disabled reordering
    // transaction should all be run prior to running the ordered transaction.
    @Test
    fun noReordering() {
        val fragment1 = CountCallsFragment()
        val fragment2 = CountCallsFragment()
        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
            fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(false)
                .commit()
            fm.executePendingTransactions()
        }
        FragmentTestUtil.assertChildren(container, fragment2)

        // No reordering, so fragment1 should have created its View
        assertThat(fragment1.onCreateViewCount).isEqualTo(1)
    }

    // Test that a fragment view that is created with focus has focus after the transaction
    // completes.
    @UiThreadTest
    @Test
    fun focusedView() {
        activityRule.activity.setContentView(R.layout.double_container)
        container = activityRule.activity.findViewById<View>(R.id.fragmentContainer1) as ViewGroup
        val firstEditText = EditText(container.context)
        container.addView(firstEditText)
        firstEditText.requestFocus()

        assertThat(firstEditText.isFocused).isTrue()
        val fragment1 = CountCallsFragment()
        val fragment2 = CountCallsFragment(R.layout.with_edit_text)
        fm.beginTransaction()
            .add(R.id.fragmentContainer2, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        fm.beginTransaction()
            .replace(R.id.fragmentContainer2, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        fm.executePendingTransactions()
        val editText = fragment2.requireView().findViewById<View>(R.id.editText)
        assertThat(editText.isFocused).isTrue()
        assertThat(firstEditText.isFocused).isFalse()
    }
}
