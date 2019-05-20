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

import androidx.annotation.RestrictTo
import java.util.AbstractList
import java.util.ArrayList

/**
 * Class holding the pages of data backing a PagedList, presenting sparse loaded data as a List.
 *
 * It has two modes of operation: contiguous and non-contiguous (tiled). This class only holds
 * data, and does not have any notion of the ideas of async loads, or prefetching.
 */
class PagedStorage<T> : AbstractList<T>, Pager.AdjacentProvider<T> {

    var leadingNullCount: Int = 0
        private set
    /**
     * List of pages in storage.
     *
     * Two storage modes:
     *
     * Contiguous - all content in mPages is valid and loaded, but may return false from isTiled().
     * Safe to access any item in any page.
     *
     * Non-contiguous - mPages may have nulls or a placeholder page, isTiled() always returns true.
     * mPages may have nulls, or placeholder (empty) pages while content is loading.
     */
    private val mPages: ArrayList<List<T>?>
    var trailingNullCount: Int = 0
        private set

    var positionOffset: Int = 0
        private set
    /**
     * Number of loaded items held by [.mPages]. When tiling, doesn't count unloaded pages in
     * [.mPages]. If tiling is disabled, same as [.mStorageCount].
     *
     * This count is the one used for trimming.
     */
    var loadedCount: Int = 0
        private set

    /**
     * Number of items represented by [.mPages]. If tiling is enabled, unloaded items in
     * [.mPages] may be null, but this value still counts them.
     */
    var storageCount: Int = 0
        private set

    // If mPageSize > 0, tiling is enabled, 'mPages' may have gaps, and leadingPages is set
    private var mPageSize: Int = 0

    var numberPrepended: Int = 0
        private set
    var numberAppended: Int = 0
        private set

    /**
     * Returns true if all pages are the same size, except for the last, which may be smaller
     */
    val isTiled: Boolean
        get() = mPageSize > 0

    val pageCount: Int
        get() = mPages.size

    val middleOfLoadedRange: Int
        get() = leadingNullCount + positionOffset + storageCount / 2

    // ------------- Adjacent Provider interface (contiguous-only) ------------------

    override // safe to access first page's first item here:
    // If contiguous, mPages can't be empty, can't hold null Pages, and items can't be empty
    val firstLoadedItem: T?
        get() = mPages[0]?.get(0)

    override // safe to access last page's last item here:
    // If contiguous, mPages can't be empty, can't hold null Pages, and items can't be empty
    val lastLoadedItem: T?
        get() {
            val page = mPages[mPages.size - 1]
            return page?.last()
        }

    override val firstLoadedItemIndex: Int
        get() = leadingNullCount + positionOffset

    override val lastLoadedItemIndex: Int
        get() = leadingNullCount + storageCount - 1 + positionOffset

    constructor() {
        leadingNullCount = 0
        mPages = ArrayList()
        trailingNullCount = 0
        positionOffset = 0
        loadedCount = 0
        storageCount = 0
        mPageSize = 1
        numberPrepended = 0
        numberAppended = 0
    }

    constructor(leadingNulls: Int, page: List<T>, trailingNulls: Int) : this() {
        init(leadingNulls, page, trailingNulls, 0)
    }

    private constructor(other: PagedStorage<T>) {
        leadingNullCount = other.leadingNullCount
        mPages = ArrayList(other.mPages)
        trailingNullCount = other.trailingNullCount
        positionOffset = other.positionOffset
        loadedCount = other.loadedCount
        storageCount = other.storageCount
        mPageSize = other.mPageSize
        numberPrepended = other.numberPrepended
        numberAppended = other.numberAppended
    }

    fun snapshot(): PagedStorage<T> {
        return PagedStorage(this)
    }

