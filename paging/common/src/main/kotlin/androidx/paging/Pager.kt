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

import androidx.paging.futures.FutureCallback
import androidx.paging.futures.Futures

import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class Pager<K, V>(
    val config: PagedList.Config/* synthetic access */,
    val source: DataSource<K, V>/* synthetic access */,
    val mNotifyExecutor: Executor/* synthetic access */,
    private val mFetchExecutor: Executor,
    val mPageConsumer: PageConsumer<V>/* synthetic access */,
    adjacentProvider: AdjacentProvider<V>?,
    result: DataSource.BaseResult<V>
) {

    private val mTotalCount: Int

    private val adjacentProvider: AdjacentProvider<V>

    private var mPrevKey: K? = null

    private var nextKey: K? = null

    private val mDetached = AtomicBoolean(false)

    var loadStateManager: PagedList.LoadStateManager = object : PagedList.LoadStateManager() {
        override fun onStateChanged(
            type: PagedList.LoadType,
            state: PagedList.LoadState,
            error: Throwable?
        ) {
            mPageConsumer.onStateChanged(type, state, error)
        }
    }

    val isDetached: Boolean
        get() = mDetached.get()

    init {
        this.adjacentProvider = adjacentProvider ?: SimpleAdjacentProvider()
        @Suppress("UNCHECKED_CAST")
        mPrevKey = result.prevKey as K?
        @Suppress("UNCHECKED_CAST")
        nextKey = result.nextKey as K?
        this.adjacentProvider.onPageResultResolution(PagedList.LoadType.REFRESH, result)
        mTotalCount = result.totalCount()

        // TODO: move this validation to tiled paging impl, once that's added back
        if (source.type === DataSource.KeyType.POSITIONAL && config.enablePlaceholders) {
            result.validateForInitialTiling(config.pageSize)
        }
    }

    private fun listenTo(
        type: PagedList.LoadType,
        future: ListenableFuture<out DataSource.BaseResult<V>>
    ) {
        // First listen on the BG thread if the DataSource is invalid, since it can be expensive
        future.addListener(Runnable {
            // if invalid, drop result on the floor
            if (source.isInvalid()) {
                detach()
                return@Runnable
            }

            // Source has been verified to be valid after producing data, so sent data to UI
            Futures.addCallback(
                future,
                object : FutureCallback<DataSource.BaseResult<V>> {
                    override fun onSuccess(value: DataSource.BaseResult<V>) {
                        onLoadSuccess(type, value)
                    }

                    override fun onError(throwable: Throwable) {
                        onLoadError(type, throwable)
                    }
                },
                mNotifyExecutor
            )
        }, mFetchExecutor)
    }

    internal interface PageConsumer<V> {
        // return true if we need to fetch more
        fun onPageResult(
            type: PagedList.LoadType,
            pageResult: DataSource.BaseResult<V>
        ): Boolean

        fun onStateChanged(
            type: PagedList.LoadType,
            state: PagedList.LoadState,
            error: Throwable?
        )
    }

    internal interface AdjacentProvider<V> {
        val firstLoadedItem: V?

        val lastLoadedItem: V?

        val firstLoadedItemIndex: Int

        val lastLoadedItemIndex: Int

        /**
         * Notify the AdjacentProvider of new loaded data, to update first/last item/index.
         *
         * NOTE: this data may not be committed (e.g. it may be dropped due to max size). Up to the
         * implementation of the AdjacentProvider to handle this (generally by ignoring this
         * call if dropping is supported).
         */
        fun onPageResultResolution(
            type: PagedList.LoadType,
            result: DataSource.BaseResult<V>
        )
    }

    fun onLoadSuccess(type: PagedList.LoadType, value: DataSource.BaseResult<V>) {
        if (isDetached) {
            // abort!
            return
        }

        adjacentProvider.onPageResultResolution(type, value)

        if (mPageConsumer.onPageResult(type, value)) {
            if (type == PagedList.LoadType.START) {
                @Suppress("UNCHECKED_CAST")
                mPrevKey = value.prevKey as K?
                schedulePrepend()
            } else if (type == PagedList.LoadType.END) {
                @Suppress("UNCHECKED_CAST")
                nextKey = value.nextKey as K?
                scheduleAppend()
            } else {
                throw IllegalStateException("Can only fetch more during append/prepend")
            }
        } else {
            val state =
                if (value.data.isEmpty()) PagedList.LoadState.DONE else PagedList.LoadState.IDLE
            loadStateManager.setState(type, state, null)
        }
    } /* synthetic accessor */

    fun onLoadError(type: PagedList.LoadType, throwable: Throwable) {
        if (isDetached) {
            // abort!
            return
        }
        // TODO: handle nesting
        val state = if (source.isRetryableError(throwable))
            PagedList.LoadState.RETRYABLE_ERROR
        else
            PagedList.LoadState.ERROR
        loadStateManager.setState(type, state, throwable)
    } /* synthetic accessor */

    fun trySchedulePrepend() {
        if (loadStateManager.start == PagedList.LoadState.IDLE) {
            schedulePrepend()
        }
    }

    fun tryScheduleAppend() {
        if (loadStateManager.end == PagedList.LoadState.IDLE) {
            scheduleAppend()
        }
    }

    private fun canPrepend(): Boolean {
        return if (mTotalCount == DataSource.BaseResult.TOTAL_COUNT_UNKNOWN) {
            // don't know count / position from initial load, so be conservative, return true
            true
        } else adjacentProvider.firstLoadedItemIndex > 0

        // position is known, do we have space left?
    }

    private fun canAppend(): Boolean {
        return if (mTotalCount == DataSource.BaseResult.TOTAL_COUNT_UNKNOWN) {
            // don't know count / position from initial load, so be conservative, return true
            true
        } else adjacentProvider.lastLoadedItemIndex < mTotalCount - 1

        // count is known, do we have space left?
    }

    private fun schedulePrepend() {
        if (!canPrepend()) {
            onLoadSuccess(PagedList.LoadType.START, DataSource.BaseResult.empty())
            return
        }

        @Suppress("UNCHECKED_CAST")
        val key = when (source.type) {
            DataSource.KeyType.POSITIONAL ->
                (adjacentProvider.firstLoadedItemIndex - 1) as K
            DataSource.KeyType.PAGE_KEYED -> mPrevKey
            DataSource.KeyType.ITEM_KEYED -> (source as ListenableItemKeyedDataSource).getKey(
                adjacentProvider.firstLoadedItem!!
            )
        }

        loadStateManager.setState(PagedList.LoadType.START, PagedList.LoadState.LOADING, null)
        listenTo(
            PagedList.LoadType.START, source.load(
                DataSource.Params(
                    DataSource.LoadType.START,
                    key,
                    config.initialLoadSizeHint,
                    config.enablePlaceholders,
                    config.pageSize
                )
            )
        )
    }

    private fun scheduleAppend() {
        if (!canAppend()) {
            onLoadSuccess(PagedList.LoadType.END, DataSource.BaseResult.empty())
            return
        }

        @Suppress("UNCHECKED_CAST")
        val key = when (source.type) {
            DataSource.KeyType.POSITIONAL -> (adjacentProvider.lastLoadedItemIndex + 1) as K
            DataSource.KeyType.PAGE_KEYED -> nextKey
            DataSource.KeyType.ITEM_KEYED -> (source as ListenableItemKeyedDataSource).getKey(
                adjacentProvider.lastLoadedItem!!
            )
        }

        loadStateManager.setState(PagedList.LoadType.END, PagedList.LoadState.LOADING, null)
        listenTo(
            PagedList.LoadType.END, source.load(
                DataSource.Params(
                    DataSource.LoadType.END,
                    key,
                    config.initialLoadSizeHint,
                    config.enablePlaceholders,
                    config.pageSize
                )
            )
        )
    }

    fun retry() {
        if (loadStateManager.start == PagedList.LoadState.RETRYABLE_ERROR) {
            schedulePrepend()
        }
        if (loadStateManager.end == PagedList.LoadState.RETRYABLE_ERROR) {
            scheduleAppend()
        }
    }

    fun detach() {
        mDetached.set(true)
    }

    internal class SimpleAdjacentProvider<V> : AdjacentProvider<V> {
        override var firstLoadedItemIndex: Int = 0
            private set
        override var lastLoadedItemIndex: Int = 0
            private set

        override var firstLoadedItem: V? = null
            private set
        override var lastLoadedItem: V? = null
            private set

        var counted: Boolean = false
        var leadingUnloadedCount: Int = 0
        var trailingUnloadedCount: Int = 0

        override fun onPageResultResolution(
            type: PagedList.LoadType,
            result: DataSource.BaseResult<V>
        ) {
            if (result.data.isEmpty()) {
                return
            }
            if (type == PagedList.LoadType.START) {
                firstLoadedItemIndex -= result.data.size
                firstLoadedItem = result.data[0]
                if (counted) {
                    leadingUnloadedCount -= result.data.size
                }
            } else if (type == PagedList.LoadType.END) {
                lastLoadedItemIndex += result.data.size
                lastLoadedItem = result.data.last()
                if (counted) {
                    trailingUnloadedCount -= result.data.size
                }
            } else {
                firstLoadedItemIndex = result.leadingNulls + result.offset
                lastLoadedItemIndex = firstLoadedItemIndex + result.data.size - 1
                firstLoadedItem = result.data[0]
                lastLoadedItem = result.data.last()

                if (result.counted) {
                    counted = true
                    leadingUnloadedCount = result.leadingNulls
                    trailingUnloadedCount = result.trailingNulls
                }
            }
        }
    }
}
