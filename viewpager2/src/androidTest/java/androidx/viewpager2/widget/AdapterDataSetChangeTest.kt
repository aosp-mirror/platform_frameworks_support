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

import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.widget.AdapterDataSetChangeTest.Action.Insert
import androidx.viewpager2.widget.AdapterDataSetChangeTest.Action.InsertRange
import androidx.viewpager2.widget.AdapterDataSetChangeTest.Action.Move
import androidx.viewpager2.widget.AdapterDataSetChangeTest.Action.Remove
import androidx.viewpager2.widget.AdapterDataSetChangeTest.Action.RemoveRange
import androidx.viewpager2.widget.AdapterDataSetChangeTest.TestConfig
import androidx.viewpager2.widget.swipe.ViewAdapter
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(Parameterized::class)
@LargeTest
class AdapterDataSetChangeTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        val startAt: Int = 0,
        val actions: List<Action>,
        val expectedFinalCurrentItem: Int,
        val expectedFinalPageText: String
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    private val adapter = ModifiableViewAdapter(MutableList(pageCount) { it.toString() })
    private lateinit var test: Context

    override fun setUp() {
        super.setUp()
        test = setUpTest(ViewPager2.ORIENTATION_HORIZONTAL)
    }

    @Test
    fun test_modifyDataSet() {
        test.setAdapterSync { adapter }
        test.viewPager.setCurrentItemSync(config.startAt, false, 2, SECONDS)
        test.assertBasicState(config.startAt)

        val layoutChangedLatch = test.viewPager.addWaitForLayoutChangeLatch()
        activityTestRule.runOnUiThread {
            config.actions.forEach { it.perform(adapter) }
        }
        layoutChangedLatch.await(1, SECONDS)
        PollingCheck.waitFor { !test.viewPager.recyclerView.isAnimating }

        assertThat(
            "Not displaying index ${config.expectedFinalCurrentItem}",
            test.viewPager.currentCompletelyVisibleItem,
            equalTo(config.expectedFinalCurrentItem)
        )
        test.assertBasicState(config.expectedFinalCurrentItem, config.expectedFinalPageText)
    }

    sealed class Action(private val action: (ModifiableViewAdapter) -> Unit) {
        fun perform(adapter: ModifiableViewAdapter) {
            action(adapter)
        }

        data class Insert(val at: Int, val item: String) : Action({
            it.insert(at, item)
        })
        data class InsertRange(val at: Int, val items: List<String>) : Action({
            it.insertRange(at, items)
        })
        data class Move(val from: Int, val to: Int) : Action({
            it.move(from, to)
        })
        data class Remove(val at: Int) : Action({
            it.remove(at)
        })
        data class RemoveRange(val at: Int, val count: Int) : Action({
            it.removeRange(at, count)
        })
    }

    class ModifiableViewAdapter(private val dataSet: MutableList<String>) : ViewAdapter(dataSet) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            return dataSet[position].toLong()
        }

        fun insert(index: Int, item: String) {
            dataSet.add(index, item)
            notifyItemInserted(index)
        }

        fun insertRange(index: Int, items: Collection<String>) {
            dataSet.addAll(index, items)
            notifyItemRangeInserted(index, items.size)
        }

        fun move(fromIndex: Int, toIndex: Int) {
            val item = dataSet.removeAt(fromIndex)
            dataSet.add(toIndex, item)
            notifyItemMoved(fromIndex, toIndex)
        }

        fun remove(index: Int) {
            dataSet.removeAt(index)
            notifyItemRemoved(index)
        }

        fun removeRange(index: Int, itemCount: Int) {
            repeat(itemCount) {
                dataSet.removeAt(index)
            }
            notifyItemRangeRemoved(index, itemCount)
        }
    }
}

private const val pageCount = 10
private const val lastPage = pageCount - 1

