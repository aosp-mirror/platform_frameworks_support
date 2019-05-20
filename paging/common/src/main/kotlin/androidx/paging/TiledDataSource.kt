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
import androidx.annotation.WorkerThread

// NOTE: Room 1.0 depends on this class, so it should not be removed until
// we can require a version of Room that uses PositionalDataSource directly
/**
 * @param T Type loaded by the TiledDataSource.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated("Use {@link PositionalDataSource}")
abstract class TiledDataSource<T>() : PositionalDataSource<T>() {

    @WorkerThread
    abstract fun countItems(): Int

    internal override fun isContiguous() = false

    @WorkerThread
    abstract fun loadRange(startPosition: Int, count: Int): List<T>?

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        val totalCount = countItems()
        if (totalCount == 0) {
            callback.onResult(emptyList(), 0, 0)
            return
        }

        // bound the size requested, based on known count
        val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
        val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)

        // convert from legacy behavior
        val list = loadRange(firstLoadPosition, firstLoadSize)
        if (list != null && list.size == firstLoadSize) {
            callback.onResult(list, firstLoadPosition, totalCount)
        } else {
            // null list, or size doesn't match request
            // The size check is a WAR for Room 1.0, subsequent versions do the check in Room
            invalidate()
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        val list = loadRange(params.startPosition, params.loadSize)
        if (list != null) {
            callback.onResult(list)
        } else {
            invalidate()
        }
    }
}
