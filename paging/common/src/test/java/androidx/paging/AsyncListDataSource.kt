/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.paging

import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture

class AsyncListDataSource<T>(list: List<T>)
    : ListenablePositionalSource<T>() {
    override fun loadInitial(params: PositionalDataSource.LoadInitialParams): ListenableFuture<InitialResult<T>> {
        val future: ResolvableFuture<InitialResult<T>> = ResolvableFuture.create()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams): ListenableFuture<RangeResult<T>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private val workItems: MutableList<() -> Unit> = ArrayList()
    private val listDataSource = ListDataSource(list)

    /*
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        workItems.add {
            listDataSource.loadInitial(params, callback)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        workItems.add {
            listDataSource.loadRange(params, callback)
        }
    }
    */

    fun flush() {
        workItems.map { it() }
        workItems.clear()
    }
}
