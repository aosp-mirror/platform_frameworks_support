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
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resumeWithException

class ContinuationWrapper<T>(
    private val owner : PausingInterceptor,
    internal val continuation: Continuation<T>) : Continuation<T> by continuation {

    @InternalCoroutinesApi
    override fun resumeWith(result: Result<T>) {
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

class PausingInterceptor(
    internal val handler : Handler
) : ContinuationInterceptor {
    private val paused = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)
    private val queue = LinkedList<() -> Unit>()
    override val key = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return ContinuationWrapper(this, continuation)
    }

    internal fun doResume(block : () -> Unit) {
        if (canRun()) {
            handler.post {
                block()
            }
        } else {
            queue.offer(block)
        }

    }

    private fun canRun() = finished.get() || !paused.get()
}