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

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.ViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SetScrollingTouchSlopTest : BaseTest() {
    private lateinit var test: Context
    private lateinit var viewConfiguration: ViewConfiguration

    override fun setUp() {
        super.setUp()
        test = setUpTest(ViewPager2.ORIENTATION_HORIZONTAL)
        test.setAdapterSync(viewAdapterProvider(stringSequence(2)))
        viewConfiguration = ViewConfiguration.get(test.activity)
    }

    @Test
    fun test_defaultTouchSlop() {
        testWithExpectedTouchSlop(viewConfiguration.scaledPagingTouchSlop)
    }

    @Test
    fun test_pagingTouchSlop() {
        test.viewPager.setScrollingTouchSlop(ViewPager2.TOUCH_SLOP_PAGING)
        testWithExpectedTouchSlop(viewConfiguration.scaledPagingTouchSlop)
    }

    @Test
    fun test_continuousTouchSlop() {
        test.viewPager.setScrollingTouchSlop(ViewPager2.TOUCH_SLOP_CONTINUOUS)
        testWithExpectedTouchSlop(viewConfiguration.scaledTouchSlop)
    }

    private fun testWithExpectedTouchSlop(expectedTouchSlop: Int) {
        val watcher = OnPageScrolledWatcher().also {
            test.viewPager.registerOnPageChangeCallback(it)
        }

        val x0 = test.viewPager.width / 2f
        val y0 = test.viewPager.height / 2f
        var dx: Int

        // when
        dispatchMotionEvent(0, ACTION_DOWN, x0, y0)
        dx = expectedTouchSlop
        dispatchMotionEvent(20, ACTION_MOVE, x0, y0, dx)
        // then
        assertThat("No scroll expected when dragging ${dx}px)", watcher.hasScrolled, equalTo(false))

        // when
        dx = expectedTouchSlop + 1
        dispatchMotionEvent(40, ACTION_MOVE, x0, y0, dx)
        // then
        assertThat("Scroll expected when dragging ${dx}px)", watcher.hasScrolled, equalTo(true))
        assertThat("Should have scrolled by 1px", watcher.scrolledOffset, equalTo(1))
    }

    private fun dispatchMotionEvent(dt: Long, action: Int, x: Float, y: Float, dx: Int = 0) {
        val event = MotionEvent.obtain(0, dt, action, x - dx, y, 0)
        try {
            activityTestRule.runOnUiThread {
                test.viewPager.dispatchTouchEvent(event)
            }
        } finally {
            event.recycle()
        }
    }

    private class OnPageScrolledWatcher : ViewPager2.OnPageChangeCallback() {
        var hasScrolled = false
        var scrolledOffset = 0
        override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
            hasScrolled = true
            scrolledOffset = offsetPixels
        }
    }
}
