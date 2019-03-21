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

abstract class RxPageKeyedDataSource<Key, Value> : PageKeyedDataSource<Key, Value>() {
    data class LoadInitialResponse<Key, Value>(
        val result: List<Value>,
        val position: Int,
        val previousPageKey: Key?,
        val nextPageKey: Key?
    )

    data class LoadBeforeResponse<Key, Value>(
        val data: List<Value>,
        val adjacentPageKey: Key?
    )

    data class LoadAfterResponse<Key, Value>(
        val data: List<Value>,
        val adjacentPageKey: Key?
    )

    override fun loadInitial(
        params: LoadInitialParams<Key>,
        callback: LoadInitialCallback<Key, Value>
    ) {
        loadInitial(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<LoadInitialResponse<Key, Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(response: LoadInitialResponse<Key, Value>) {
                    callback.onResult(
                        response.result,
                        response.position,
                        response.result.size,
                        response.previousPageKey,
                        response.nextPageKey
                    )
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Key, Value>) {
        loadBefore(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<LoadBeforeResponse<Key, Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(response: LoadBeforeResponse<Key, Value>) {
                    callback.onResult(response.data, response.adjacentPageKey)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Key, Value>) {
        loadAfter(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<LoadAfterResponse<Key, Value>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(response: LoadAfterResponse<Key, Value>) {
                    callback.onResult(response.data, response.adjacentPageKey)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    abstract fun loadInitial(
        params: LoadInitialParams<Key>
    ): Single<LoadInitialResponse<Key, Value>>

    abstract fun loadBefore(params: LoadParams<Key>): Single<LoadBeforeResponse<Key, Value>>

    abstract fun loadAfter(params: LoadParams<Key>): Single<LoadAfterResponse<Key, Value>>
}