private fun createTestSet(): List<TestConfig> {
    return listOf(
        // Single action, looking at first page
        TestConfig(
            actions = listOf(Insert(0, "-1")),
            expectedFinalCurrentItem = 1,
            expectedFinalPageText = "0"
        ),
        TestConfig(
            actions = listOf(Remove(0)),
            expectedFinalCurrentItem = 0,
            expectedFinalPageText = "1"
        ),
        TestConfig(
            actions = listOf(Move(0, 1)),
            expectedFinalCurrentItem = 1,
            expectedFinalPageText = "0"
        ),
        TestConfig(
            actions = listOf(Move(lastPage, 0)),
            expectedFinalCurrentItem = 1,
            expectedFinalPageText = "0"
        ),

        // Single action, looking at last page
        TestConfig(
            startAt = lastPage,
            actions = listOf(Insert(0, "-1")),
            expectedFinalCurrentItem = lastPage + 1,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(Insert(lastPage, "-1")),
            expectedFinalCurrentItem = lastPage + 1,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(Remove(0)),
            expectedFinalCurrentItem = lastPage - 1,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(Remove(lastPage)),
            expectedFinalCurrentItem = lastPage - 1,
            expectedFinalPageText = "${lastPage - 1}"
        ),

        // Single range action, looking at first page
        TestConfig(
            actions = listOf(InsertRange(0, listOf("-1", "-2"))),
            expectedFinalCurrentItem = 2,
            expectedFinalPageText = "0"
        ),
        TestConfig(
            actions = listOf(RemoveRange(0, 2)),
            expectedFinalCurrentItem = 0,
            expectedFinalPageText = "2"
        ),

        // Single range action, looking at last page
        TestConfig(
            startAt = lastPage,
            actions = listOf(InsertRange(0, listOf("-1", "-2"))),
            expectedFinalCurrentItem = lastPage + 2,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(InsertRange(lastPage - 1, listOf("-1", "-2"))),
            expectedFinalCurrentItem = lastPage + 2,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(InsertRange(lastPage, listOf("-1", "-2"))),
            expectedFinalCurrentItem = lastPage + 2,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(RemoveRange(0, 2)),
            expectedFinalCurrentItem = lastPage - 2,
            expectedFinalPageText = "$lastPage"
        ),
        TestConfig(
            startAt = lastPage,
            actions = listOf(RemoveRange(lastPage - 1, 2)),
            expectedFinalCurrentItem = lastPage - 2,
            expectedFinalPageText = "${lastPage - 2}"
        ),

        // "Trivial" cases from random tests
        TestConfig(
            startAt = 1,
            actions = listOf(
                Remove(at = 0),
                Remove(at = 5),
                Remove(at = 1),
                Insert(at = 2, item = "-1"),
                Insert(at = 6, item = "-2"),
                Insert(at = 7, item = "-3"),
                Move(from = 5, to = 2),
                Insert(at = 9, item = "-4"),
                Insert(at = 1, item = "-5"),
                Move(from = 4, to = 9)
            ),
            expectedFinalCurrentItem = 0,
            expectedFinalPageText = "1"
        ),
        TestConfig(
            startAt = 6,
            actions = listOf(
                Remove(at = 1),
                Remove(at = 2),
                Insert(at = 0, item = "-1"),
                Move(from = 2, to = 7),
                Move(from = 8, to = 6),
                Remove(at = 1),
                Move(from = 1, to = 0),
                Insert(at = 3, item = "-2"),
                Move(from = 3, to = 3),
                Remove(at = 3)
            ),
            expectedFinalCurrentItem = 3,
            expectedFinalPageText = "ignored"
        ),

        // "Non-trivial" cases from random tests
        TestConfig(
            startAt = 2,
            actions = listOf(
                Insert(at = 0, item = "-1"),
                Insert(at = 9, item = "-2"),
                Remove(at = 6),
                Move(from = 0, to = 6),
                Remove(at = 9),
                Remove(at = 1),
                Remove(at = 8),
                Insert(at = 0, item = "-3"),
                Move(from = 3, to = 8),
                Remove(at = 2)
            ),
            expectedFinalCurrentItem = 1,
            expectedFinalPageText = "ignored"
        )
    )
}
