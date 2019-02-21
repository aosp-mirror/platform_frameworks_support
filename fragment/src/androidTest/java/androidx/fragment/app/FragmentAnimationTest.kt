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
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.annotation.AnimRes
import androidx.core.view.ViewCompat
import androidx.fragment.app.CtsMockitoUtils.within
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentAnimationTest {

    @get:Rule
    var mActivityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private var mInstrumentation: Instrumentation? = null

    @Before
    fun setupContainer() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation()
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    fun addAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    fun removeAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertExitPopEnter(fragment)
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    @Test
    fun showAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertEnterPopExit(fragment)
    }

    // Ensure that hiding and popping a Fragment uses the exit and popEnter animators
    @Test
    fun hideAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .hide(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertExitPopEnter(fragment)
    }

    // Ensure that attaching and popping a Fragment uses the enter and popExit animators
    @Test
    fun attachAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).detach(fragment).commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .attach(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertEnterPopExit(fragment)
    }

    // Ensure that detaching and popping a Fragment uses the exit and popEnter animators
    @Test
    fun detachAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .detach(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertExitPopEnter(fragment)
    }

    // Replace should exit the existing fragments and enter the added fragment, then
    // popping should popExit the removed fragment and popEnter the added fragments
    @Test
    fun replaceAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = AnimatorFragment()
        val fragment2 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val fragment3 = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertFragmentAnimation(fragment1, 1, false, EXIT)
        assertFragmentAnimation(fragment2, 1, false, EXIT)
        assertFragmentAnimation(fragment3, 1, true, ENTER)

        fm.popBackStack()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT)
        val replacement1 = fm.findFragmentByTag("1") as AnimatorFragment?
        val replacement2 = fm.findFragmentByTag("1") as AnimatorFragment?
        val expectedAnimations = if (replacement1 === fragment1) 2 else 1
        assertFragmentAnimation(replacement1!!, expectedAnimations, true, POP_ENTER)
        assertFragmentAnimation(replacement2!!, expectedAnimations, true, POP_ENTER)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedAddAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fragment.postponeEnterTransition()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertPostponed(fragment, 0)
        fragment.startPostponedEnterTransition()

        FragmentTestUtil.waitForExecution(mActivityRule)
        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedRemoveAnimators() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertExitPostponedPopEnter(fragment)
    }

    // Ensure that adding and popping a Fragment is postponed in both directions
    // when the fragments have been marked for postponing.
    @Test
    fun postponedAddRemove() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        FragmentTestUtil.waitForExecution(mActivityRule)

        assertPostponed(fragment2, 0)
        assertThat(fragment1.view).isNotNull()
        assertThat(fragment1.view!!.visibility.toLong()).isEqualTo(View.VISIBLE.toLong())
        assertThat(fragment1.view!!.alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.view!!)).isTrue()

        fragment2.startPostponedEnterTransition()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertExitPostponedPopEnter(fragment1)
    }

    // Popping a postponed transaction should result in no animators
    @Test
    fun popPostponed() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)
        assertThat(fragment1.numAnimators.toLong()).isEqualTo(0)

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        FragmentTestUtil.waitForExecution(mActivityRule)

        assertPostponed(fragment2, 0)

        // Now pop the postponed transaction
        FragmentTestUtil.popBackStackImmediate(mActivityRule)

        assertThat(fragment1.view).isNotNull()
        assertThat(fragment1.view!!.visibility.toLong()).isEqualTo(View.VISIBLE.toLong())
        assertThat(fragment1.view!!.alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.view!!)).isTrue()
        assertThat(fragment1.isAdded).isTrue()

        assertThat(fragment2.view).isNull()
        assertThat(fragment2.isAdded).isFalse()

        assertThat(fragment1.numAnimators.toLong()).isEqualTo(0)
        assertThat(fragment2.numAnimators.toLong()).isEqualTo(0)
        assertThat(fragment1.animation).isNull()
        assertThat(fragment2.animation).isNull()
    }

    // Make sure that if the state was saved while a Fragment was animating that its
    // state is proper after restoring.
    @Test
    fun saveWhileAnimatingAway() {
        waitForAnimationReady()
        val fc1 = FragmentTestUtil.createController(mActivityRule)
        FragmentTestUtil.resume(mActivityRule, fc1, null)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = StrictViewFragment()
        fragment1.setLayoutId(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val fragment2 = StrictViewFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        mInstrumentation!!.runOnMainSync { fm1.executePendingTransactions() }
        FragmentTestUtil.waitForExecution(mActivityRule)

        fm1.popBackStack()

        mInstrumentation!!.runOnMainSync { fm1.executePendingTransactions() }
        FragmentTestUtil.waitForExecution(mActivityRule)
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        // still exists because it is animating
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)

        val state = FragmentTestUtil.destroy(mActivityRule, fc1)

        val fc2 = FragmentTestUtil.createController(mActivityRule)
        FragmentTestUtil.resume(mActivityRule, fc2, state)

        val fm2 = fc2.supportFragmentManager
        val fragment2restored = fm2.findFragmentByTag("2")
        assertThat(fragment2restored).isNull()

        val fragment1restored = fm2.findFragmentByTag("1")
        assertThat(fragment1restored).isNotNull()
        assertThat(fragment1restored!!.view).isNotNull()
    }

    // When an animation is running on a Fragment's View, the view shouldn't be
    // prevented from being removed. There's no way to directly test this, so we have to
    // test to see if the animation is still running.
    @Test
    fun clearAnimations() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val fragmentView = fragment1.requireView()

        val xAnimation = TranslateAnimation(0f, 1000f, 0f, 0f)
        mActivityRule.runOnUiThread { fragmentView.startAnimation(xAnimation) }

        FragmentTestUtil.waitForExecution(mActivityRule)
        FragmentTestUtil.popBackStackImmediate(mActivityRule)
        mActivityRule.runOnUiThread { assertThat(fragmentView.animation).isNull() }
    }

    // When a view is animated out, is parent should be null after the animation completes
    @Test
    fun parentNullAfterAnimation() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        val fragment1 = AnimationListenerFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val fragment2 = AnimationListenerFragment()

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        FragmentTestUtil.waitForExecution(mActivityRule)

        assertThat(fragment1.exitLatch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment2.enterLatch.await(1, TimeUnit.SECONDS)).isTrue()

        mActivityRule.runOnUiThread {
            assertThat(fragment1.view).isNotNull()
            assertThat(fragment2.view).isNotNull()
            assertThat(fragment1.view!!.parent).isNull()
        }

        // Now pop the transaction
        FragmentTestUtil.popBackStackImmediate(mActivityRule)

        assertThat(fragment2.exitLatch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment1.enterLatch.await(1, TimeUnit.SECONDS)).isTrue()

        mActivityRule.runOnUiThread { assertThat(fragment2.view!!.parent).isNull() }
    }

    @Test
    fun animationListenersAreCalled() {
        waitForAnimationReady()
        val fm = mActivityRule.activity.supportFragmentManager

        // Add first fragment
        val fragment1 = AnimationListenerFragment()
        fragment1.mForceRunOnHwLayer = false
        fragment1.mRepeat = true
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        // Replace first fragment with second fragment with a fade in/out animation
        val fragment2 = AnimationListenerFragment()
        fragment2.mForceRunOnHwLayer = true
        fragment2.mRepeat = false
        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(mActivityRule)

        // Wait for animation to finish
        assertThat(fragment1.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment2.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

        // Check if all animation listener callbacks have been called
        mActivityRule.runOnUiThread {
            assertThat(fragment1.mExitStartCount.toLong()).isEqualTo(1)
            assertThat(fragment1.mExitRepeatCount.toLong()).isEqualTo(1)
            assertThat(fragment1.mExitEndCount.toLong()).isEqualTo(1)
            assertThat(fragment2.mEnterStartCount.toLong()).isEqualTo(1)
            assertThat(fragment2.mEnterRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mEnterEndCount.toLong()).isEqualTo(1)

            // fragment1 exited, so its enter animation should not have been called
            assertThat(fragment1.mEnterStartCount.toLong()).isEqualTo(0)
            assertThat(fragment1.mEnterRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment1.mEnterEndCount.toLong()).isEqualTo(0)
            // fragment2 entered, so its exit animation should not have been called
            assertThat(fragment2.mExitStartCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mExitRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mExitEndCount.toLong()).isEqualTo(0)
        }
        fragment1.resetCounts()
        fragment2.resetCounts()

        // Now pop the transaction
        FragmentTestUtil.popBackStackImmediate(mActivityRule)

        assertThat(fragment2.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment1.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

        mActivityRule.runOnUiThread {
            assertThat(fragment2.mExitStartCount.toLong()).isEqualTo(1)
            assertThat(fragment2.mExitRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mExitEndCount.toLong()).isEqualTo(1)
            assertThat(fragment1.mEnterStartCount.toLong()).isEqualTo(1)
            assertThat(fragment1.mEnterRepeatCount.toLong()).isEqualTo(1)
            assertThat(fragment1.mEnterEndCount.toLong()).isEqualTo(1)

            // fragment1 entered, so its exit animation should not have been called
            assertThat(fragment1.mExitStartCount.toLong()).isEqualTo(0)
            assertThat(fragment1.mExitRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment1.mExitEndCount.toLong()).isEqualTo(0)
            // fragment2 exited, so its enter animation should not have been called
            assertThat(fragment2.mEnterStartCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mEnterRepeatCount.toLong()).isEqualTo(0)
            assertThat(fragment2.mEnterEndCount.toLong()).isEqualTo(0)
        }
    }

    private fun assertEnterPopExit(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, true, ENTER)

        val fm = mActivityRule.activity.supportFragmentManager
        fm.popBackStack()
        FragmentTestUtil.waitForExecution(mActivityRule)

        assertFragmentAnimation(fragment, 2, false, POP_EXIT)
    }

    private fun assertExitPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        val fm = mActivityRule.activity.supportFragmentManager
        fm.popBackStack()
        FragmentTestUtil.waitForExecution(mActivityRule)

        val replacement = fm.findFragmentByTag("1") as AnimatorFragment?

        val isSameFragment = replacement === fragment
        val expectedAnimators = if (isSameFragment) 2 else 1
        assertFragmentAnimation(replacement!!, expectedAnimators, true, POP_ENTER)
    }

    private fun assertExitPostponedPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        fragment.postponeEnterTransition()
        FragmentTestUtil.popBackStackImmediate(mActivityRule)

        assertPostponed(fragment, 1)

        fragment.startPostponedEnterTransition()
        FragmentTestUtil.waitForExecution(mActivityRule)
        assertFragmentAnimation(fragment, 2, true, POP_ENTER)
    }

    @Throws(InterruptedException::class)
    private fun assertFragmentAnimation(
        fragment: AnimatorFragment,
        numAnimators: Int,
        isEnter: Boolean,
        animatorResourceId: Int
    ) {
        assertThat(fragment.numAnimators.toLong()).isEqualTo(numAnimators.toLong())
        assertThat(fragment.enter).isEqualTo(isEnter)
        assertThat(fragment.resourceId.toLong()).isEqualTo(animatorResourceId.toLong())
        assertThat(fragment.animation).isNotNull()
        assertThat(FragmentTestUtil.waitForAnimationEnd(1000, fragment.animation)).isTrue()
        assertThat(fragment.animation?.hasStarted()!!).isTrue()
    }

    @Throws(InterruptedException::class)
    private fun assertPostponed(fragment: AnimatorFragment, expectedAnimators: Int) {
        assertThat(fragment.mOnCreateViewCalled).isTrue()
        assertThat(fragment.requireView().visibility.toLong()).isEqualTo(View.VISIBLE.toLong())
        assertThat(fragment.requireView().alpha).isWithin(0f).of(0f)
        assertThat(fragment.numAnimators.toLong()).isEqualTo(expectedAnimators.toLong())
    }

    // On Lollipop and earlier, animations are not allowed during window transitions
    private fun waitForAnimationReady() {
        val view = arrayOfNulls<View>(1)
        val activity = mActivityRule.activity
        // Add a view to the hierarchy
        mActivityRule.runOnUiThread {
            view[0] = spy(View(activity))
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.addView(view[0])
        }

        // Wait for its draw method to be called so we know that drawing can happen after
        // the first frame (API 21 didn't allow it during Window transitions)
        verify(view[0], within(1000))?.draw(ArgumentMatchers.any() as Canvas?)

        // Remove the view that we just added
        mActivityRule.runOnUiThread {
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.removeView(view[0])
        }
    }

    class AnimatorFragment : StrictViewFragment() {
        var numAnimators: Int = 0
        var animation: Animation? = null
        var enter: Boolean = false
        var resourceId: Int = 0

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }
            this.numAnimators++
            this.animation = TranslateAnimation(-10f, 0f, 0f, 0f)
            (this.animation as TranslateAnimation).duration = 1
            this.resourceId = nextAnim
            this.enter = enter
            return this.animation
        }
    }

    class AnimationListenerFragment : StrictViewFragment() {
        @get:JvmName("getView_") var view: View? = null
        var mForceRunOnHwLayer: Boolean = false
        var mRepeat: Boolean = false
        var mEnterStartCount = 0
        var mEnterRepeatCount = 0
        var mEnterEndCount = 0
        var mExitStartCount = 0
        var mExitRepeatCount = 0
        var mExitEndCount = 0
        val enterLatch = CountDownLatch(1)
        val exitLatch = CountDownLatch(1)

        fun resetCounts() {
            mEnterEndCount = 0
            mEnterRepeatCount = mEnterEndCount
            mEnterStartCount = mEnterRepeatCount
            mExitEndCount = 0
            mExitRepeatCount = mExitEndCount
            mExitStartCount = mExitRepeatCount
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            if (view != null) {
                return view
            }
            view = super.onCreateView(inflater, container, savedInstanceState)
            if (mForceRunOnHwLayer && view != null) {
                // Set any background color on the TextView, so view.hasOverlappingRendering() will
                // return true, which in turn makes FragmentManagerImpl.shouldRunOnHWLayer() return
                // true.
                view!!.setBackgroundColor(-0x1)
            }
            return view
        }

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }
            val anim = AnimationUtils.loadAnimation(activity, nextAnim)
            if (anim != null) {
                if (mRepeat) {
                    anim.repeatCount = 1
                }
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        if (enter) {
                            mEnterStartCount++
                        } else {
                            mExitStartCount++
                        }
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        if (enter) {
                            mEnterEndCount++
                            enterLatch.countDown()
                        } else {
                            mExitEndCount++
                            // When exiting, the view is detached after onAnimationEnd,
                            // so wait one frame to count down the latch
                            view!!.post { exitLatch.countDown() }
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                        if (enter) {
                            mEnterRepeatCount++
                        } else {
                            mExitRepeatCount++
                        }
                    }
                })
            }
            return anim
        }
    }

    companion object {
        // These are pretend resource IDs for animators. We don't need real ones since we
        // load them by overriding onCreateAnimator
        @AnimRes
        private val ENTER = 1
        @AnimRes
        private val EXIT = 2
        @AnimRes
        private val POP_ENTER = 3
        @AnimRes
        private val POP_EXIT = 4
    }
}
