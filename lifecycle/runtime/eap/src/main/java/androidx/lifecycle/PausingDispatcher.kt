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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

internal const val MAX_DELAY = Long.MAX_VALUE / 2 // cannot delay for too long on Android

/**
 * Creates a new coroutine context with the given [handler] and [parentJob].
 *
 * @see PausingDispatcher
 */
@UseExperimental(InternalCoroutinesApi::class)
internal fun createContextWithPausingDispatcher(
    handler: Handler,
    parentJob: Job? = null
): CoroutineContext {
    val job = Job(parentJob)
    return PausingDispatcher(handler, job) + job
}

/**
 * A [CoroutineDispatcher] implementation that also has [pause] and [finish] methods.
 *
 * When the dispatcher is paused, it does not run resume any continuation until it is resumed.
 * If finished, it runs all pending continuations after cancelling the related job, which in return
 * runs the finally blocks and nothing else.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
@InternalCoroutinesApi
internal class PausingDispatcher(
    /**
     * The handler into which messages are psted
     */
    private val handler: Handler,
    /**
     * The job created for this dispatcher's context. It is cancelled when dispatcher is finished.
     */
    job: Job,
    /**
     * helper class to maintain state and enqueued continuations.
     */
    private val stateManager: StateManager<Wrapper> = StateManagerImpl(handler, job) {
        it.doRun()
    }
) : MainCoroutineDispatcher(), StateManager<PausingDispatcher.Wrapper> by stateManager, Delay {

    @ExperimentalCoroutinesApi
    override val immediate: MainCoroutineDispatcher = this

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        stateManager.runOrEnqueue(maybeWrap(block))
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        val runnable = maybeWrap(Runnable {
            with(continuation) {
                // we can resume undispatched since we already have the runnable wrapped into
                // our custom wrapper
                resumeUndispatched(Unit)
            }
        })
        handler.postDelayed(runnable, timeMillis.coerceAtMost(MAX_DELAY))
        continuation.invokeOnCancellation {
            handler.removeCallbacks(runnable)
        }
    }

    /**
     * Wraps the given runnable into a [Wrapper] so that we can reference back to this dispatcher
     * when it is time to run it.
     */
    private fun maybeWrap(runnable: Runnable): Wrapper =
        if (runnable is Wrapper && runnable.pausingDispatcher === this) {
            runnable
        } else {
            Wrapper(this, runnable)
        }

    /**
     * A wrapper for the enqueued runnable that keeps a reference back to the [PausingDispatcher].
     */
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