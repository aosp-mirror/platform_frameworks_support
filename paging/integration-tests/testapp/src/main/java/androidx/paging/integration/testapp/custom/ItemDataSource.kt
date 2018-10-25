/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.paging.integration.testapp.custom

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.paging.PositionalDataSource
import androidx.paging.SuspendingPositionalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

val dataSourceError = AtomicBoolean(false)
/**
 * Sample data source with artificial data.
 */
internal class ItemDataSource : SuspendingPositionalDataSource<Item>(Dispatchers.Default) {
    class RetryableItemError : Exception()

    private val mGenerationId = sGenerationId++

    private suspend fun loadRangeInternal(startPosition: Int, loadCount: Int): List<Item> {
        val items = ArrayList<Item>()
        val end = Math.min(COUNT, startPosition + loadCount)
        val bgColor = COLORS[mGenerationId % COLORS.size]

        delay(1000L)

        println("    loading items $startPosition upto $end")
        if (end < startPosition) {
            throw IllegalStateException()
        }
        for (i in startPosition until end) {
            items.add(Item(i, "item $i", bgColor))
        }
        if (dataSourceError.compareAndSet(true, false)) {
            throw RetryableItemError()
        }
        return items
    }

    companion object {
        private const val COUNT = 60

        @ColorInt
        private val COLORS = intArrayOf(Color.RED, Color.BLUE, Color.BLACK)

        private var sGenerationId: Int = 0
    }


    override suspend fun loadInitialSuspend(params: PositionalDataSource.LoadInitialParams): InitialResult<Item> {
        println("load initial " + params.requestedStartPosition)
        val position = PositionalDataSource.computeInitialLoadPosition(params, COUNT)
        val loadSize = PositionalDataSource.computeInitialLoadSize(params, position, COUNT)
        val data = loadRangeInternal(position, loadSize)

        return InitialResult(data, position, COUNT)
    }

    override suspend fun loadRangeSuspend(params: PositionalDataSource.LoadRangeParams): RangeResult<Item> {
        println("load range " + params.startPosition + ", " + params.loadSize)
        val data = loadRangeInternal(params.startPosition, params.loadSize)
        return RangeResult(data)
    }

    override fun isRetryableError(error: Throwable): Boolean {
        return error is RetryableItemError
    }
}
