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

package androidx.recyclerview.widget

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingParent3
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.ArrayList
import java.util.Arrays

@RunWith(Parameterized::class)
@LargeTest
class RecyclerViewNestedScrollingFlingTest(
    private val orientationVertical: Boolean,
    private val scrollDirectionForward: Boolean,
    private val preScrollConsumption: Int,
    private val postScrollConsumption: Int
) {

    private lateinit var mNestedScrollingParent: NestedScrollingParent
    private lateinit var mRecyclerView: RecyclerView

    @Rule
    @JvmField
    var mActivityRule = ActivityTestRule(TestActivity::class.java)

    @Before
    @Throws(Throwable::class)
    fun setup() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        mRecyclerView = RecyclerView(context).apply {
            minimumWidth = 1000
            minimumHeight = 1000
            adapter = TestAdapter(context, 1000)
            val rvOrientation =
                if (orientationVertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
            layoutManager = LinearLayoutManager(context, rvOrientation, false)
        }

        mNestedScrollingParent = NestedScrollingParent(context).apply {
            minimumWidth = 1000
            minimumHeight = 1000
            addView(mRecyclerView)
        }

        val testedFrameLayout = mActivityRule.activity.container
        testedFrameLayout.expectLayouts(1)
        mActivityRule.runOnUiThread { testedFrameLayout.addView(mNestedScrollingParent) }
        testedFrameLayout.waitForLayout(2)
    }

    @Test
    fun uiFingerFling_flingVelocityIsCorrect() {

        val directionalDistance = if (scrollDirectionForward) -200 else 200
        val elapsedTime = 20L
        val expectedVelocity = if (scrollDirectionForward) 10000 else -10000

        if (orientationVertical) {
            mNestedScrollingParent.preScrollY =
                preScrollConsumption * if (scrollDirectionForward) 1 else -1
            mNestedScrollingParent.postScrollY =
                postScrollConsumption * if (scrollDirectionForward) 1 else -1
        } else {
            mNestedScrollingParent.preScrollX =
                preScrollConsumption * if (scrollDirectionForward) 1 else -1
            mNestedScrollingParent.postScrollX =
                postScrollConsumption * if (scrollDirectionForward) 1 else -1
        }

        // If we aren't scrolling forward, then pre scroll so that
        if (!scrollDirectionForward) {
            val x = if (orientationVertical) 0 else 100
            val y = if (orientationVertical) 100 else 0
            mNestedScrollingParent.scrollTo(x, y)
        }

        val velocities = intArrayOf(1, 1)
        mRecyclerView.onFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                velocities[0] = velocityX
                velocities[1] = velocityY
                return false
            }
        }

        val halfDirectionalDistance = directionalDistance / 2
        val halfTime = elapsedTime / 2

        val x2 = if (orientationVertical) 500f else 500f + halfDirectionalDistance
        val y2 = if (orientationVertical) 500f + halfDirectionalDistance else 500f
        val x3 = if (orientationVertical) 500f else 500f + directionalDistance
        val y3 = if (orientationVertical) 500f + directionalDistance else 500f

        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        val move1 = MotionEvent.obtain(0, halfTime, MotionEvent.ACTION_MOVE, x2, y2, 0)
        val move2 = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_MOVE, x3, y3, 0)
        val up = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_UP, x3, y3, 0)

        mNestedScrollingParent.dispatchTouchEvent(down)
        mNestedScrollingParent.dispatchTouchEvent(move1)
        mNestedScrollingParent.dispatchTouchEvent(move2)
        mNestedScrollingParent.dispatchTouchEvent(up)

        val expected =
            if (orientationVertical)
                intArrayOf(0, expectedVelocity)
            else
                intArrayOf(expectedVelocity, 0)

        assertThat(velocities, `is`(expected))
    }

    inner class NestedScrollingParent(context: Context) : FrameLayout(context),
        NestedScrollingChild3, NestedScrollingParent3 {

        var preScrollX: Int = 0
        var postScrollX: Int = 0
        var preScrollY: Int = 0
        var postScrollY: Int = 0

        override fun onStartNestedScroll(
            child: View,
            target: View,
            axes: Int,
            type: Int
        ): Boolean {
            return true
        }

        override fun onNestedScrollAccepted(
            child: View,
            target: View,
            axes: Int,
            type: Int
        ) {}

        override fun onStopNestedScroll(target: View, type: Int) {}

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int
        ) {}

        override fun onNestedPreScroll(
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int
        ) {
            val toScrollX = amountOfScrollToConsume(dx, preScrollX)
            preScrollX -= toScrollX
            consumed[0] += toScrollX

            val toScrollY = amountOfScrollToConsume(dy, preScrollY)
            preScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(toScrollX, toScrollY)
        }

        override fun startNestedScroll(axes: Int, type: Int): Boolean {
            return false
        }

        override fun stopNestedScroll(type: Int) {}

        override fun hasNestedScrollingParent(type: Int): Boolean {
            return false
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?,
            type: Int
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?,
            type: Int
        ): Boolean {
            return false
        }

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
            consumed: IntArray
        ) {
            val toScrollX = amountOfScrollToConsume(dxUnconsumed, postScrollX)
            postScrollX -= toScrollX
            consumed[0] += toScrollX

            val toScrollY = amountOfScrollToConsume(dyUnconsumed, postScrollY)
            postScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(toScrollX, toScrollY)
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?,
            type: Int,
            consumed: IntArray
        ) {
        }

        override fun setNestedScrollingEnabled(enabled: Boolean) {}

        override fun isNestedScrollingEnabled(): Boolean {
            return false
        }

        override fun startNestedScroll(axes: Int): Boolean {
            return false
        }

        override fun stopNestedScroll() {}

        override fun hasNestedScrollingParent(): Boolean {
            return false
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?
        ): Boolean {
            return false
        }

        override fun dispatchNestedFling(
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
            return false
        }

        override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {}

        override fun onStopNestedScroll(target: View) {}

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int
        ) {}

        override fun onNestedPreScroll(
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray
        ) {}

        override fun onNestedFling(
            target: View,
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean
        ): Boolean {
            return false
        }

        override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        override fun getNestedScrollAxes(): Int {
            return 0
        }

        private fun amountOfScrollToConsume(d: Int, max: Int): Int {
            if (d < 0 && max < 0) {
                return Math.max(d, max)
            } else if (d > 0 && max > 0) {
                return Math.min(d, max)
            }
            return 0
        }
    }

    private inner class TestAdapter internal constructor(
        private val mContext: Context,
        private val itemSize: Int
    ) : RecyclerView.Adapter<TestViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TestViewHolder {
            val view = View(mContext)
            view.layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            view.minimumWidth = itemSize
            view.minimumHeight = itemSize
            return TestViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {}

        override fun getItemCount(): Int {
            return 1
        }
    }

    private inner class TestViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView)

    companion object {

        @JvmStatic
        @Parameterized.Parameters(
            name = "orientationVertical:{0}," +
                    "scrollDirectionForward:{1}," +
                    "preScrollConsumption:{2}," +
                    "postScrollConsumption:{3}"
        )
        fun data(): Collection<Array<Any>> {
            val configurations = ArrayList<Array<Any>>()

            for (orientationVertical in booleanArrayOf(true, false)) {
                for (scrollDirectionForward in booleanArrayOf(true, false)) {
                    configurations.addAll(
                        Arrays.asList(
                            *arrayOf(
                                arrayOf(orientationVertical, scrollDirectionForward, 0, 0),
                                arrayOf(orientationVertical, scrollDirectionForward, 25, 0),
                                arrayOf(orientationVertical, scrollDirectionForward, 50, 0),
                                arrayOf(orientationVertical, scrollDirectionForward, 100, 0),
                                arrayOf(orientationVertical, scrollDirectionForward, 0, 25),
                                arrayOf(orientationVertical, scrollDirectionForward, 0, 50),
                                arrayOf(orientationVertical, scrollDirectionForward, 0, 100),
                                arrayOf(orientationVertical, scrollDirectionForward, 12, 13),
                                arrayOf(orientationVertical, scrollDirectionForward, 25, 25),
                                arrayOf(orientationVertical, scrollDirectionForward, 50, 50)
                            )
                        )
                    )
                }
            }

            return configurations
        }
    }
}
