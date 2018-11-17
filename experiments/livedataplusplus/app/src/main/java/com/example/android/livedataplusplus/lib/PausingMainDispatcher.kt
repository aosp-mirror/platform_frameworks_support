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

package com.example.android.livedataplusplus.lib

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.IllegalStateException
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

private const val MAX_DELAY = Long.MAX_VALUE / 2 // cannot delay for too long on Android

fun lifecycleBoundContext(lifecycleOwner: LifecycleOwner): CoroutineContext {
    val dispatcher = LifecycleBoundDispatcher(lifecycleOwner)
    return dispatcher + dispatcher.job
}

class LifecycleBoundDispatcher(
    private val lifecycleOwner : LifecycleOwner
) : PausingMainDispatcher() {
    internal val job = Job()
    init {
        pause()
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun handleStart() {
                resume()
            }
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun handleStop() {
                pause()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun handleDestroy() {
                job.cancel()
                stop()
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }
}

class ManualPausingDispatcher : PausingMainDispatcher()

@UseExperimental(InternalCoroutinesApi::class)
sealed class PausingMainDispatcher (
    private val handler : Handler = createAsync(Looper.getMainLooper())
) : MainCoroutineDispatcher(), Delay {
    private val pending = LinkedList<RunnableWrapper>()
    var stopped = false
    var paused = false

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val wrapper = wrap(block)
        if (canRun()) {
            execute(wrapper)
        } else {
            pending.add(wrapper)
        }
    }

    internal suspend fun <T> run(f : () -> T) : T {
        val suspension = suspendCancellableCoroutine<T> { cnt ->
            pending.add(RunnableWrapper(this, object : Runnable {
                override fun run() {
                    try {
                        val value = f()
                        cnt.resumeWith(Result.success(value))
                    } catch (t : Throwable) {
                        cnt.resumeWithException(t)
                    }
                }

            }))
        }
        return suspension
    }

    internal suspend fun waitUntilRunning() : Boolean {
        if (!paused) {
            return !stopped
        }
        return run {
            !stopped
        }
    }

    private fun wrap(block: Runnable): RunnableWrapper {
        return if (block is RunnableWrapper && block.dispatcher === this) {
            block
        } else {
            RunnableWrapper(this, block)
        }
    }

    private fun execute(wrapper: RunnableWrapper) {
        if (isHandlerThread()) {
            wrapper.run()
        } else {
            handler.post(wrapper)
        }
    }

    private fun isHandlerThread() = Thread.currentThread() === handler.looper.thread

    private fun assertHandlerThread() {
        if (!isHandlerThread()) {
            throw IllegalStateException("must be on the handler thread")
        }
    }

    // HandlerThread
    fun pause() {
        assertHandlerThread()
        paused = true
    }

    // HandlerThread
    fun stop() {
        assertHandlerThread()
        if (stopped) {
            return
        }
        stopped = true
        runPending()
    }

    // HandlerThread
    fun resume() {
        assertHandlerThread()
        if (!paused) {
            return
        }
        paused = false
        runPending()
    }

    private fun runPending() {
        while (pending.isNotEmpty()) {
            execute(pending.poll())
        }
    }

    @ExperimentalCoroutinesApi
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val block = wrap(Runnable {
            with(continuation) { resumeUndispatched(Unit) }
        })
        handler.postDelayed(block, timeMillis.coerceAtMost(MAX_DELAY))
        continuation.invokeOnCancellation {
            Log.d("CORO", "cancelled")
            handler.removeCallbacks(block)
        }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
        handler.postDelayed(block, timeMillis.coerceAtMost(MAX_DELAY))
        return object : DisposableHandle {
            override fun dispose() {
                handler.removeCallbacks(block)
            }
        }
    }

    internal fun requeue(wrapper : RunnableWrapper) {
        pending.add(wrapper)
    }

    internal fun canRun() = stopped || !paused

    @ExperimentalCoroutinesApi override val immediate: MainCoroutineDispatcher = this

    internal class RunnableWrapper(
        val dispatcher : PausingMainDispatcher,
        val runnable: Runnable
    ) : Runnable {
        override fun run() {
            if (dispatcher.canRun()) {
                runnable.run()
            } else {
                dispatcher.requeue(this)
            }
        }

    }
}

private fun createAsync(looper: Looper): Handler {
    if (Build.VERSION.SDK_INT >= 28) {
        return Handler.createAsync(looper)
    }
    if (Build.VERSION.SDK_INT >= 16) {
        try {
            return Handler::class.java.getDeclaredConstructor(
                Looper::class.java, Handler.Callback::class.java,
                Boolean::class.javaPrimitiveType
            )
                .newInstance(looper, null, true)
        } catch (ignored: IllegalAccessException) {
        } catch (ignored: InstantiationException) {
        } catch (ignored: NoSuchMethodException) {
        } catch (e: InvocationTargetException) {
            return Handler(looper)
        }

    }
    return Handler(looper)
}