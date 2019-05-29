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

package androidx.paging.integration.testapp.custom

import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder

/**
 * Sample ViewModel backed by an artificial data source
 */
class PagedListItemViewModel : ViewModel() {
    internal val dataSourceLock = Any()
    internal var dataSource: ItemDataSource? = null

    private val factory = object : DataSource.Factory<Int, Item>() {
        override fun create(): DataSource<Int, Item> {
            val newDataSource = ItemDataSource()
            synchronized(dataSourceLock) {
                dataSource = newDataSource
                return newDataSource
            }
        }
    }

    internal val livePagedList = LivePagedListBuilder(factory, 10).build()

    internal fun invalidateList() {
        synchronized(dataSourceLock) {
            if (dataSource != null) {
                dataSource!!.invalidate()
            }
        }
    }
}
