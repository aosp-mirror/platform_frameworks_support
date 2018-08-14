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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.PageTransformerTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PageTransformerTest.Event.TransformPageEvent
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
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
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class PageTransformerTest : BaseTest() {

    private val EPSILON = 0.000001f

    @Test
    fun test_transformer_smoothScroll() {
        // given
        setUpTest(100, HORIZONTAL).apply {

            // when
            val targets = randomList(20, 0).plus(listOf(99, 0))
            targets.forEach { targetPage ->
                val currentPage = viewPager.currentItem
                val listener = viewPager.addNewRecordingListener()

                gotoPageWithCommand(targetPage) {
                    viewPager.setCurrentItem(targetPage, true)
                }

                // then
                val pageIxDelta = targetPage - currentPage
                if (pageIxDelta == 0) {
                    return@forEach
                }
                listener.apply {
                    // check that first recorded event is a transform event
                    assertThat(events.first(), instanceOf(TransformPageEvent::class.java))
                    // check that last recorded event is a scroll event
                    assertThat(events.last(), instanceOf(OnPageScrolledEvent::class.java))
                    // check that offsets of a single page are monotonic
                    val sortOrder = if (pageIxDelta > 0) SortOrder.ASC else SortOrder.DESC
                    pages.forEach {
                        eventsOf(it).assertMonotonic(sortOrder)
                    }
                    // check that all events between scroll events are from different pages
                    frames.assertUniquePagesPerFrame()
                    // check that there is exactly one scroll event
                    // between all events of a single page
                    pages.forEach {
                        frames.assertPageTransformsAreContiguousFor(it)
                    }
                    // check that all transform offsets between two scroll events
                    // are one apart of each other
                    frames.assertSpaceBetweenPages()
                    // check that between two scroll events there is always
                    // either a page with offset 0, or one negative and one positive offset
                    frames.assertScreenFilledEachFrame()
                    // check that pages don't 'overtake' each other
                    pages.forEach { pageA ->
                        val pageAEvents = indexedEventsOf(pageA)
                        pages.forEach { pageB ->
                            if (pageA != pageB) {
                                pageAEvents.assertNoOvertakingOf(indexedEventsOf(pageB))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Context.gotoPageWithCommand(targetPage: Int, command: () -> Unit) {
        val latch = viewPager.addWaitForScrolledLatch(targetPage, false)

        // temporary hack to stop the tests from failing
        // this most likely shows a bug in PageChangeListener - communicating IDLE before
        // RecyclerView is ready; TODO: investigate further and fix
        val latchRV = CountDownLatch(1)
        val rv = viewPager.getChildAt(0) as RecyclerView
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == 0) {
                    latchRV.countDown()
                }
            }
        })
        runOnUiThread(command)
        latch.await(2, SECONDS)
        latchRV.await(2, SECONDS)
    }

    private fun randomList(numPages: Int, length: Int): List<Int> {
        val random = Random()
        return List(length) { random.nextInt(numPages) }
    }

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val layoutManager = (getChildAt(0) as RecyclerView).layoutManager as LinearLayoutManager
        val listener = RecordingListener(layoutManager)
        setPageTransformer(false, listener)
        addOnPageChangeListener(listener)
        return listener
    }

    private sealed class Event {
        // void transformPage(@NonNull View page, float offset);
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

    private class RecordingListener(val layoutManager: LinearLayoutManager) :
        PageTransformer, OnPageChangeListener {
        val events = mutableListOf<Event>()
        val frames by lazy {
            events.foldIndexed(mutableListOf(mutableListOf<TransformPageEvent>())) { i, groups, e ->
                if (e is Event.OnPageScrolledEvent) {
                    if (0 < i && i < events.size - 1) {
                        groups.add(mutableListOf())
                    }
                } else if (e is TransformPageEvent) {
                    groups.last().add(e)
                }
                groups
            }
        }
        val pages by lazy {
            events.mapNotNull { (it as? TransformPageEvent)?.page }.distinct()
        }

        fun eventsOf(page: Int): List<TransformPageEvent> {
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

    private fun List<List<TransformPageEvent>>.assertSpaceBetweenPages() {
        forEach {
            it.sortedBy { it.offset }.zipWithNext { a, b ->
                assertThat(b.offset - a.offset, isBetweenInIn(1f - EPSILON, 1f + EPSILON))
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
        filter { lhs ->
            // First, remove all frames that do not occur in the other list
            indexedEvents.any { rhs -> lhs.first == rhs.first }
        }.map { event ->
            // Then, zip the events of the two lists together by frame
            Pair(event.second, indexedEvents.find { it.first == event.first }!!.second)
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
