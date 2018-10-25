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

package androidx.paging.integration.testapp.twolayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.ViewModelsWithStateFactories
import androidx.lifecycle.get
import androidx.paging.PagedList
import androidx.paging.PagedList.LoadState.IDLE
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

/**
 * Sample PagedList activity with artificial data source.
 */
class TwoLayerActivity : AppCompatActivity() {

    @SuppressLint("SyntheticAccessor") // ???
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)
        val viewModel = ViewModelProvider(this, ViewModelsWithStateFactories.of(this)).get<ItemViewModel>()

        val adapter = ItemAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = adapter
        viewModel.liveData.observe(this, Observer(adapter::submitList))

        val buttonStart = findViewById<Button>(R.id.button_start)
        val buttonRefresh = findViewById<Button>(R.id.button_refresh)
        val buttonEnd = findViewById<Button>(R.id.button_end)


        // instead, could hook these signals up to adapter / swipeRefresh
        adapter.addLoadStateListener { type, state, error ->
            println("$type $state")
            val button = when (type) {
                PagedList.LoadType.REFRESH -> buttonRefresh
                PagedList.LoadType.START -> buttonStart
                PagedList.LoadType.END -> buttonEnd
            }
            when (state) {
                PagedList.LoadState.IDLE -> {
                    button.text = "Idle"
                    button.isEnabled = false
                }
                PagedList.LoadState.LOADING -> {
                    button.text = "Loading"
                    button.isEnabled = false
                }
                PagedList.LoadState.DONE -> {
                    button.text = "Done"
                    button.isEnabled = false
                }
                PagedList.LoadState.ERROR -> {
                    button.text = "Error"
                    button.isEnabled = false
                }
                PagedList.LoadState.RETRYABLE_ERROR -> {
                    button.text = "Error"
                    button.isEnabled = true
                }

            }
        }
    }
}
