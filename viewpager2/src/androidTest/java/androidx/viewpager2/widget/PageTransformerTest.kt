/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import androidx.viewpager2.widget.PageTransformerTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PageTransformerTest.Event.TransformPageEvent
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.ViewPager2.PageTransformer
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
@LargeTest
class PageTransformerTest : BaseTest() {
    private val epsilon = 0.000001f

    /*
    When the ViewPager2 smooth scrolls to another page, it should generate transform events. The
    resulting event stream should have certain properties:

    - Every scroll event should be accompanied by one or more
      transform events, this is called a frame in this test
    - Frames do not overlap in time
    - All transform events in a frame are from different pages
    - The transform events of a certain page are in a contiguous stream of frames
    - The transform events of a certain page may only move that page in one direction
    - The pages of the transform events in a frame cover the whole screen (no gaps and no borders)
    - The ordering of pages must be consistent (no overtaking)

    For comparison, here are the event streams of ViewPager and ViewPager2:

    0 -> 1 (ViewPager)                       0 -> 1 (ViewPager2)
    onPageScrollStateChanged(2)              onPageScrollStateChanged(2)
    onPageSelected(1)                        onPageSelected(1)
    onPageScrolled(0, 0.162963, 176)         onPageScrolled(0, 0.181727, 181)
    transformPage(0, -0.162963)              transformPage(0, -0.181727)
    transformPage(1, 0.837037)               transformPage(1, 0.818273)
    transformPage(2, 1.837037)               transformPage(2, 1.818273)
    onPageScrolled(0, 0.457407, 494)         onPageScrolled(0, 0.344378, 343)
    transformPage(0, -0.457407)              transformPage(0, -0.344378)
    transformPage(1, 0.542593)               transformPage(1, 0.655622)
    transformPage(2, 1.542593)               transformPage(2, 1.655622)
    onPageScrolled(0, 0.672222, 726)         onPageScrolled(0, 0.488956, 487)
    transformPage(0, -0.672222)              transformPage(0, -0.488956)
    transformPage(1, 0.327778)               transformPage(1, 0.511044)
    transformPage(2, 1.327778)               transformPage(2, 1.511044)
    onPageScrolled(0, 0.812963, 878)         onPageScrolled(0, 0.608434, 606)
    transformPage(0, -0.812963)              transformPage(0, -0.608434)
    transformPage(1, 0.187037)               transformPage(1, 0.391566)
    transformPage(2, 1.187037)               transformPage(2, 1.391566)
    onPageScrolled(0, 0.900926, 973)         onPageScrolled(0, 0.718876, 716)
    transformPage(0, -0.900926)              transformPage(0, -0.718876)
    transformPage(1, 0.099074)               transformPage(1, 0.281124)
    transformPage(2, 1.099074)               transformPage(2, 1.281124)
    onPageScrolled(0, 0.951852, 1028)        onPageScrolled(0, 0.810241, 807)
    transformPage(0, -0.951852)              transformPage(0, -0.810241)
    transformPage(1, 0.048148)               transformPage(1, 0.189759)
    transformPage(2, 1.048148)               transformPage(2, 1.189759)
    onPageScrolled(0, 0.978704, 1057)        onPageScrolled(0, 0.883534, 880)
    transformPage(0, -0.978704)              transformPage(0, -0.883534)
    transformPage(1, 0.021296)               transformPage(1, 0.116466)
    transformPage(2, 1.021296)               transformPage(2, 1.116466)
    onPageScrolled(0, 0.991667, 1071)        onPageScrolled(0, 0.939759, 936)
    transformPage(0, -0.991667)              transformPage(0, -0.939759)
    transformPage(1, 0.008333)               transformPage(1, 0.060241)
    transformPage(2, 1.008333)               transformPage(2, 1.060241)
    onPageScrolled(0, 0.998148, 1078)        onPageScrolled(0, 0.975904, 972)
    transformPage(0, -0.998148)              transformPage(0, -0.975904)
    transformPage(1, 0.001852)               transformPage(1, 0.024096)
    transformPage(2, 1.001852)               transformPage(2, 1.024096)
    onPageScrolled(1, 0.000000, 0)           onPageScrolled(0, 0.995984, 992)
    transformPage(0, -1.000000)              transformPage(0, -0.995984)
    transformPage(1, 0.000000)               transformPage(1, 0.004016)
    transformPage(2, 1.000000)               transformPage(2, 1.004016)
    onPageScrollStateChanged(0)              onPageScrolled(1, 0.000000, 0)
                                             transformPage(1, 0.000000)
                                             transformPage(2, 1.000000)
                                             onPageScrollStateChanged(0)
     */

