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

package androidx.viewpager2.widget

import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val STATE_RESUMED = "RESUMED"
private const val STATE_STARTED = "STARTED"
private const val STATE_INITIALIZING = "INITIALIZING"
private const val STATE_CREATED = "CREATED"
private const val STATE_ACTIVITY_CREATED = "ACTIVITY_CREATED"

private val FRAGMENT_LIFECYCLE_STATES = listOf(
    STATE_INITIALIZING,
    STATE_CREATED,
    STATE_ACTIVITY_CREATED,
    STATE_STARTED,
    STATE_RESUMED
)

// TODO: add tests for data-set changes

/**  Verifies that primary item Fragment is RESUMED while the rest is STARTED */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentLifecycleTest : BaseTest() {
    private val orientation = ORIENTATION_HORIZONTAL
    private val totalPages = 10
    private val adapterProvider = fragmentAdapterProviderValueId
    private val timeoutMs = 3000L

    @Test
    fun test_swipeBetweenPages() {
        setUpTest(orientation).apply {
            val expectedValues = stringSequence(totalPages).toMutableList()
            val adapter = adapterProvider(expectedValues.toList()) // immutable defensive copy
            setAdapterSync(adapter)

            var ix = 0
            performAssertions(expectedValues, ix)

            val swipeCount = expectedValues.count() - 1
            repeat(swipeCount) {
                val latch = viewPager.addWaitForIdleLatch()
                swipeForward()
                latch.await(timeoutMs, MILLISECONDS)

                performAssertions(expectedValues, ++ix)
            }

            repeat(swipeCount) {
                val latch = viewPager.addWaitForIdleLatch()
                swipeBackward()
                latch.await(timeoutMs, MILLISECONDS)

                performAssertions(expectedValues, --ix)
            }
        }
    }

    @Test
    fun test_setCurrentItem() {
        setUpTest(orientation).apply {
            val expectedValues = stringSequence(totalPages).toMutableList()
            val adapter = adapterProvider(expectedValues.toList()) // immutable defensive copy
            setAdapterSync(adapter)

            performAssertions(expectedValues, 0)

            // Pair(targetPageIx, isSmoothScroll)
            val steps = listOf(
                9 to true,
                0 to false,
                1 to true,
                8 to false,
                7 to false,
                9 to true,
                5 to true,
                0 to true
            )

            steps.forEach { (target, isSmoothScroll) ->
                viewPager.setCurrentItemSync(target, isSmoothScroll, timeoutMs, MILLISECONDS)
                performAssertions(expectedValues, target)
            }
        }
    }

    /**
     * Verifies the following:
     *  - Primary item Fragment is Resumed
     *  - Other Fragments are Started
     *  - Correct page content is displayed
     *  - ViewPager2 currentItem is correct
     *  - A11y page actions are correct
     *  - Adapter self-checks if present are passing
     */
    private fun Context.performAssertions(expectedValues: MutableList<String>, ix: Int) {
        val fragmentInfo: List<FragmentInfo> =
            activity.supportFragmentManager.fragments.map {
                FragmentInfo(
                    it.tag!!,
                    it.stateString,
                    it.isResumed
                )
            }

        val resumed = fragmentInfo.filter { it.state == STATE_RESUMED }.toList()
        assertThat(resumed.count(), equalTo(1))
        val expectedId = expectedValues[ix].toInt()
        val expectedTag = "f$expectedId"
        assertThat(resumed.first().tag, equalTo(expectedTag))
        assertThat(resumed.first().isResumed, equalTo(true))

        fragmentInfo.filter { it.state != STATE_RESUMED }.forEach { fi ->
            assertThat(fi.state, equalTo(STATE_STARTED))
            assertThat(fi.isResumed, equalTo(false))
        }

        assertBasicState(ix, expectedValues[ix])
    }
}

private data class FragmentInfo(val tag: String, val state: String, val isResumed: Boolean)

private fun fragmentStateToString(state: Int): String =
    FRAGMENT_LIFECYCLE_STATES.first { getFragmentFieldValue<Int>(it, null) == state }

private val (Fragment).stateInt: Int get() = getFragmentFieldValue("mState", this)

private val (Fragment).stateString: String get() = fragmentStateToString(this.stateInt)

private fun <T> getFragmentFieldValue(methodName: String, target: Fragment?): T {
    val field = Fragment::class.java.declaredFields.first { it.name.contains(methodName) }
    field.isAccessible = true
    val result = field.get(target)
    @Suppress("UNCHECKED_CAST")
    return result as T
}
