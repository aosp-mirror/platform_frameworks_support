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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.item_mutable_collection.buttonCountIncrease
import kotlinx.android.synthetic.main.item_mutable_collection.textViewCount
import kotlinx.android.synthetic.main.item_mutable_collection.textViewItemId

private const val KEY_ITEM_TEXT = "com.example.androidx.viewpager2.KEY_ITEM_TEXT"
private const val KEY_CLICK_COUNT = "com.example.androidx.viewpager2.KEY_CLICK_COUNT"

/**
 * Shows how to use [FragmentStateAdapter.notifyDataSetChanged] with [ViewPager2]
 */
class MutableCollectionFragmentActivity : MutableCollectionBaseActivity() {
    override fun createViewPagerAdapter(): RecyclerView.Adapter<*> {
        val items = items // avoids resolving the ViewModel multiple times
        return object : FragmentStateAdapter(supportFragmentManager) {
            override fun getItem(position: Int) = PageFragment.create(items[items.itemId(position)])
            override fun getItemCount(): Int = items.size
            override fun getItemId(position: Int): Long = items.itemId(position)
            override fun containsItem(itemId: Long): Boolean = items.contains(itemId)
        }
    }
}

class PageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.item_mutable_collection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val itemText = arguments?.getString(KEY_ITEM_TEXT) ?: throw IllegalStateException()
        textViewItemId.text = itemText

        fun updateCountView(count: Int) {
            textViewCount.text = "$count"
        }
        updateCountView(savedInstanceState?.getInt(KEY_CLICK_COUNT) ?: 0)

        buttonCountIncrease.setOnClickListener {
            updateCountView(clickCount() + 1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_CLICK_COUNT, clickCount())
    }

    private fun clickCount(): Int {
        return "${textViewCount.text}".toInt()
    }

    companion object {
        fun create(itemText: String) =
            PageFragment().apply {
                arguments = Bundle(1).apply {
                    putString(KEY_ITEM_TEXT, itemText)
                }
            }
    }
}