    private fun init(leadingNulls: Int, page: List<T>, trailingNulls: Int, positionOffset: Int) {
        leadingNullCount = leadingNulls
        mPages.clear()
        mPages.add(page)
        trailingNullCount = trailingNulls

        this.positionOffset = positionOffset
        loadedCount = page.size
        storageCount = loadedCount

        // initialized as tiled. There may be 3 nulls, 2 items, but we still call this tiled
        // even if it will break if nulls convert.
        mPageSize = page.size

        numberPrepended = 0
        numberAppended = 0
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun init(
        leadingNulls: Int,
        page: List<T>,
        trailingNulls: Int,
        positionOffset: Int,
        callback: Callback
    ) {
        init(leadingNulls, page, trailingNulls, positionOffset)
        callback.onInitialized(size)
    }

    override fun get(index: Int): T? {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        // is it definitely outside 'mPages'?
        val localIndex = index - leadingNullCount
        if (localIndex < 0 || localIndex >= storageCount) {
            return null
        }

        var localPageIndex: Int
        var pageInternalIndex: Int

        if (isTiled) {
            // it's inside mPages, and we're tiled. Jump to correct tile.
            localPageIndex = localIndex / mPageSize
            pageInternalIndex = localIndex % mPageSize
        } else {
            // it's inside mPages, but page sizes aren't regular. Walk to correct tile.
            // Pages can only be null while tiled, so accessing page count is safe.
            pageInternalIndex = localIndex
            val localPageCount = mPages.size
            localPageIndex = 0
            while (localPageIndex < localPageCount) {
                val pageSize = mPages[localPageIndex]!!.size
                if (pageSize > pageInternalIndex) {
                    // stop, found the page
                    break
                }
                pageInternalIndex -= pageSize
                localPageIndex++
            }
        }

        val page = mPages[localPageIndex]
        return if (page == null || page.isEmpty()) {
            // can only occur in tiled case, with untouched inner/placeholder pages
            null
        } else page[pageInternalIndex]
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Callback {
        fun onInitialized(count: Int)
        fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int)
        fun onPageAppended(endPosition: Int, changed: Int, added: Int)
        fun onPagePlaceholderInserted(pageIndex: Int)
        fun onPageInserted(start: Int, count: Int)
        fun onPagesRemoved(startOfDrops: Int, count: Int)
        fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int)
    }

    override val size: Int
        get() = leadingNullCount + storageCount + trailingNullCount

    fun computeLeadingNulls(): Int {
        var total = leadingNullCount
        val pageCount = mPages.size
        for (i in 0 until pageCount) {
            val page = mPages[i]
            if (page != null && page !is PlaceholderList) {
                break
            }
            total += mPageSize
        }
        return total
    }

    fun computeTrailingNulls(): Int {
        var total = trailingNullCount
        for (i in mPages.indices.reversed()) {
            val page = mPages[i]
            if (page != null && page !is PlaceholderList) {
                break
            }
            total += mPageSize
        }
        return total
    }

    // ---------------- Trimming API -------------------
    // Trimming is always done at the beginning or end of the list, as content is loaded.
    // In addition to trimming pages in the storage, we also support pre-trimming pages (dropping
    // them just before they're added) to avoid dispatching an add followed immediately by a trim.
    //
    // Note - we avoid trimming down to a single page to reduce chances of dropping page in
    // viewport, since we don't strictly know the viewport. If trim is aggressively set to size of a
    // single page, trimming while the user can see a page boundary is dangerous. To be safe, we
    // just avoid trimming in these cases entirely.

    private fun needsTrim(maxSize: Int, requiredRemaining: Int, localPageIndex: Int): Boolean {
        val page = mPages[localPageIndex]
        return page == null || (loadedCount > maxSize &&
                mPages.size > 2 &&
                page !is PlaceholderList &&
                loadedCount - page.size >= requiredRemaining)
    }

    fun needsTrimFromFront(maxSize: Int, requiredRemaining: Int): Boolean {
        return needsTrim(maxSize, requiredRemaining, 0)
    }

    fun needsTrimFromEnd(maxSize: Int, requiredRemaining: Int): Boolean {
        return needsTrim(maxSize, requiredRemaining, mPages.size - 1)
    }

    fun shouldPreTrimNewPage(maxSize: Int, requiredRemaining: Int, countToBeAdded: Int): Boolean {
        return (loadedCount + countToBeAdded > maxSize &&
                mPages.size > 1 &&
                loadedCount >= requiredRemaining)
    }

    internal fun trimFromFront(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromFront(maxSize, requiredRemaining)) {
            val page = mPages.removeAt(0)
            val removed = page?.size ?: mPageSize
            totalRemoved += removed
            storageCount -= removed
            loadedCount -= page?.size ?: 0
        }

