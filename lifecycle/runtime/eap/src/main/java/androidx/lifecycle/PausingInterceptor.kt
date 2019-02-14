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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

internal fun createContextWithPausingInterceptor(handler: Handler, parentJob: Job? = null) : CoroutineContext {
    val job = Job(parentJob)
    return handler.asCoroutineDispatcher() + PausingInterceptor(handler, job) + job
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
    internal val handler: Handler,
    job : Job,
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