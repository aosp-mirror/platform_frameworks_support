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
import android.app.Application
import androidx.annotation.WorkerThread
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import androidx.room.Room
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.util.UUID

/**
 * Sample database-backed view model of Customers
 */
class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var database: SampleDatabase
    private lateinit var liveCustomerList: LiveData<PagedList<Customer>>

    internal val pagedListFlowable: Flowable<PagedList<Customer>>
        get() = getPagedListFlowable(database.customerDao.loadPagedAgeOrder())

    init {
        createDb()
    }

    @SuppressLint("RestrictedApi")
    private fun createDb() {
        database = Room.databaseBuilder(
            this.getApplication(),
            SampleDatabase::class.java, "customerDatabase"
        ).build()

        ArchTaskExecutor.getInstance().executeOnDiskIO {
            // fill with some simple data
            val customerCount = database.customerDao.countCustomers()
            if (customerCount == 0) {
                val initialCustomers = List(10) { createCustomer() }.toTypedArray()
                database.customerDao.insertAll(initialCustomers)
            }
        }
    }

    @WorkerThread
    private fun createCustomer(): Customer {
        val customer = Customer()
        customer.name = UUID.randomUUID().toString()
        customer.lastName = UUID.randomUUID().toString()
        return customer
    }

    @SuppressLint("RestrictedApi")
    internal fun insertCustomer() {
        ArchTaskExecutor.getInstance()
            .executeOnDiskIO { database.customerDao.insert(createCustomer()) }
    }

    @SuppressLint("RestrictedApi")
    internal fun clearAllCustomers() {
        ArchTaskExecutor.getInstance().executeOnDiskIO { database.customerDao.removeAll() }
    }

    private fun <K> getLivePagedList(
        initialLoadKey: K,
        dataSourceFactory: DataSource.Factory<K, Customer>
    ): LiveData<PagedList<Customer>> {
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(false)
            .build()
        return LivePagedListBuilder(dataSourceFactory, config)
            .setInitialLoadKey(initialLoadKey)
            .build()
    }

    internal fun <K> getPagedListFlowable(
        dataSourceFactory: DataSource.Factory<K, Customer>
    ): Flowable<PagedList<Customer>> {
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(false)
            .build()
        return RxPagedListBuilder(dataSourceFactory, config)
            .buildFlowable(BackpressureStrategy.LATEST)
    }

    internal fun getLivePagedList(position: Int): LiveData<PagedList<Customer>> {
        if (!::liveCustomerList.isInitialized) {
            liveCustomerList = getLivePagedList(position, database.customerDao.loadPagedAgeOrder())
        }
        return liveCustomerList
    }

    internal fun getLivePagedList(key: String): LiveData<PagedList<Customer>> {
        if (!::liveCustomerList.isInitialized) {
            liveCustomerList =
                getLivePagedList(key, LastNameAscCustomerDataSource.factory(database))
        }
        return liveCustomerList
    }
}
