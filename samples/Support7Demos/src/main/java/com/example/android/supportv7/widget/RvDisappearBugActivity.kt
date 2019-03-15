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

package com.example.android.supportv7.widget

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.example.android.supportv7.R

class RvDisappearBugActivity : Activity() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rv_disappear_bug)

        // Recycler view.
        recyclerView = findViewById(R.id.recycler_view)
        val adapter = ItemAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, HORIZONTAL, false)

        adapter.add(1)
        adapter.add(2)

        val text = findViewById<TextView>(R.id.text)
        val delta = 100
        findViewById<Button>(R.id.button0).setOnClickListener {
            text.layoutParams.apply {
                height += delta
                text.layoutParams = this
            }
        }

        findViewById<Button>(R.id.button1).setOnClickListener {
            text.layoutParams.apply {
                height -= delta
                text.layoutParams = this
            }
        }
    }
}

private class ItemViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
private class ItemAdapter : RecyclerView.Adapter<ItemViewHolder>() {
    private val list = arrayListOf<Int>()

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rv_disappear_bug, parent, false)
        return ItemViewHolder(view as TextView)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        viewHolder.textView.text = "View ${list[position]}"
    }

    fun clear() {
        val previousSize = list.size
        list.clear()
        notifyItemRangeRemoved(0, previousSize)
    }

    fun add(value: Int) {
        val index = list.size
        list.add(value)
        notifyItemInserted(index)
    }

    fun update(position: Int, value: Int) {
        list[position] = value
        notifyItemChanged(position)
    }
}