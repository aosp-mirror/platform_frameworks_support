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

package androidx.paging.integration.testapp.room

import android.os.Bundle
import android.widget.Button

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Sample PagedList activity which uses Room.
 */
open class RoomPagedListActivity : AppCompatActivity() {
    companion object {
        private const val STRING_KEY = "STRING_KEY"
        private const val INT_KEY = "INT_KEY"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PagedListCustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_recycler_view)
        val viewModel = ViewModelProviders.of(this).get(CustomerViewModel::class.java)

        recyclerView = findViewById(R.id.recyclerview)
        adapter = PagedListCustomerAdapter()
        recyclerView.adapter = adapter

        val livePagedList: LiveData<PagedList<Customer>>
        if (useKeyedQuery()) {
            var key: String? = null
            if (savedInstanceState != null) {
                key = savedInstanceState.getString(STRING_KEY)
                adapter.setScrollToKey(key!!)
            }
            livePagedList = viewModel.getLivePagedList(key!!)
        } else {
            var position = 0
            if (savedInstanceState != null) {
                position = savedInstanceState.getInt(INT_KEY)
                adapter.setScrollToPosition(position)
            }
            livePagedList = viewModel.getLivePagedList(position)
        }
        livePagedList.observe(this, Observer { items -> adapter.submitList(items) })

        val addButton = findViewById<Button>(R.id.addButton)
        addButton.setOnClickListener { viewModel.insertCustomer() }

        val clearButton = findViewById<Button>(R.id.clearButton)
        clearButton.setOnClickListener { viewModel.clearAllCustomers() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val list = adapter.currentList ?: return // Can't find anything to restore
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
        val targetPosition = layoutManager!!.findFirstVisibleItemPosition()

        if (useKeyedQuery()) {
            val customer = list[targetPosition]
            if (customer != null) {
                val key = LastNameAscCustomerDataSource.getKeyStatic(customer)
                outState.putString(STRING_KEY, key)
            }
        } else {
            // NOTE: in the general case, we can't just rely on RecyclerView/LinearLayoutManager to
            // preserve position, because of position offset which is present when using an
            // uncounted, non-keyed source).
            val absolutePosition = targetPosition + list.getPositionOffset()
            outState.putInt(INT_KEY, absolutePosition)
        }
    }

    protected open fun useKeyedQuery() = false
}
