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

abstract class RxPositionalDataSource<T> : PositionalDataSource<T>() {
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        loadInitial(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<List<T>> {
                override fun onSubscribe(d: Disposable?) {}

                override fun onSuccess(result: List<T>) {
                    callback.onResult(result, params.requestedStartPosition, result.size)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        loadRange(params)
            .subscribeOn(Schedulers.from(executor))
            .subscribe(object : SingleObserver<List<T>> {
                override fun onSubscribe(d: Disposable?) {}

                override fun onSuccess(result: List<T>) {
                    callback.onResult(result)
                }

                override fun onError(e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    abstract fun loadInitial(params: LoadInitialParams): Single<List<T>>

    abstract fun loadRange(params: LoadRangeParams): Single<List<T>>
}
