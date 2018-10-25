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

import androidx.concurrent.futures.ResolvableFuture
import androidx.paging.PositionalDataSource.computeInitialLoadPosition
import androidx.paging.PositionalDataSource.computeInitialLoadSize
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PagerTest {

    class ImmediateListDataSource(private val data: List<String>) : ListenablePositionalDataSource<String>() {
        override fun loadInitial(params: PositionalDataSource.LoadInitialParams): ListenableFuture<InitialResult<String>> {
            val future = ResolvableFuture.create<InitialResult<String>>()

            val totalCount = data.size

            val position = computeInitialLoadPosition(params, totalCount)
            val loadSize = computeInitialLoadSize(params, position, totalCount)

            val sublist = data.subList(position, position + loadSize)
            future.set(InitialResult(sublist, position, totalCount))
            return future
        }

        override fun loadRange(params: PositionalDataSource.LoadRangeParams): ListenableFuture<RangeResult<String>> {
            val future = ResolvableFuture.create<RangeResult<String>>()

            future.set(RangeResult(data.subList(params.startPosition,
                    params.startPosition + params.loadSize)))

            return future
        }
    }

    val data = List(25) { "$it" }

    @Test
    fun pager() {
        /*
        object : Pager.PageConsumer<String> {
            override fun onPageResult(
                type: PagedList.LoadType,
                pageResult: ListenableSource.BaseResult<String>
            ): Boolean {
            }

            override fun onStateChanged(
                type: PagedList.LoadType,
                state: PagedList.LoadState,
                error: Throwable?
            ) {
            }

        }
        Pager(
            PagedList.Config(2, 2, true, 10, PagedList.Config.MAX_SIZE_UNBOUNDED),
            ImmediateListDataSource(data),
            DirectExecutor.INSTANCE,
            ,
            null,
            ListenablePositionalDataSource.InitialResult(data.subList(5, ))

        );
        */
    }
}