        if (totalRemoved > 0) {
            if (insertNulls) {
                // replace removed items with nulls
                val previousLeadingNulls = leadingNullCount
                leadingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(previousLeadingNulls, totalRemoved)
            } else {
                // simply remove, and handle offset
                positionOffset += totalRemoved
                callback.onPagesRemoved(leadingNullCount, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    internal fun trimFromEnd(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromEnd(maxSize, requiredRemaining)) {
            val page = mPages.removeAt(mPages.size - 1)
            val removed = page?.size ?: mPageSize
            totalRemoved += removed
            storageCount -= removed
            loadedCount -= page?.size ?: 0
        }

        if (totalRemoved > 0) {
            val newEndPosition = leadingNullCount + storageCount
            if (insertNulls) {
                // replace removed items with nulls
                trailingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(newEndPosition, totalRemoved)
            } else {
                // items were just removed, signal
                callback.onPagesRemoved(newEndPosition, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    // ---------------- Contiguous API -------------------

    internal fun prependPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }
        if (mPageSize > 0 && count != mPageSize) {
            if (mPages.size == 1 && count > mPageSize) {
                // prepending to a single item - update current page size to that of 'inner' page
                mPageSize = count
            } else {
                // no longer tiled
                mPageSize = -1
            }
        }

        mPages.add(0, page)
        loadedCount += count
        storageCount += count

        val changedCount = Math.min(leadingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            leadingNullCount -= changedCount
        }
        positionOffset -= addedCount
        numberPrepended += count

        callback.onPagePrepended(leadingNullCount, changedCount, addedCount)
    }

    internal fun appendPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        if (mPageSize > 0) {
            // if the previous page was smaller than mPageSize,
            // or if this page is larger than the previous, disable tiling
            if (mPages[mPages.size - 1]!!.size != mPageSize || count > mPageSize) {
                mPageSize = -1
            }
        }

        mPages.add(page)
        loadedCount += count
        storageCount += count

        val changedCount = Math.min(trailingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            trailingNullCount -= changedCount
        }
        numberAppended += count
        callback.onPageAppended(
            leadingNullCount + storageCount - count,
            changedCount, addedCount
        )
    }

    override fun onPageResultResolution(
        type: PagedList.LoadType,
        result: DataSource.BaseResult<T>
    ) {
        // ignored
    }

    // ------------------ Non-Contiguous API (tiling required) ----------------------

    /**
     * Return true if the page at the passed position would be the first (if trimFromFront) or last
     * page that's currently loading.
     */
    fun pageWouldBeBoundary(positionOfPage: Int, trimFromFront: Boolean): Boolean {
        if (mPageSize < 1 || mPages.size < 2) {
            throw IllegalStateException("Trimming attempt before sufficient load")
        }

        if (positionOfPage < leadingNullCount) {
            // position represent page in leading nulls
            return trimFromFront
        }

        if (positionOfPage >= leadingNullCount + storageCount) {
            // position represent page in trailing nulls
            return !trimFromFront
        }

        val localPageIndex = (positionOfPage - leadingNullCount) / mPageSize

        // walk outside in, return false if we find non-placeholder page before localPageIndex
        if (trimFromFront) {
            for (i in 0 until localPageIndex) {
                if (mPages[i] != null) {
                    return false
                }
            }
        } else {
            for (i in mPages.size - 1 downTo localPageIndex + 1) {
                if (mPages[i] != null) {
                    return false
                }
            }
        }

        // didn't find another page, so this one would be a boundary
        return true
    }

    internal fun initAndSplit(
        leadingNulls: Int,
        multiPageList: List<T>,
        trailingNulls: Int,
        positionOffset: Int,
        pageSize: Int,
        callback: Callback
    ) {
        val pageCount = (multiPageList.size + (pageSize - 1)) / pageSize
        for (i in 0 until pageCount) {
            val beginInclusive = i * pageSize
            val endExclusive = Math.min(multiPageList.size, (i + 1) * pageSize)

            val sublist = multiPageList.subList(beginInclusive, endExclusive)

            if (i == 0) {
                // Trailing nulls for first page includes other pages in multiPageList
                val initialTrailingNulls = trailingNulls + multiPageList.size - sublist.size
                init(leadingNulls, sublist, initialTrailingNulls, positionOffset)
            } else {
                val insertPosition = leadingNulls + beginInclusive
                insertPage(insertPosition, sublist, null)
            }
        }
        callback.onInitialized(size)
    }

    internal fun tryInsertPageAndTrim(
        position: Int,
        page: List<T>,
        lastLoad: Int,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ) {
        val trim = maxSize != PagedList.Config.MAX_SIZE_UNBOUNDED
        val trimFromFront = lastLoad > middleOfLoadedRange

        val pageInserted = (!trim ||
                !shouldPreTrimNewPage(maxSize, requiredRemaining, page.size) ||
                !pageWouldBeBoundary(position, trimFromFront))

        if (pageInserted) {
            insertPage(position, page, callback)
        } else {
            // trim would have us drop the page we just loaded - swap it to null
            val localPageIndex = (position - leadingNullCount) / mPageSize
            mPages.set(localPageIndex, null)

            // note: we also remove it, so we don't have to guess how large a 'null' page is later
            storageCount -= page.size
            if (trimFromFront) {
                mPages.removeAt(0)
                leadingNullCount += page.size
            } else {
                mPages.removeAt(mPages.size - 1)
                trailingNullCount += page.size
            }
        }

        if (trim) {
            if (trimFromFront) {
                trimFromFront(true, maxSize, requiredRemaining, callback)
            } else {
                trimFromEnd(true, maxSize, requiredRemaining, callback)
            }
        }
    }

    internal fun insertPage(position: Int, page: List<T>, callback: Callback?) {
        val newPageSize = page.size
        if (newPageSize != mPageSize) {
            // differing page size is OK in 2 cases, when the page is being added:
            // 1) to the end (in which case, ignore new smaller size)
            // 2) only the last page has been added so far (in which case, adopt new bigger size)

            val size = size
            val addingLastPage = position == size - size % mPageSize && newPageSize < mPageSize
            val onlyEndPagePresent = (trailingNullCount == 0 && mPages.size == 1 &&
                    newPageSize > mPageSize)

            // OK only if existing single page, and it's the last one
            if (!onlyEndPagePresent && !addingLastPage) {
                throw IllegalArgumentException("page introduces incorrect tiling")
            }
            if (onlyEndPagePresent) {
                mPageSize = newPageSize
            }
        }

        val pageIndex = position / mPageSize

        allocatePageRange(pageIndex, pageIndex)

        val localPageIndex = pageIndex - leadingNullCount / mPageSize

        val oldPage = mPages[localPageIndex]
        if (oldPage != null && oldPage !is PlaceholderList) {
            throw IllegalArgumentException(
                "Invalid position $position: data already loaded"
            )
        }
        mPages[localPageIndex] = page
        loadedCount += newPageSize
        callback?.onPageInserted(position, newPageSize)
    }

    fun allocatePageRange(minimumPage: Int, maximumPage: Int) {
        var leadingNullPages = leadingNullCount / mPageSize

        if (minimumPage < leadingNullPages) {
            for (i in 0 until leadingNullPages - minimumPage) {
                mPages.add(0, null)
            }
            val newStorageAllocated = (leadingNullPages - minimumPage) * mPageSize
            storageCount += newStorageAllocated
            leadingNullCount -= newStorageAllocated

            leadingNullPages = minimumPage
        }
        if (maximumPage >= leadingNullPages + mPages.size) {
            val newStorageAllocated = Math.min(
                trailingNullCount,
                (maximumPage + 1 - (leadingNullPages + mPages.size)) * mPageSize
            )
            for (i in mPages.size..maximumPage - leadingNullPages) {
                mPages.add(mPages.size, null)
            }
            storageCount += newStorageAllocated
            trailingNullCount -= newStorageAllocated
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun allocatePlaceholders(index: Int, prefetchDistance: Int, pageSize: Int, callback: Callback) {
        if (pageSize != mPageSize) {
            if (pageSize < mPageSize) {
                throw IllegalArgumentException("Page size cannot be reduced")
            }
            if (mPages.size != 1 || trailingNullCount != 0) {
                // not in single, last page allocated case - can't change page size
                throw IllegalArgumentException(
                    "Page size can change only if last page is only one present"
                )
            }
            mPageSize = pageSize
        }

        val maxPageCount = (size + mPageSize - 1) / mPageSize
        val minimumPage = Math.max((index - prefetchDistance) / mPageSize, 0)
        val maximumPage = Math.min((index + prefetchDistance) / mPageSize, maxPageCount - 1)

        allocatePageRange(minimumPage, maximumPage)
        val leadingNullPages = leadingNullCount / mPageSize
        for (pageIndex in minimumPage..maximumPage) {
            val localPageIndex = pageIndex - leadingNullPages
            if (mPages[localPageIndex] == null) {
                mPages[localPageIndex] = placeholderList
                callback.onPagePlaceholderInserted(pageIndex)
            }
        }
    }

    fun hasPage(pageSize: Int, index: Int): Boolean {
        // NOTE: we pass pageSize here to avoid in case mPageSize
        // not fully initialized (when last page only one loaded)
        val leadingNullPages = leadingNullCount / pageSize

        if (index < leadingNullPages || index >= leadingNullPages + mPages.size) {
            return false
        }

        val page = mPages[index - leadingNullPages]

        return page != null && page !is PlaceholderList
    }

    override fun toString(): String {
        val ret = StringBuilder(
            "leading " + leadingNullCount +
                    ", storage " + storageCount +
                    ", trailing " + trailingNullCount
        )

        for (i in mPages.indices) {
            ret.append(" ").append(mPages[i])
        }
        return ret.toString()
    }

    /**
     * Lists instances are compared (with instance equality) to PLACEHOLDER_LIST to check if an item
     * in that position is already loading. We use a singleton placeholder list that is distinct
     * from Collections.emptyList() for safety.
     */
    private class PlaceholderList<T> : ArrayList<T>()

    private val placeholderList = PlaceholderList<T>()
}
