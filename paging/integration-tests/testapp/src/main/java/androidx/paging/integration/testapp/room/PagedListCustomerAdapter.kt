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

import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

/**
 * Sample adapter which uses a AsyncPagedListDiffer.
 */
internal class PagedListCustomerAdapter :
    PagedListAdapter<Customer, RecyclerView.ViewHolder>(Customer.DIFF_CALLBACK) {
    private var recyclerView: RecyclerView? = null
    private var setObserved: Boolean = false
    private var scrollToPosition = -1
    private var scrollToKey: String? = null

    fun setScrollToPosition(position: Int) {
        scrollToPosition = position
    }

    fun setScrollToKey(key: String) {
        scrollToKey = key
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = object : RecyclerView.ViewHolder(TextView(parent.context)) {}
        holder.itemView.minimumHeight = 400
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val customer = getItem(position)

        if (customer != null) {
            (holder.itemView as TextView).text = "${customer.id} ${customer.lastName}"
        } else {
            (holder.itemView as TextView).setText(R.string.loading)
        }
    }

    private fun findKeyInPagedList(key: String, list: PagedList<Customer>): Int {
        for (i in list.indices) {
            val customer = list[i]
            if (customer != null && LastNameAscCustomerDataSource.getKeyStatic(customer) == key) {
                return i
            }
        }
        return 0 // couldn't find, fall back to 0 - could alternately search with comparator
    }

    override fun submitList(pagedList: PagedList<Customer>?) {
        super.submitList(pagedList)

        if (pagedList != null) {
            val firstSet = !setObserved
            setObserved = true

            if (firstSet && recyclerView != null && (scrollToPosition >= 0 || scrollToKey != null)
            ) {
                val localScrollToPosition: Int
                if (scrollToKey != null) {
                    localScrollToPosition = findKeyInPagedList(scrollToKey!!, pagedList)
                    scrollToKey = null
                } else {
                    // if there's 20 items unloaded items (without placeholders holding the spots)
                    // at the beginning of list, we subtract 20 from saved position
                    localScrollToPosition = scrollToPosition - pagedList.getPositionOffset()
                }
                recyclerView!!.scrollToPosition(localScrollToPosition)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }
}
