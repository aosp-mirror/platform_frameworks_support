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

package com.example.androidx.viewpager2

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.atomic.AtomicLong

/**
 * Shows how to use notifyDataSetChanged with [ViewPager2]
 */
abstract class MutableCollectionBaseActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getCollection().apply {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_mutables)

            viewPager.adapter = createViewPagerAdapter()

            spinner.adapter = object : BaseAdapter() {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
                    ((convertView as TextView?) ?: TextView(parent?.context)).apply {
                        text = getItem(position)
                    }

                override fun getItem(position: Int): String = items[position]
                override fun getItemId(position: Int): Long = viewPager.adapter.getItemId(position)
                override fun getCount(): Int = viewPager.adapter.itemCount
            }

            buttonGoTo.setOnClickListener {
                viewPager.setCurrentItem(spinner.selectedItemPosition, true)
            }

            buttonRemove.setOnClickListener {
                items.removeAt(spinner.selectedItemPosition)
                notifyDataSetChanged()
            }

            buttonAddBefore.setOnClickListener {
                items.add(spinner.selectedItemPosition, longToItem(nextValue.getAndIncrement()))
                notifyDataSetChanged()
            }

            buttonAddAfter.setOnClickListener {
                items.add(spinner.selectedItemPosition + 1, longToItem(nextValue.getAndIncrement()))
                notifyDataSetChanged()
            }
        }
    }

    abstract fun createViewPagerAdapter(): RecyclerView.Adapter<*>

    abstract fun getCollection(): ItemCollection

    private fun notifyDataSetChanged() {
        viewPager.adapter.notifyDataSetChanged()
        (spinner.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private val viewPager: ViewPager2 get() = findViewById(R.id.view_pager)
    private val spinner: Spinner get() = findViewById(R.id.item_spinner)
    private val buttonGoTo: Button get() = findViewById(R.id.button_goto)
    private val buttonRemove: Button get() = findViewById(R.id.button_remove)
    private val buttonAddBefore: Button get() = findViewById(R.id.button_add_before)
    private val buttonAddAfter: Button get() = findViewById(R.id.button_add_after)
}

class ItemCollection {
    val nextValue = AtomicLong(1)
    val items = (1..9).map { longToItem(nextValue.getAndIncrement()) }.toMutableList()
    fun longToItem(value: Long): String = "item#$value"
    fun itemToLong(value: String): Long = value.split("#")[1].toLong()
}
