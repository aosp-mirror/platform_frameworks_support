/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback

/**
 * Methods for computing and applying DiffResults between PagedLists.
 *
 * To minimize the amount of diffing caused by placeholders, we only execute DiffUtil in a reduced
 * 'diff space' - in the range (computeLeadingNulls..size-computeTrailingNulls).
 *
 * This allows the diff of a PagedList, e.g.:
 * 100 nulls, placeholder page, (empty page) x 5, page, 100 nulls
 *
 * To only inform DiffUtil about single loaded page in this case, by pruning all other nulls from
 * consideration.
 *
 * @see PagedStorage.computeLeadingNulls
 * @see PagedStorage.computeTrailingNulls
 */
internal object PagedStorageDiffHelper {
    fun <T : Any> computeDiff(
        oldList: PagedStorage<T>,
        newList: PagedStorage<T>,
        diffCallback: DiffUtil.ItemCallback<T>
    ): DiffUtil.DiffResult {
        val oldOffset = oldList.computeLeadingNulls()
        val newOffset = newList.computeLeadingNulls()

        val oldSize = oldList.size - oldOffset - oldList.computeTrailingNulls()
        val newSize = newList.size - newOffset - newList.computeTrailingNulls()

        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldItem = oldList[oldItemPosition + oldOffset]
                val newItem = newList[newItemPosition + newList.leadingNullCount]

                return when {
                    oldItem == null || newItem == null -> null
                    else -> diffCallback.getChangePayload(oldItem, newItem)
                }
            }

            override fun getOldListSize() = oldSize

            override fun getNewListSize() = newSize

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition + oldOffset]
                val newItem = newList[newItemPosition + newList.leadingNullCount]

                return when {
                    oldItem === newItem -> true
                    oldItem == null || newItem == null -> false
                    else -> diffCallback.areItemsTheSame(oldItem, newItem)
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition + oldOffset]
                val newItem = newList[newItemPosition + newList.leadingNullCount]

                return when {
                    oldItem === newItem -> true
                    oldItem == null || newItem == null -> false
                    else -> diffCallback.areContentsTheSame(oldItem, newItem)
                }
            }
        }, true)
    }

    private class OffsettingListUpdateCallback internal constructor(
        private val offset: Int,
        private val callback: ListUpdateCallback
    ) : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            callback.onInserted(position + offset, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            callback.onRemoved(position + offset, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            callback.onMoved(fromPosition + offset, toPosition + offset)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            callback.onChanged(position + offset, count, payload)
        }
    }

    /**
     * TODO: improve diffing logic
     *
     * This function currently does a naive diff, assuming null does not become an item, and vice
     * versa (so it won't dispatch onChange events for these). It's similar to passing a list with
     * leading/trailing nulls in the beginning / end to DiffUtil, but dispatches the remove/insert
     * for changed nulls at the beginning / end of the list.
     *
     * Note: if lists mutate between diffing the snapshot and dispatching the diff here, then we
     * handle this by passing the snapshot to the callback, and dispatching those changes
     * immediately after dispatching this diff.
     */
    fun <T : Any> dispatchDiff(
        callback: ListUpdateCallback,
        oldList: PagedStorage<T>,
        newList: PagedStorage<T>,
        diffResult: DiffUtil.DiffResult
    ) {
        val trailingOld = oldList.computeTrailingNulls()
        val trailingNew = newList.computeTrailingNulls()
        val leadingOld = oldList.computeLeadingNulls()
        val leadingNew = newList.computeLeadingNulls()

        if (trailingOld == 0 &&
            trailingNew == 0 &&
            leadingOld == 0 &&
            leadingNew == 0
        ) {
            // Simple case, dispatch & return
            diffResult.dispatchUpdatesTo(callback)
            return
        }

        // First, remove or insert trailing nulls
        if (trailingOld > trailingNew) {
            val count = trailingOld - trailingNew
            callback.onRemoved(oldList.size - count, count)
        } else if (trailingOld < trailingNew) {
            callback.onInserted(oldList.size, trailingNew - trailingOld)
        }

        // Second, remove or insert leading nulls
        if (leadingOld > leadingNew) {
            callback.onRemoved(0, leadingOld - leadingNew)
        } else if (leadingOld < leadingNew) {
            callback.onInserted(0, leadingNew - leadingOld)
        }

        // apply the diff, with an offset if needed
        if (leadingNew != 0) {
            diffResult.dispatchUpdatesTo(OffsettingListUpdateCallback(leadingNew, callback))
        } else {
            diffResult.dispatchUpdatesTo(callback)
        }
    }

    /**
     * Given an oldPosition representing an anchor in the old data set, computes its new position
     * after the diff, or a guess if it no longer exists.
     */
    fun transformAnchorIndex(
        diffResult: DiffUtil.DiffResult,
        oldList: PagedStorage<*>,
        newList: PagedStorage<*>,
        oldPosition: Int
    ): Int {
        val oldOffset = oldList.computeLeadingNulls()

        // diffResult's indices starting after nulls, need to transform to diffutil indices
        // (see also dispatchDiff(), which adds this offset when dispatching)
        val diffIndex = oldPosition - oldOffset

        val oldSize = oldList.size - oldOffset - oldList.computeTrailingNulls()

        // if our anchor is non-null, use it or close item's position in new list
        if (diffIndex in 0 until oldSize) {
            // search outward from old position for position that maps
            for (i in 0..29) {
                val positionToTry = diffIndex + i / 2 * if (i % 2 == 1) -1 else 1

                // reject if (null) item was not passed to DiffUtil, and wouldn't be in the result
                if (positionToTry < 0 || positionToTry >= oldList.storageCount) {
                    continue
                }

                val result = diffResult.convertOldPositionToNew(positionToTry)
                if (result != -1) {
                    // also need to transform from diffutil output indices to newList
                    return result + newList.leadingNullCount
                }
            }
        }

        // not anchored to an item in new list, so just reuse position (clamped to newList size)
        return Math.max(0, Math.min(oldPosition, newList.size - 1))
    }
}
