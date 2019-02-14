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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

@Suppress("EXPERIMENTAL_API_USAGE")
@InternalCoroutinesApi
internal class PausingDispatcher(
    val handler: Handler
) : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        handler.post(maybeWrap(block))
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        val runnable = maybeWrap(Runnable {
            with(continuation) {
                resumeUndispatched(Unit)
            }
        })
        handler.postDelayed(runnable, timeMillis)
        continuation.invokeOnCancellation {
            handler.removeCallbacks(runnable)
        }
    }

    internal fun runOrEnqueue(wrapper: Wrapper) {
        wrapper.doRun()
    }

    private fun maybeWrap(runnable: Runnable) = when(runnable) {
        is Wrapper -> runnable
        else -> Wrapper(this, runnable)
    }

    internal class Wrapper(
        val pausingDispatcher: PausingDispatcher,
        val real: Runnable
    ) : Runnable {
        fun doRun() = real.run()
        override fun run() {
            pausingDispatcher.runOrEnqueue(this)
        }

    }
}