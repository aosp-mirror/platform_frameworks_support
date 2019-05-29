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
import androidx.lifecycle.ViewModelProviders
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable

/**
 * Sample `Flowable<PagedList>` activity which uses Room.
 */
class RoomPagedListRxActivity : AppCompatActivity() {
    private val disposable = CompositeDisposable()
    private lateinit var adapter: PagedListCustomerAdapter
    private lateinit var viewModel: CustomerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)
        viewModel = ViewModelProviders.of(this).get(CustomerViewModel::class.java)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        adapter = PagedListCustomerAdapter()
        recyclerView.adapter = adapter

        val addButton = findViewById<Button>(R.id.addButton)
        addButton.setOnClickListener { viewModel.insertCustomer() }

        val clearButton = findViewById<Button>(R.id.clearButton)
        clearButton.setOnClickListener { viewModel.clearAllCustomers() }
    }

    override fun onStart() {
        super.onStart()
        disposable.add(viewModel.pagedListFlowable.subscribe { adapter.submitList(it) })
    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }
}
