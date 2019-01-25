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

import android.os.SystemClock.sleep
import androidx.test.filters.LargeTest
import androidx.testutils.FragmentActivityUtils.waitForCycles
import androidx.viewpager2.widget.DisableUserScrollingTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.DisableUserScrollingTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.DisableUserScrollingTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.DisableUserScrollingTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt

/**
 * Tests what happens when a smooth scroll is interrupted by a drag
 */
@RunWith(Parameterized::class)
@LargeTest
class DisableUserScrollingTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val configChange: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    private val pageCount = 10
    private val firstPage = 0
    private val middlePage = pageCount / 2
    private val lastPage = pageCount - 1

    private lateinit var test: Context
    private lateinit var adapterProvider: AdapterProvider

    override fun setUp() {
        super.setUp()
        adapterProvider = viewAdapterProvider(stringSequence(pageCount))
        test = setUpTest(config.orientation).also {
            it.viewPager.isUserScrollable = false
            it.setAdapterSync(adapterProvider)
            it.assertBasicState(0)
        }
    }

    @Test
    fun testSwipe() {
        listOf(firstPage, firstPage + 1).forEach { swipeToAndVerifyNothingHappened(it) }
        test.viewPager.setCurrentItemSync(middlePage, false, 2, SECONDS)
        listOf(middlePage - 1, middlePage + 1).forEach { swipeToAndVerifyNothingHappened(it) }
        test.viewPager.setCurrentItemSync(lastPage, false, 2, SECONDS)
        listOf(lastPage - 1, lastPage).forEach { swipeToAndVerifyNothingHappened(it) }
    }

    private fun swipeToAndVerifyNothingHappened(targetPage: Int) {
        // given
        val recorder = test.viewPager.addNewRecordingCallback()
        val currentPage = test.viewPager.currentItem

        // when
        test.swipe(currentPage, targetPage)
        sleep(100)
        waitForCycles(3, test.activityTestRule)

        // then
        test.assertBasicState(currentPage)
        assertThat(recorder.eventCount, equalTo(0))

        test.viewPager.unregisterOnPageChangeCallback(recorder)

        // verify config change
        if (config.configChange) {
            verifyConfigChange(currentPage)
        }
    }

    private fun testSetCurrentItem(smoothScroll: Boolean) {
        listOf(1, 9, 7, 0).forEach { targetPage ->
            // given
            val currentPage = test.viewPager.currentItem
            val recorder = test.viewPager.addNewRecordingCallback()

            // when
            test.viewPager.setCurrentItemSync(targetPage, smoothScroll, 2, SECONDS)

            // then
            assertThat(test.viewPager.currentItem, equalTo(targetPage))
            val pageSize = test.viewPager.pageSize
            recorder.scrollEvents.assertValueSanity(currentPage, targetPage, pageSize)
            recorder.scrollEvents.assertLastCorrect(targetPage)
            recorder.selectEvents.assertSelected(listOf(targetPage))

            test.viewPager.unregisterOnPageChangeCallback(recorder)

            // verify config change
            if (config.configChange) {
                verifyConfigChange(targetPage)
            }
        }
    }

    @Test
    fun testSetCurrentItemSmooth() {
        testSetCurrentItem(true)
    }

    @Test
    fun testSetCurrentItemNotSmooth() {
        testSetCurrentItem(false)
    }

    private fun verifyConfigChange(page: Int) {
        test.recreateActivity(adapterProvider)
        test.assertBasicState(page)
        assertThat(test.viewPager.isUserScrollable, equalTo(false))
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
    }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
        data class OnPageSelectedEvent(val position: Int) : Event()
        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val eventCount get() = events.size
        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val selectEvents get() = events.mapNotNull { it as? OnPageSelectedEvent }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            synchronized(events) {
                events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
            }
        }

        override fun onPageSelected(position: Int) {
            synchronized(events) {
                events.add(OnPageSelectedEvent(position))
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            synchronized(events) {
                events.add(OnPageScrollStateChangedEvent(state))
            }
        }
    }

    private fun List<OnPageScrolledEvent>.assertValueSanity(
        initialPage: Int,
        otherPage: Int,
        pageSize: Int
    ) = forEach {
        assertThat(it.position, isBetweenInInMinMax(initialPage, otherPage))
        assertThat(it.positionOffset, isBetweenInEx(0f, 1f))
        assertThat((it.positionOffset * pageSize).roundToInt(), equalTo(it.positionOffsetPixels))
    }

    private fun List<OnPageScrolledEvent>.assertLastCorrect(targetPage: Int) {
        last().apply {
            assertThat(position, equalTo(targetPage))
            assertThat(positionOffsetPixels, equalTo(0))
        }
    }

    private fun List<OnPageSelectedEvent>.assertSelected(pages: List<Int>) {
        assertThat(map { it.position }, equalTo(pages))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).map { configChange ->
            TestConfig(
                orientation = orientation,
                configChange = configChange
            )
        }
    }
}

// endregion
