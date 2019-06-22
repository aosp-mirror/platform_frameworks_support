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

package androidx.viewpager2.integration.testapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.util.Random

private const val columns = 4
private val random = Random()

/**
 * Shows Fragment Lifecycle debugging information while performing operations
 */
class FragmentLifecycleActivity : FragmentActivity() {

    @SuppressLint("SyntheticAccessor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_lifecycle)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val console: TextView = findViewById(R.id.console)
        val useDiffUtil: CheckBox = findViewById(R.id.useDiffUtil)

        val operationLog: MutableList<String> = mutableListOf()

        fun <T> getFieldValue(methodName: String, target: Any?): T {
            val m = Fragment::class.java.declaredFields.first { it.name.contains(methodName) }
            m.isAccessible = true
            val r = m.get(target)
            @Suppress("UNCHECKED_CAST")
            return r as T
        }

        fun fragmentDump(): String {
            val fragments = supportFragmentManager.fragments

            fun stateToString(state: Int): String {
                val states =
                    listOf("INITIALIZING", "CREATED", "ACTIVITY_CREATED", "STARTED", "RESUMED")
                return states.first { getFieldValue<Int>(it, null) == state }
            }

            val states = fragments.map { f ->
                val state = getFieldValue<Int>("mState", f)
                val stateString = stateToString(state)
                "${f.tag}:$stateString"
            }

            return "FragmentCount=${fragments.size}\nStates=$states"
        }

        fun itemsDump(): String {
            return items.createIdSnapshot().mapIndexed { ix, id -> "ix=$ix:id=$id" }.toString()
        }

        fun executeWhenIdle(f: () -> Unit) {
            viewPager.postDelayed({
                if (viewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                    f()
                } else {
                    executeWhenIdle(f)
                }
            }, 50L)
        }

        fun updateLog(message: String) {
            executeWhenIdle {
                var lineIx = 0
                val position = viewPager.currentItem
                val expectedResumedFragmentId = items.itemId(position)
                operationLog.add(lineIx++, message)
                operationLog.add(lineIx++, "Current item as per VP2: $position")
                operationLog.add(
                    lineIx++,
                    "Expected resumed fragment id: $expectedResumedFragmentId"
                )
                operationLog.add(lineIx++, itemsDump())
                operationLog.add(lineIx++, fragmentDump())
                operationLog.add(lineIx, "---")
                console.text = operationLog.fold("", { acc, current -> acc + "$current\n" })
            }
        }

        fun goToPage(target: Int, smooth: Boolean) {
            if (target in 0 until items.size && target != viewPager.currentItem) {
                viewPager.post {
                    viewPager.setCurrentItem(target, smooth)
                    updateLog("Went (smooth=$smooth) to page $target")
                }
            }
        }

        fun changeDataSet(message: String, performChanges: () -> Unit) {
            if (useDiffUtil.isChecked) {
                /** using [DiffUtil] */
                val idsOld = items.createIdSnapshot()
                performChanges()
                val idsNew = items.createIdSnapshot()
                DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = idsOld.size
                    override fun getNewListSize(): Int = idsNew.size

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        idsOld[oldItemPosition] == idsNew[newItemPosition]

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        areItemsTheSame(oldItemPosition, newItemPosition)
                }, true).dispatchUpdatesTo(viewPager.adapter!!)
            } else {
                /** without [DiffUtil] */
                performChanges()
                viewPager.adapter!!.notifyDataSetChanged()
            }
            updateLog(message)
        }

        fun currIx(): Int = viewPager.currentItem
        fun nextIx(): Int = viewPager.currentItem + 1
        fun prevIx(): Int = viewPager.currentItem - 1
        fun randIx(): Int = random.nextInt(items.size)

        fun removePage(target: Int) {
            changeDataSet("Removed item at position $target") { items.removeAt(target) }
        }

        fun addPage(target: Int) {
            changeDataSet("Added item at position $target") { items.addNewAt(target) }
        }

        val operations = listOf(
            Operation("Dump info") {
                updateLog("Dumping fragment data")
            },
            Operation("Jump to previous") {
                goToPage(prevIx(), false)
            },
            Operation("Jump to next page") {
                goToPage(nextIx(), false)
            },
            Operation("Jump to random") {
                goToPage(randIx(), false)
            },
            Operation("") {
                // empty button
            },
            Operation("Scroll to previous") {
                goToPage(prevIx(), true)
            },
            Operation("Scroll to next page") {
                goToPage(nextIx(), true)
            },
            Operation("Scroll to random") {
                goToPage(randIx(), true)
            },
            Operation("Remove current") {
                removePage(currIx())
            },
            Operation("Remove previous") {
                removePage(prevIx())
            },
            Operation("Remove next") {
                removePage(nextIx())
            },
            Operation("Remove random") {
                removePage(randIx())
            },
            Operation("Add #0") {
                addPage(0)
            },
            Operation("Add before current") {
                addPage(currIx())
            },
            Operation("Add after current") {
                addPage(nextIx())
            },
            Operation("Add #last") {
                addPage(items.size)
            }
        )

        val items = items // avoids resolving the ViewModel multiple times
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): PageFragment {
                val itemId = items.itemId(position)
                val itemText = items.getItemById(itemId)
                return PageFragment.create(itemText)
            }

            override fun getItemCount(): Int = items.size
            override fun getItemId(position: Int): Long = items.itemId(position)
            override fun containsItem(itemId: Long): Boolean = items.contains(itemId)
        }

        val toolbox: RecyclerView = findViewById(R.id.rvToolbox)
        toolbox.layoutManager = GridLayoutManager(this, columns)
        toolbox.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val button = Button(parent.context).also {
                    it.width = columns + 1
                    it.height = RecyclerView.LayoutParams.WRAP_CONTENT
                    it.setPadding(0, 5, 0, 5)
                }
                return object : RecyclerView.ViewHolder(button) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val button = holder.itemView as Button
                val operation = operations[position]
                button.text = operation.label
                button.setOnClickListener { operation.function() }
            }

            override fun getItemCount(): Int = operations.size
        }
    }

    private val items: ItemsViewModel by viewModels()
}

private data class Operation(val label: String, val function: () -> Unit)
