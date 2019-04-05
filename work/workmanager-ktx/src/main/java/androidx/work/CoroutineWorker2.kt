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

package androidx.work

import android.content.Context
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A [ListenableWorker] implementation that provides interop with Kotlin Coroutines.  Override
 * the [doWork] function to do your suspending work.
 * <p>
 * By default, CoroutineWorker runs on the executor defined in [Configuration.Builder.setExecutor]
 * this can be modified by overriding [coroutineContext].
 * <p>
 * A CoroutineWorker is given a maximum of ten minutes to finish its execution and return a
 * [ListenableWorker.Result].  After this time has expired, the worker will be signalled to stop.
 */
abstract class CoroutineWorker2(
    appContext: Context,
    params: WorkerParameters
) : ListenableWorker(appContext, params) {

    internal val job = Job()
    internal val future: SettableFuture<Result> = SettableFuture.create()

    init {
        future.addListener(
            Runnable {
                if (future.isCancelled) {
                    job.cancel()
                }
            },
            taskExecutor.backgroundExecutor
        )
    }

    /**
     * The coroutine context on which [doWork] will run. By default, this is defined in
     * [Configuration.Builder.setExecutor].
     */
    open val coroutineContext: CoroutineContext = backgroundExecutor.asCoroutineDispatcher()

    final override fun startWork(): ListenableFuture<Result> {

        val coroutineScope = CoroutineScope(coroutineContext + job)
        coroutineScope.launch {
            try {
                val result = doWork()
                future.set(result)
            } catch (t: Throwable) {
                future.setException(t)
            }
        }

        return future
    }

    /**
     * A suspending method to do your work.  This function runs on the coroutine context specified
     * by [coroutineContext]. The receiver inside the [doWork] method is the [coroutineContext]
     * used.
     * <p>
     * A CoroutineWorker is given a maximum of ten minutes to finish its execution and return a
     * [ListenableWorker.Result].  After this time has expired, the worker will be signalled to
     * stop.
     *
     * @return The [ListenableWorker.Result] of the result of the background work; note that
     * dependent work will not execute if you return [ListenableWorker.Result.failure]
     */
    abstract suspend fun CoroutineScope.doWork(): Result

    final override fun onStopped() {
        super.onStopped()
        future.cancel(false)
    }
}