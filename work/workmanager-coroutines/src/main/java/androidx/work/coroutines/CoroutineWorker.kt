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

package androidx.work.coroutines

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A {@link ListenableWorker} implementation that provides interop with Kotlin Coroutines.  By
 * default, CoroutineWorker runs on {@link Dispatchers#Default}, which can be modified by overriding
 * {@link #getCoroutineContext()}.  Override the {@link #doWork()} function to do your suspending
 * work.
 */
abstract class CoroutineWorker(
    appContext: Context,
    params: WorkerParameters
) : ListenableWorker(appContext, params) {

    private val job = Job()
    private val future: SettableFuture<Payload> = SettableFuture.create()

    final override fun startWork(): ListenableFuture<Payload> {

        val coroutineContext = getCoroutineContext()
        val coroutineScope = CoroutineScope(coroutineContext + job)
        coroutineScope.launch(coroutineContext) {
            try {
                val payload = doWork()
                future.set(payload)
            } catch (t: Throwable) {
                future.setException(t)
            }
        }

        return future
    }

    /**
     * A suspending method to do your work.  This function runs on the coroutine context specified
     * by {@link #getCoroutineContext()}.
     *
     * @return The {@link ListenableWorker.Payload} of the result of the background work
     */
    abstract suspend fun doWork(): ListenableWorker.Payload

    /**
     * The coroutine context on which {@link #doWork()} will run.  By default, this is
     * {@link Dispatchers#Default}.
     *
     * @return The coroutine context on which to do background work
     */
    open fun getCoroutineContext() = Dispatchers.Default

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)
        future.cancel(false)
        job.cancel()
    }
}