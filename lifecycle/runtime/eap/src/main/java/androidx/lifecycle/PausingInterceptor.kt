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

package androidx.lifecycle

import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resumeWithException


internal suspend fun <T> whenResumedInterceptor(
    handler: Handler = MAIN_HANDLER,
    lifecycle: Lifecycle,
    block: suspend CoroutineScope.() -> T
): T = withContext(Dispatchers.Main) {
    val job = coroutineContext[Job] ?: throw IllegalStateException(
        "calling coroutine must have a job to use lifecycle dispatcher"
    )
    val pausingInterceptor = PausingInterceptor(handler, job)
    val controller = LifecycleController(lifecycle, Lifecycle.State.RESUMED, pausingInterceptor)
    try {
        withContext(handler.asCoroutineDispatcher() + pausingInterceptor, block)
    } finally {
        controller.dispose()
    }
}

internal class ContinuationWrapper<T>(
    private val owner: PausingInterceptor,
    internal val continuation: Continuation<T>
) : Continuation<T> by continuation {

    @InternalCoroutinesApi
    override fun resumeWith(result: Result<T>) {
        // TODO DispatchedTask has a lot more logic, should we copy it all?
        owner.doResume {
            val job = context[Job]!! // TODO our usage can probably enforce a job ?
            if (job.isActive) {
                continuation.resumeWith(result)
            } else {
                continuation.resumeWithException(job.getCancellationException())
            }
        }
    }
}

internal class PausingInterceptor(
    handler: Handler,
    job: Job,
    private val stateManager: StateManager<() -> Unit> = StateManagerImpl(handler, job) {
        it()
    }
) : ContinuationInterceptor, StateManager<() -> Unit> by stateManager {
    override val key = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return ContinuationWrapper(this, continuation)
    }

    internal fun doResume(block: () -> Unit) {
        stateManager.runOrEnqueue(block)
    }
}