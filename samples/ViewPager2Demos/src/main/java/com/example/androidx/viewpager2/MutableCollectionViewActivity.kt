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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * Shows how to use [RecyclerView.Adapter.notifyDataSetChanged] with [ViewPager2]
 */
class MutableCollectionViewActivity : MutableCollectionBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            collection = ItemCollection()
            itemToCount = emptyMap<String, Int>().toMutableMap()
        }
        super.onCreate(savedInstanceState)
    }

    override fun getCollection(): ItemCollection = collection

    override fun createViewPagerAdapter(): RecyclerView.Adapter<*> {
        collection.apply {
            return object : RecyclerView.Adapter<PageView>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    PageView(parent)

                override fun onBindViewHolder(holder: PageView, position: Int) =
                    holder.onBind(items[position])

                override fun getItemCount(): Int = items.size
                override fun getItemId(position: Int): Long = itemToLong(items[position])
            }
        }
    }
}

private lateinit var collection: ItemCollection
private lateinit var itemToCount: MutableMap<String, Int>

class PageView(parent: ViewGroup) :
    RecyclerView.ViewHolder(
        View.inflate(parent.context, R.layout.item_mutable_collection, null).apply {
            layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }) {

    fun onBind(item: String) {
        idView.text = item
        updateCountView()
        buttonView.setOnClickListener {
            val currentCount = itemToCount[idView.text as String] ?: 0
            itemToCount[idView.text as String] = currentCount + 1
            updateCountView()
        }
    }

    private fun updateCountView() =
        countView.apply { text = (itemToCount[idView.text as String] ?: 0).toString() }

    private val idView: TextView get() = itemView.findViewById(R.id.text_view_item_id)
    private val countView: TextView get() = itemView.findViewById(R.id.text_view_count)
    private val buttonView: Button get() = itemView.findViewById(R.id.button_count_increase)
}
