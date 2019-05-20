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

import androidx.annotation.MainThread
import java.util.concurrent.Executor

internal open class ContiguousPagedList<K, V>(
    val mDataSource: DataSource<K, V>/* synthetic access */,
    mainThreadExecutor: Executor,
    backgroundThreadExecutor: Executor,
    boundaryCallback: BoundaryCallback<V>?,
    config: Config,
    initialResult: DataSource.BaseResult<V>,
    lastLoad: Int
) : PagedList<V>(
    PagedStorage<V>(),
    mainThreadExecutor,
    backgroundThreadExecutor,
    boundaryCallback,
    config
), PagedStorage.Callback, Pager.PageConsumer<V> {

    var mPrependItemsRequested = 0/* synthetic access */
    var mAppendItemsRequested = 0/* synthetic access */

    var mReplacePagesWithNulls = false/* synthetic access */

    val mShouldTrim: Boolean/* synthetic access */

    private val mPager: Pager<*, *>

    override val isDetached: Boolean
        get() = mPager.isDetached

    override val isContiguous: Boolean
        get() = true

    override val dataSource: DataSource<*, V>
        get() = mDataSource

    override val lastKey: Any?
        get() = mDataSource.getKey(lastLoad, lastItem)

    /**
     * Given a page result, apply or drop it, and return whether more loading is needed.
     */
    override fun onPageResult(type: LoadType, pageResult: DataSource.BaseResult<V>): Boolean {
        var continueLoading = false
        val page = pageResult.data


        // if we end up trimming, we trim from side that's furthest from most recent access
        val trimFromFront = lastLoad > storage.middleOfLoadedRange

        // is the new page big enough to warrant pre-trimming (i.e. dropping) it?
        val skipNewPage = mShouldTrim && storage.shouldPreTrimNewPage(
            config.maxSize, mRequiredRemainder, page.size
        )

        if (type == LoadType.END) {
            if (skipNewPage && !trimFromFront) {
                // don't append this data, drop it
                mAppendItemsRequested = 0
            } else {
                storage.appendPage(page, this@ContiguousPagedList)
                mAppendItemsRequested -= page.size
                if (mAppendItemsRequested > 0 && page.size != 0) {
                    continueLoading = true
                }
            }
        } else if (type == LoadType.START) {
            if (skipNewPage && trimFromFront) {
                // don't append this data, drop it
                mPrependItemsRequested = 0
            } else {
                storage.prependPage(page, this@ContiguousPagedList)
                mPrependItemsRequested -= page.size
                if (mPrependItemsRequested > 0 && page.size != 0) {
                    continueLoading = true
                }
            }
        } else {
            throw IllegalArgumentException("unexpected result type $type")
        }

        if (mShouldTrim) {
            // Try and trim, but only if the side being trimmed isn't actually fetching.
            // For simplicity (both of impl here, and contract w/ DataSource) we don't
            // allow fetches in same direction - this means reading the load state is safe.
            if (trimFromFront) {
                if (mPager.mLoadStateManager.start != LoadState.LOADING) {
                    if (storage.trimFromFront(
                            mReplacePagesWithNulls,
                            config.maxSize,
                            mRequiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        // trimmed from front, ensure we can fetch in that dir
                        mPager.mLoadStateManager.setState(
                            LoadType.START,
                            LoadState.IDLE,
                            null
                        )
                    }
                }
            } else {
                if (mPager.mLoadStateManager.end != LoadState.LOADING) {
                    if (storage.trimFromEnd(
                            mReplacePagesWithNulls,
                            config.maxSize,
                            mRequiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        mPager.mLoadStateManager.setState(LoadType.END, LoadState.IDLE, null)
                    }
                }
            }
        }

        triggerBoundaryCallback(type, page)
        return continueLoading
    }

    override fun onStateChanged(
        type: PagedList.LoadType, state: PagedList.LoadState,
        error: Throwable?
    ) {
        dispatchStateChange(type, state, error)
    }

    private fun triggerBoundaryCallback(type: PagedList.LoadType, page: List<V>) {
        if (boundaryCallback != null) {
            val deferEmpty = storage.size == 0
            val deferBegin = (!deferEmpty
                    && type == PagedList.LoadType.START
                    && page.size == 0)
            val deferEnd = (!deferEmpty
                    && type == PagedList.LoadType.END
                    && page.size == 0)
            deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd)
        }
    }

    override fun retry() {
        super.retry()
        mPager.retry()

        if (mRefreshRetryCallback != null && mPager.mLoadStateManager.refresh == PagedList.LoadState.RETRYABLE_ERROR) {
            // Loading the next PagedList failed, signal the retry callback.
            mRefreshRetryCallback!!.run()
        }
    }

    init {
        this.lastLoad = lastLoad
        mPager = Pager(
            config, mDataSource, mainThreadExecutor, backgroundThreadExecutor,
            this, storage, initialResult
        )

        if (config.enablePlaceholders) {
            // Placeholders enabled, pass raw data to storage init
            storage.init(
                initialResult.leadingNulls, initialResult.data,
                initialResult.trailingNulls, initialResult.offset, this
            )
        } else {
            // If placeholder are disabled, avoid passing leading/trailing nulls,
            // since DataSource may have passed them anyway
            storage.init(
                0, initialResult.data,
                0, initialResult.offset + initialResult.leadingNulls, this
            )
        }

        mShouldTrim =
            mDataSource.supportsPageDropping() && this.config.maxSize != PagedList.Config.MAX_SIZE_UNBOUNDED

        if (this.lastLoad == LAST_LOAD_UNSPECIFIED) {
            // Because the ContiguousPagedList wasn't initialized with a last load position,
            // initialize it to the middle of the initial load
            this.lastLoad = (initialResult.leadingNulls + initialResult.offset
                    + initialResult.data.size / 2)
        }
        triggerBoundaryCallback(LoadType.REFRESH, initialResult.data)
    }

    override fun dispatchCurrentLoadState(listener: LoadStateListener) {
        mPager.mLoadStateManager.dispatchCurrentLoadState(listener)
    }

    override fun setInitialLoadState(loadState: LoadState, error: Throwable?) {
        mPager.mLoadStateManager.setState(LoadType.REFRESH, loadState, error)
    }

    @MainThread
    override fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<V>, callback: Callback) {
        val snapshotStorage = snapshot.storage

        val newlyAppended = storage.numberAppended - snapshotStorage.numberAppended
        val newlyPrepended = storage.numberPrepended - snapshotStorage.numberPrepended

        val previousTrailing = snapshotStorage.trailingNullCount
        val previousLeading = snapshotStorage.leadingNullCount

        // Validate that the snapshot looks like a previous version of this list - if it's not,
        // we can't be sure we'll dispatch callbacks safely
        if (snapshotStorage.isEmpty()
            || newlyAppended < 0
            || newlyPrepended < 0
            || storage.trailingNullCount != Math.max(previousTrailing - newlyAppended, 0)
            || storage.leadingNullCount != Math.max(previousLeading - newlyPrepended, 0)
            || storage.storageCount != snapshotStorage.storageCount + newlyAppended + newlyPrepended
        ) {
            throw IllegalArgumentException("Invalid snapshot provided - doesn't appear" + " to be a snapshot of this PagedList")
        }

        if (newlyAppended != 0) {
            val changedCount = Math.min(previousTrailing, newlyAppended)
            val addedCount = newlyAppended - changedCount

            val endPosition = snapshotStorage.leadingNullCount + snapshotStorage.storageCount
            if (changedCount != 0) {
                callback.onChanged(endPosition, changedCount)
            }
            if (addedCount != 0) {
                callback.onInserted(endPosition + changedCount, addedCount)
            }
        }
        if (newlyPrepended != 0) {
            val changedCount = Math.min(previousLeading, newlyPrepended)
            val addedCount = newlyPrepended - changedCount

            if (changedCount != 0) {
                callback.onChanged(previousLeading, changedCount)
            }
            if (addedCount != 0) {
                callback.onInserted(0, addedCount)
            }
        }
    }

    @MainThread
    override fun loadAroundInternal(index: Int) {
        val prependItems = getPrependItemsRequested(
            config.prefetchDistance, index,
            storage.leadingNullCount
        )
        val appendItems = getAppendItemsRequested(
            config.prefetchDistance, index,
            storage.leadingNullCount + storage.storageCount
        )

        mPrependItemsRequested = Math.max(prependItems, mPrependItemsRequested)
        if (mPrependItemsRequested > 0) {
            mPager.trySchedulePrepend()
        }

        mAppendItemsRequested = Math.max(appendItems, mAppendItemsRequested)
        if (mAppendItemsRequested > 0) {
            mPager.tryScheduleAppend()
        }
    }

    override fun detach() {
        mPager.detach()
    }

    @MainThread
    override fun onInitialized(count: Int) {
        notifyInserted(0, count)
        // simple heuristic to decide if, when dropping pages, we should replace with placeholders
        mReplacePagesWithNulls = storage.leadingNullCount > 0 || storage.trailingNullCount > 0
    }

    @MainThread
    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {

        // finally dispatch callbacks, after prepend may have already been scheduled
        notifyChanged(leadingNulls, changed)
        notifyInserted(0, added)

        offsetAccessIndices(added)
    }

    @MainThread
    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {
        // finally dispatch callbacks, after append may have already been scheduled
        notifyChanged(endPosition, changed)
        notifyInserted(endPosition + changed, added)
    }


    @MainThread
    override fun onPagePlaceholderInserted(pageIndex: Int) {
        throw IllegalStateException("Tiled callback on ContiguousPagedList")
    }

    @MainThread
    override fun onPageInserted(start: Int, count: Int) {
        throw IllegalStateException("Tiled callback on ContiguousPagedList")
    }

    override fun onPagesRemoved(startOfDrops: Int, count: Int) {
        notifyRemoved(startOfDrops, count)
    }

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) {
        notifyChanged(startOfDrops, count)
    }

    companion object {

        val LAST_LOAD_UNSPECIFIED = -1

        fun getPrependItemsRequested(prefetchDistance: Int, index: Int, leadingNulls: Int): Int {
            return prefetchDistance - (index - leadingNulls)
        }

        fun getAppendItemsRequested(
            prefetchDistance: Int, index: Int, itemsBeforeTrailingNulls: Int
        ): Int {
            return index + prefetchDistance + 1 - itemsBeforeTrailingNulls
        }
    }
}
