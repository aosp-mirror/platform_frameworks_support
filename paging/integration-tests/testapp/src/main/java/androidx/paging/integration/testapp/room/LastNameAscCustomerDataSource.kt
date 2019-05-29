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

import android.annotation.SuppressLint
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.room.InvalidationTracker

/**
 * Sample Room keyed data source.
 */
class LastNameAscCustomerDataSource
/**
 * Create a DataSource from the customer table of the given database
 */
@SuppressLint("RestrictedApi")
internal constructor(private val db: SampleDatabase) : ItemKeyedDataSource<String, Customer>() {
    private val customerDao: CustomerDao = db.customerDao
    private val observer: InvalidationTracker.Observer

    init {
        observer = object : InvalidationTracker.Observer("customer") {
            override fun onInvalidated(tables: Set<String>) {
                invalidate()
            }
        }
        db.invalidationTracker.addWeakObserver(observer)
    }

    @SuppressLint("RestrictedApi")
    override fun isInvalid(): Boolean {
        db.invalidationTracker.refreshVersionsSync()
        return super.isInvalid()
    }

    override fun getKey(item: Customer) = getKeyStatic(item)

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<Customer>
    ) {
        val customerName = params.requestedInitialKey
        val list: MutableList<Customer>
        if (customerName != null) {
            // initial keyed load - load before 'customerName',
            // and load after last item in before list
            val pageSize = params.requestedLoadSize / 2
            var key: String = customerName
            list = customerDao.customerNameLoadBefore(key, pageSize).toMutableList()
            list.reverse()
            if (list.isNotEmpty()) {
                key = getKey(list[list.size - 1])
            }
            list.addAll(customerDao.customerNameLoadAfter(key, pageSize))
        } else {
            list = customerDao.customerNameInitial(params.requestedLoadSize).toMutableList()
        }

        if (params.placeholdersEnabled && !list.isEmpty()) {
            val firstKey = getKey(list[0])
            val lastKey = getKey(list[list.size - 1])

            // only bother counting if placeholders are desired
            val position = customerDao.customerNameCountBefore(firstKey)
            val count = position + list.size + customerDao.customerNameCountAfter(lastKey)
            callback.onResult(list, position, count)
        } else {
            callback.onResult(list)
        }
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Customer>) =
        callback.onResult(customerDao.customerNameLoadAfter(params.key, params.requestedLoadSize))

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<Customer>) {
        val list = customerDao.customerNameLoadBefore(params.key, params.requestedLoadSize)
        callback.onResult(list.reversed())
    }

    companion object {
        internal fun factory(db: SampleDatabase): Factory<String, Customer> {
            return object : DataSource.Factory<String, Customer>() {
                override fun create(): DataSource<String, Customer> {
                    return LastNameAscCustomerDataSource(db)
                }
            }
        }

        internal fun getKeyStatic(customer: Customer) = customer.lastName!!
    }
}
