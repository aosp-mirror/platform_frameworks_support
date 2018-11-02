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

package androidx.recyclerview.widget

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val RV_WIDTH = 500
private val RV_HEIGHT = 500
private val ITEM_WIDTH = 500
private val ITEM_HEIGHT = 200
private val NUM_ITEMS = 100

// TODO: This probably isn't a small test
@SmallTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewSmoothScrollToPositionTest {

    internal lateinit var mRecyclerView: RecyclerView

    @get:Rule
    val mActivityTestRule = ActivityTestRule(TestContentViewActivity::class.java)

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val context = mActivityTestRule.activity

        mRecyclerView = RecyclerView(context)
        mRecyclerView.layoutParams = ViewGroup.LayoutParams(RV_WIDTH, RV_HEIGHT)
        mRecyclerView.setBackgroundColor(0x7A7A7AFF)
        mRecyclerView.layoutManager = LinearLayoutManager(context)
        mRecyclerView.adapter = MyAdapter()

        val testContentView = mActivityTestRule.activity.contentView
        testContentView.expectLayouts(1)
        mActivityTestRule.runOnUiThread { testContentView.addView(mRecyclerView) }
        testContentView.awaitLayouts(2)
    }

    @Test
    @Throws(Throwable::class)
    fun smoothScrollToPosition_targetOffScreenBelow_atIdleTargetAtBottom() {
        val latch = CountDownLatch(1)
        var targetView: View? = null
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    latch.countDown()
                    targetView = getTargetViewWithText(mRecyclerView, "2")
                }
            }
        })

        mActivityTestRule.runOnUiThread { mRecyclerView.smoothScrollToPosition(2) }
        assertThat(latch.await(2, TimeUnit.SECONDS), `is`(true))

        assertThat(targetView, `is`(notNullValue()))
        assertThat(targetView!!.bottom, `is`(RV_HEIGHT))
    }

    /**
     * smoothScrollToPosition(int, int)  - This is the only thing that creates a smoothScroller
     * _targetOffScreen
     * __getsToPosition
     * __callsCallbacksCorrectly
     * _targetPartiallyOnScreen
     * __getsToPosition
     * __callsCallbacksCorrectly
     * _targetIsOnScreen
     * __doesNotMove
     * __callsNoCallbacks
     * _calledTwice
     * __getsToPosition
     * __callsCallbacksCorrectly
     *
     * _calledDuringSmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringSmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringUiFling
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastSmoothScrollerOnAnimation
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedBySmoothScrollToPosition
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedBySmoothScrollBy
     * __changesDirections
     * __completesCallbacksCorrectly
     *
     * _calledDuringFirstScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringMiddleScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     * _calledDuringLastScrollListenerOnScrolled_startedByFling
     * __changesDirections
     * __completesCallbacksCorrectly
     */
}

private fun getTargetViewWithText(viewGroup: ViewGroup, text: CharSequence): View? {
    for (i in 0 until viewGroup.childCount) {
        val view = viewGroup.getChildAt(i)
        if (view is TextView && text == view.text) return view
    }
    return null
}

private class MyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object : RecyclerView.ViewHolder(
            TextView(parent.context).apply {
                minWidth = ITEM_WIDTH
                minHeight = ITEM_HEIGHT
            }
        ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).text = Integer.toString(position)
    }

    override fun getItemCount() = NUM_ITEMS
}