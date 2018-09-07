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

package androidx.viewpager2.widget

import android.os.SystemClock
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.widget.RapidlySetCurrentItem.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.RapidlySetCurrentItem.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.RapidlySetCurrentItem.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.ViewPager2.ScrollState.DRAGGING
import androidx.viewpager2.widget.ViewPager2.ScrollState.SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random

@RunWith(Parameterized::class)
@LargeTest
class RapidlySetCurrentItem(private val config: RapidlySetCurrentItemConfig) : BaseTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<RapidlySetCurrentItemConfig> = createTestSet()
    }

    @Test
    fun test() {
        config.apply {
            // given
            setUpTest(totalPages, orientation).apply {
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()

                // when
                pageSequence.forEach { targetPage ->
                    runOnUiThread {
                        viewPager.setCurrentItem(targetPage, true)
                        viewPager.assertCurrentItemSet(targetPage)
                        listener.assertTargetPageSelected(targetPage)
                    }
                    SystemClock.sleep(100)
                }
                PollingCheck.waitFor(2000) {
                    (listener.lastEvent as? Event.OnPageScrollStateChangedEvent)?.state == 0
                }

                // then
                listener.apply {
                    assertThat(settlingIx, equalTo(0))
                    assertThat(draggingIx, equalTo(-1))
                    assertThat((lastEvent as? OnPageScrollStateChangedEvent)?.state, equalTo(0))
                    assertThatStateEventsDoNotRepeat()
                    assertScrollTowardsSelectedPage()
                }
            }
        }
    }

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val listener = RecordingListener()
        addOnPageChangeListener(listener)
        return listener
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

    private class RecordingListener : ViewPager2.OnPageChangeListener {
        val events = mutableListOf<Event>()

        val stateEvents get() = events.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val lastEvent get() = events.last()
        val settlingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SETTLING))
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(DRAGGING))

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }

        override fun onPageSelected(position: Int) {
            events.add(OnPageSelectedEvent(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            events.add(OnPageScrollStateChangedEvent(state))
        }
    }

    private fun RecordingListener.assertTargetPageSelected(targetPage: Int) {
        assertThat(lastEvent, instanceOf(OnPageSelectedEvent::class.java))
        val selectedEvent = lastEvent as Event.OnPageSelectedEvent
        assertThat(selectedEvent.position, equalTo(targetPage))
    }

    private fun RecordingListener.assertThatStateEventsDoNotRepeat() {
        stateEvents.zipWithNext { a, b ->
            assertThat("State transition to same state found", a.state, not(equalTo(b.state)))
        }
    }

    private fun RecordingListener.assertScrollTowardsSelectedPage() {
        var target = 0
        var prevPosition = 0f
        events.forEach {
            when (it) {
                is OnPageSelectedEvent -> target = it.position
                is Event.OnPageScrolledEvent -> {
                    val currentPosition = it.position + it.positionOffset
                    assertThat(
                        "Scroll event fired before page selected event",
                        target, not(equalTo(-1))
                    )
                    assertThat(
                        "Scroll event not between start and destination",
                        currentPosition, isBetweenInInMinMax(prevPosition, target.toFloat())
                    )
                    prevPosition = currentPosition
                }
            }
        }
    }

    private fun ViewPager2.assertCurrentItemSet(targetPage: Int) {
        assertThat(currentItem, equalTo(targetPage))
    }
}

// region Parameter definition

data class RapidlySetCurrentItemConfig(
    val title: String,
    @ViewPager2.Orientation val orientation: Int,
    val totalPages: Int,
    val pageSequence: List<Int>
) {
    override fun toString(): String {
        return "$title-pages-$totalPages-seq-${pageSequence.joinToString("-")}"
    }
}

// endregion

// region Test Suite creation

private fun createTestSet(): List<RapidlySetCurrentItemConfig> {
    return listOf(
        ViewPager2.Orientation.HORIZONTAL,
        ViewPager2.Orientation.VERTICAL
    ).flatMap { orientation -> createTestSet(orientation) }
}

private fun createTestSet(orientation: Int): List<RapidlySetCurrentItemConfig> {
    return listOf(
        RapidlySetCurrentItemConfig(
            title = "cone-increasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6, 3, 7, 3, 8, 4, 9)
        ),
        RapidlySetCurrentItemConfig(
            title = "cone-increasing-fast",
            orientation = orientation,
            totalPages = 19,
            pageSequence = listOf(2, 1, 4, 2, 6, 3, 8, 4, 10, 5, 12, 6, 14, 7, 16, 8, 18)
        ),
        RapidlySetCurrentItemConfig(
            title = "cone-decreasing-slow",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(9, 8, 9, 7, 8, 6, 8, 5, 7, 4, 7, 3, 6, 2, 6, 1, 5, 0)
        ),
        RapidlySetCurrentItemConfig(
            title = "cone-decreasing-fast",
            orientation = orientation,
            totalPages = 19,
            pageSequence = listOf(18, 16, 17, 14, 16, 12, 15, 10, 14, 8, 13, 6, 12, 4, 11, 2, 10, 0)
        ),
        RapidlySetCurrentItemConfig(
            title = "regression-hump-positive",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(7, 6, 0, 7, 6, 0, 7, 6)
        ),
        RapidlySetCurrentItemConfig(
            title = "regression-hump-negative",
            orientation = orientation,
            totalPages = 10,
            pageSequence = listOf(8, 2, 3, 8, 2, 3, 8, 2, 3)
        ),
        RapidlySetCurrentItemConfig(
            title = "regression-do-not-jump-forward",
            orientation = orientation,
            totalPages = 20,
            pageSequence = listOf(3, 6, 9, 5)
        )
    )
}

// endregion

// region Random test creation

private fun createRandomTest(orientation: Int): RapidlySetCurrentItemConfig {
    return recreateRandomTest(orientation)
}

private fun recreateRandomTest(
    orientation: Int,
    seed: Long = generatePositiveSeed(),
    numScrolls: Int = 10,
    numPages: Int = 12
): RapidlySetCurrentItemConfig {
    return RapidlySetCurrentItemConfig(
        title = "random-seed-$seed",
        orientation = orientation,
        totalPages = numPages,
        pageSequence = generateRandomSequence(seed, numScrolls, numPages)
    )
}

private fun generatePositiveSeed(): Long {
    return Random().nextLong() and (1L shl 63).inv()
}

private fun generateRandomSequence(seed: Long, len: Int, max: Int): List<Int> {
    val r = Random(seed)
    var page = 0
    return List(len) {
        page = generateRandomNumber(r, max, page)
        return@List page
    }
}

private fun generateRandomNumber(r: Random, max: Int, exclude: Int): Int {
    var num: Int
    do num = r.nextInt(max) while (num == exclude)
    return num
}

// endregion