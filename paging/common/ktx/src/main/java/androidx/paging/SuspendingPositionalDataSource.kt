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
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class SuspendingPositionalDataSource<T>(coroutineContext: CoroutineContext) : ListenablePositionalSource<T>() {
    val job = Job().apply {
        start()
    }
    val scope = CoroutineScope(coroutineContext + job)

    override fun invalidate() {
        super.invalidate()
        job.cancel()
    }

    override fun loadInitial(params: PositionalDataSource.LoadInitialParams): ListenableFuture<InitialResult<T>> {
        val future = ResolvableFuture.create<InitialResult<T>>()
        scope.launch {
            try {
                future.set(loadInitialSuspend(params))
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams): ListenableFuture<RangeResult<T>> {
        val future = ResolvableFuture.create<RangeResult<T>>()
        scope.launch {
            try {
                future.set(loadRangeSuspend(params))
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    abstract suspend fun loadInitialSuspend(params: PositionalDataSource.LoadInitialParams): InitialResult<T>
    abstract suspend fun loadRangeSuspend(params: PositionalDataSource.LoadRangeParams): RangeResult<T>
}
