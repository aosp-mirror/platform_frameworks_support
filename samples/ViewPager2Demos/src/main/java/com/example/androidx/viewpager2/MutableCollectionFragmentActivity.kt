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

package com.example.androidx.viewpager2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

private const val KEY_ID = "KEY"
private const val KEY_COUNT = "COUNT"

/**
 * Shows how to use [FragmentStateAdapter.notifyDataSetChanged] with [ViewPager2]
 */
class MutableCollectionFragmentActivity : MutableCollectionBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            collection = ItemCollection()
        }
        super.onCreate(savedInstanceState)
    }

    override fun getCollection(): ItemCollection = collection

    override fun createViewPagerAdapter(): RecyclerView.Adapter<*> {
        collection.apply {
            return object : FragmentStateAdapter(supportFragmentManager) {
                override fun getItem(position: Int): Fragment = PageFragment.create(items[position])
                override fun getItemCount(): Int = items.size
                override fun getItemId(position: Int): Long = itemToLong(items[position])
                override fun containsItem(itemId: Long): Boolean =
                    items.contains(longToItem(itemId))
            }
        }
    }
}

private lateinit var collection: ItemCollection

class PageFragment : Fragment() {
    private var count: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.item_mutable_collection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        idView.text = arguments?.getString(KEY_ID) ?: throw IllegalStateException()

        count = savedInstanceState?.getInt(KEY_COUNT) ?: 0
        updateCountView()

        buttonView.setOnClickListener { count++; updateCountView() }
    }

    override fun onSaveInstanceState(outState: Bundle) = outState.putInt(KEY_COUNT, count)

    private fun updateCountView() = countView.apply { text = count.toString() }

    private val idView: TextView get() = view!!.findViewById(R.id.text_view_item_id)
    private val countView: TextView get() = view!!.findViewById(R.id.text_view_count)
    private val buttonView: Button get() = view!!.findViewById(R.id.button_count_increase)

    companion object {
        @JvmStatic
        fun create(itemText: String): PageFragment =
            PageFragment().apply {
                arguments = Bundle(1).apply {
                    putString(KEY_ID, itemText)
                }
            }
    }
}
