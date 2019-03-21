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

package androidx.paging

import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class RxItemKeyedDataSource<Key, Value> : ItemKeyedDataSource<Key, Value>() {
    data class LoadInitialResponse<Value>(
        val data: List<Value>,
        val position: Int
    )

    override fun loadInitial(params: LoadInitialParams<Key>, callback: LoadInitialCallback<Value>) {
        loadInitial(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<LoadInitialResponse<Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(response: LoadInitialResponse<Value>) {
                    callback.onResult(response.data, response.position, response.data.size)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Value>) {
        loadAfter(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<List<Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(data: List<Value>) {
                    callback.onResult(data)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Value>) {
        loadBefore(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<List<Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(data: List<Value>) {
                    callback.onResult(data)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    abstract fun loadInitial(params: LoadInitialParams<Key>): Single<LoadInitialResponse<Value>>

    abstract fun loadAfter(params: LoadParams<Key>): Single<List<Value>>

    abstract fun loadBefore(params: LoadParams<Key>): Single<List<Value>>
}