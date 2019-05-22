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

import androidx.arch.core.util.Function

import java.util.IdentityHashMap

internal class WrapperItemKeyedDataSource<K : Any, A : Any, B : Any>(
    private val mSource: ItemKeyedDataSource<K, A>,
    @JvmField internal val mListFunction: Function<List<A>, List<B>>
) : ItemKeyedDataSource<K, B>() {

    private val mKeyMap = IdentityHashMap<B, K>()

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        mSource.addInvalidatedCallback(onInvalidatedCallback)
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        mSource.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun invalidate() {
        mSource.invalidate()
    }

    override fun isInvalid() = mSource.isInvalid

    fun convertWithStashedKeys(source: List<A>): List<B> {
        val dest = convert(mListFunction, source)
        synchronized(mKeyMap) {
            // synchronize on mKeyMap, since multiple loads may occur simultaneously.
            // Note: manually sync avoids locking per-item (e.g. Collections.synchronizedMap)
            for (i in dest.indices) {
                mKeyMap[dest[i]] = mSource.getKey(source[i])
            }
        }
        return dest
    }

    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<B>) {
        mSource.loadInitial(params, object : ItemKeyedDataSource.LoadInitialCallback<A>() {
            override fun onResult(data: List<A>, position: Int, totalCount: Int) {
                callback.onResult(convertWithStashedKeys(data), position, totalCount)
            }

            override fun onResult(data: List<A>) {
                callback.onResult(convertWithStashedKeys(data))
            }

            override fun onError(error: Throwable) {
                callback.onError(error)
            }
        })
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<B>) {
        mSource.loadAfter(params, object : ItemKeyedDataSource.LoadCallback<A>() {
            override fun onResult(data: List<A>) {
                callback.onResult(convertWithStashedKeys(data))
            }

            override fun onError(error: Throwable) {
                callback.onError(error)
            }
        })
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<B>) {
        mSource.loadBefore(params, object : ItemKeyedDataSource.LoadCallback<A>() {
            override fun onResult(data: List<A>) {
                callback.onResult(convertWithStashedKeys(data))
            }

            override fun onError(error: Throwable) {
                callback.onError(error)
            }
        })
    }

    override fun getKey(item: B): K = synchronized(mKeyMap) {
        return mKeyMap[item]!!
    }
}
