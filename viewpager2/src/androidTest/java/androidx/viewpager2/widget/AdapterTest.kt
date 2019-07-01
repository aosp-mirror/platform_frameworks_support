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

import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.widget.AdapterTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.AdapterTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.AdapterTest.Event.OnPageSelectedEvent
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@LargeTest
@RunWith(AndroidJUnit4::class)
class AdapterTest : BaseTest() {
    private val pageCount = 5

    private lateinit var test: Context
    private lateinit var dataSet: MutableList<String>
    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    override fun setUp() {
        super.setUp()
        test = setUpTest(ViewPager2.ORIENTATION_HORIZONTAL)
    }

    @Test
    fun test_setAdapter() {
        test.setAdapterSync(viewAdapterProvider(stringSequence(5)))
        test.assertBasicState(0)
        test.viewPager.setCurrentItemSync(1, false, 2, SECONDS)
        test.assertBasicState(1)
        activityTestRule.runOnUiThread {
            test.viewPager.adapter = test.viewPager.adapter
        }
        test.assertBasicState(0)
    }

    @Test
    fun test_removeAllLookingAt0() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount)
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            eventsForPage(0)            // for setting the adapter
                .plus(eventsForPage(0)) // for clearing it
        ))
    }

    @Test
    fun test_removeAllLookingAt1() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount, initialPage = 1)
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            eventsForPage(0)            // for setting the adapter
                .plus(eventsForPage(1)) // for going to page 1
                .plus(eventsForPage(0)) // for clearing it
        ))
    }

    @Test
    fun test_addItemsWhileEmpty() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(0)
        fillDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            eventsForPage(0) // for populating the adapter
        ))
    }

    @Test
    fun test_removeAllAddAllRemoveAgain() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount)

        clearDataSet()
        fillDataSet()
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            eventsForPage(0)            // for setting the adapter
                .plus(eventsForPage(0)) // for clearing it
                .plus(eventsForPage(0)) // for repopulating it
                .plus(eventsForPage(0)) // for clearing it again
        ))
    }

    private fun eventsForPage(page: Int): List<Event> {
        return listOf(
            OnPageSelectedEvent(page),
            OnPageScrolledEvent(page, 0f, 0)
        )
    }

    private fun setUpAdapterSync(pageCount: Int, initialPage: Int? = null) {
        dataSet = MutableList(pageCount) { "$it" }
        test.setAdapterSync(viewAdapterProvider(dataSet))
        adapter = test.viewPager.adapter!!

        if (initialPage != null) {
            test.viewPager.setCurrentItemSync(initialPage, false, 2, SECONDS)
        }
    }

    private fun clearDataSet() {
        modifyDataSet {
            dataSet.clear()
            adapter.notifyItemRangeRemoved(0, pageCount)
        }
    }

    private fun fillDataSet() {
        modifyDataSet {
            dataSet.addAll(List(pageCount) { "$it" })
            adapter.notifyItemRangeInserted(0, pageCount)
        }
    }

    private fun modifyDataSet(block: () -> Unit) {
        val layoutChangedLatch = test.viewPager.addWaitForLayoutChangeLatch()
        activityTestRule.runOnUiThread {
            block()
        }
        layoutChangedLatch.await(1, SECONDS)

        // Let animations run
        val animationLatch = CountDownLatch(1)
        test.viewPager.recyclerView.itemAnimator!!.isRunning {
            animationLatch.countDown()
        }
        animationLatch.await(1, SECONDS)

        // Wait until VP2 has stabilized
        val adapter = test.viewPager.adapter
        if (adapter != null && adapter.itemCount > 0) {
            PollingCheck.waitFor(1000) { test.viewPager.currentCompletelyVisibleItem != -1 }
        }
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

        val allEvents get() = events.toList()

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
}