    fun test_transformer_smoothScroll(@ViewPager2.Orientation orientation: Int) {
        // given
        setUpTest(100, orientation).apply {

            // when
            listOf(1, 3, 6, 10, 15, 99, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                val listener = viewPager.addNewRecordingListener()

                val pageIxDelta = targetPage - currentPage
                if (pageIxDelta == 0) {
                    return@forEach
                }

                gotoPage(targetPage, true, 2, SECONDS)

                // then
                listener.apply {
                    assertThat(events.first(), instanceOf(OnPageScrolledEvent::class.java))
                    assertThat(events.last(), instanceOf(TransformPageEvent::class.java))
                    val sortOrder = if (pageIxDelta > 0) SortOrder.ASC else SortOrder.DESC
                    pageIndices.forEach {
                        transformPageEventsOf(it).assertMonotonic(sortOrder)
                    }
                    frames.assertUniquePagesPerFrame()
                    pageIndices.forEach {
                        frames.assertPageTransformsAreContiguousFor(it)
                    }
                    frames.assertUniformSpacingBetweenPages()
                    frames.assertScreenFilledEachFrame()
                    pageIndices.forEach { pageA ->
                        val pageAEvents = indexedEventsOf(pageA)
                        pageIndices.forEach { pageB ->
                            if (pageA != pageB) {
                                pageAEvents.assertNoOvertakingOf(indexedEventsOf(pageB))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_transformer_smoothScroll_horizontal() {
        test_transformer_smoothScroll(HORIZONTAL)
    }

    @Test
    fun test_transformer_smoothScroll_vertical() {
        test_transformer_smoothScroll(VERTICAL)
    }

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val layoutManager = (getChildAt(0) as RecyclerView).layoutManager as LinearLayoutManager
        val listener = RecordingListener(layoutManager)
        setPageTransformer(listener)
        addOnPageChangeListener(listener)
        return listener
    }

    private sealed class Event {
        data class TransformPageEvent(
            val page: Int,
            val offset: Float
        ) : Event()

        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
    }

    private val Pair<Int, TransformPageEvent>.index get() = first
    private val Pair<Int, TransformPageEvent>.event get() = second

    private class RecordingListener(val layoutManager: LinearLayoutManager) :
        PageTransformer, OnPageChangeListener {
        val events = mutableListOf<Event>()
        val frames by lazy {
            events.foldIndexed(mutableListOf<MutableList<TransformPageEvent>>()) { i, groups, e ->
                if (e is Event.OnPageScrolledEvent) {
                    groups.add(mutableListOf())
                } else if (e is TransformPageEvent) {
                    groups.last().add(e)
                }
                groups
            }
        }
        val pageIndices by lazy {
            events.mapNotNull { (it as? TransformPageEvent)?.page }.distinct()
        }

        fun transformPageEventsOf(page: Int): List<TransformPageEvent> {
            return events.mapNotNull {
                if (it is TransformPageEvent && it.page == page) it else null
            }
        }

        fun indexedEventsOf(page: Int): List<Pair<Int, TransformPageEvent>> {
            return frames.mapIndexedNotNull { ix, frame ->
                frame.find { it.page == page }?.withIndex(ix)
            }
        }

        private fun TransformPageEvent.withIndex(ix: Int): Pair<Int, TransformPageEvent> {
            return Pair(ix, this)
        }

        override fun transformPage(page: View, position: Float) {
            events.add(TransformPageEvent(layoutManager.getPosition(page), position))
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }

        override fun onPageSelected(position: Int) {
        }

        override fun onPageScrollStateChanged(state: Int) {
        }
    }

    private fun List<TransformPageEvent>.assertMonotonic(sortOrder: SortOrder) {
        map { it.offset }.assertSorted { it * -sortOrder.sign }
    }

    private fun List<List<TransformPageEvent>>.assertUniquePagesPerFrame() {
        forEach {
            it.map { it.page }.apply {
                assertThat(size, equalTo(distinct().size))
            }
        }
    }

    private fun List<List<TransformPageEvent>>.assertPageTransformsAreContiguousFor(page: Int) {
        val containsPage: (List<TransformPageEvent>) -> Boolean = { it.any { it.page == page } }
        assertFalse(
            map(containsPage).dropWhile { !it }.dropWhile { it }.any { it }
        )
    }

    private fun List<List<TransformPageEvent>>.assertUniformSpacingBetweenPages() {
        forEach {
            it.sortedBy { it.offset }.zipWithNext { a, b ->
                assertThat(b.offset - a.offset, isBetweenInIn(1f - epsilon, 1f + epsilon))
            }
        }
    }

    private fun List<List<TransformPageEvent>>.assertScreenFilledEachFrame() {
        // check that in each frame, there is either a page with offset 0,
        // or at least one negative and one positive offset
        forEach {
            // if there is an event with offset 0, then all's fine
            if (it.none { it.offset == 0f }) {
                // otherwise, check if the last negative and first
                // positive offsets are not more then 0 apart
                val pageOffsets = it.map { it.offset }
                assertTrue(pageOffsets.any { it < 0 })
                assertTrue(pageOffsets.any { it > 0 })
            }
        }
    }

    private fun List<Pair<Int, TransformPageEvent>>.assertNoOvertakingOf(
        indexedEvents: List<Pair<Int, TransformPageEvent>>
    ) {
        filter { event ->
            // First, remove all frames that do not occur in the other list
            indexedEvents.any { other -> event.index == other.index }
        }.map { event ->
            // Then, zip the events of the two lists together by frame
            Pair(event.event, indexedEvents.find { it.index == event.index }!!.event)
        }.fold(0) { prevRelation, pair ->
            // Then, check that all pairs have the same equivalence relation
            val currRelation = pair.first.offset.compareTo(pair.second.offset)
            when {
                prevRelation == 0 -> assertThat(currRelation, not(0))
                prevRelation < 0 -> assertThat(currRelation, lessThan(0))
                prevRelation > 0 -> assertThat(currRelation, greaterThan(0))
            }
            currRelation
        }
    }
}
