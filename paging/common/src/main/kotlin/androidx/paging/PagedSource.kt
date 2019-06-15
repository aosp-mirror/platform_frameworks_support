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

import androidx.paging.PagedSource.KeyProvider.ItemKey
import androidx.paging.PagedSource.KeyProvider.PageKey
import androidx.paging.PagedSource.KeyProvider.Positional
import com.google.common.util.concurrent.ListenableFuture

abstract class PagedSource<Key : Any, Value : Any> {
    enum class LoadType {
        INITIAL, START, END
    }

    /**
     * Builder will be provided for Java (also consider @JvmOverloads)
     */
    data class LoadParams<Key>(
        /**
         * Type, for different behavior, e.g. only count initial load
         */
        val loadType: LoadType,
        /**
         * Key for the page to be loaded
         */
        val key: Key?,
        /**
         * Number of items to load
         */
        val loadSize: Int,
        /**
         * Whether placeholders are enabled - if false, can skip counting
         */
        val placeholdersEnabled: Boolean,
        val pageSize: Int
    )

    /**
     * Builder will be provided for Java (also consider @JvmOverloads)
     */
    data class LoadResult<Key, Value>(
        /**
         * Optional count of items before the loaded data.
         */
        val itemsBefore: Int = COUNT_UNDEFINED,
        /**
         * Optional count of items after the loaded data.
         */
        val itemsAfter: Int = COUNT_UNDEFINED,
        /**
         * Key for next page - ignored unless you're using KeyProvider.PageKey
         */
        val nextKey: Key? = null,
        /**
         * Key for previous page - ignored unless you're using KeyProvider.PageKey
         */
        val prevKey: Key? = null,
        /**
         * Loaded data
         */
        val data: List<Value>
    )

    /**
     * Used to define how pages are indexed, one of:
     * * [Positional] (Useful for jumping positions, and parallelizable loads)
     * * [PageKey] (Standard for network pagination)
     * * [ItemKey] (Ideal for DB pagination, granular continuation after invalidate)
     */
    sealed class KeyProvider<Key : Any, Value : Any> {
        class Positional<Value : Any> : KeyProvider<Int, Value>()
        class PageKey<Key : Any, Value : Any> : KeyProvider<Key, Value>()
        abstract class ItemKey<Key : Any, Value : Any> : KeyProvider<Key, Value>() {
            abstract fun getKey(item: Value): Key
        }
    }

    abstract val keyProvider: KeyProvider<Key, Value>

    abstract val invalid: Boolean

    /**
     * Signal the [PagedSource] to stop loading, and notify its callback.
     *
     * If invalidate has already been called, this method does nothing.
     */
    abstract fun invalidate()

    /**
     * Loading API for [DataSource].
     *
     * Implement this method to trigger your async load (e.g. from database or network).
     */
    abstract fun load(params: LoadParams<Key>): ListenableFuture<LoadResult<Key, Value>>

    /**
     * Return false if the observed error should never be retried.
     */
    abstract fun isRetryableError(error: Throwable): Boolean

    companion object {
        const val COUNT_UNDEFINED = -1
    }
}
