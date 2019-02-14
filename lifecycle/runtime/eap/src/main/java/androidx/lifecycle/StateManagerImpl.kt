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

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import kotlinx.coroutines.Job
import java.lang.IllegalStateException
import java.util.LinkedList
import java.util.Queue

internal interface StateManager<T> {
    @MainThread
    fun pause()

    @MainThread
    fun resume()

    @MainThread
    fun finish()

    @AnyThread
    fun runOrEnqueue(runnable: T)
}

/**
 * Shared logic for enqueuing runnables, keeping running state etc
 * TODO: consider merging back once we have 1 implementation
 */
internal class StateManagerImpl<T>(
    private val handler : Handler,
    private val job : Job,
    private val consumer : (T) -> Unit
) : StateManager<T> {
    // handler thread
    private var paused : Boolean = false
    private var finished : Boolean = false
    // TODO consider using circular buffer
    private val queue : Queue<T> = LinkedList<T>()

    @MainThread
    override fun pause() {
        assertLooper()
        paused = true
    }

    @MainThread
    override fun resume() {
        assertLooper()
        if (!paused) {
            return
        }
        paused = false
        consumeEnqueued()
    }

    @MainThread
    override fun finish() {
        assertLooper()
        job.cancel()
        finished = true
        consumeEnqueued()
    }

    @MainThread
    private fun consumeEnqueued() {
        while (queue.isNotEmpty()) {
            consumer(queue.poll())
        }
    }

    @MainThread
    private fun canRun() = finished || !paused

    @SuppressLint("WrongThread") // we are checking the thread
    override fun runOrEnqueue(runnable: T) {
        if (isHandlerThread()) {
            if (canRun()) {
                consumer(runnable)
            } else {
                enqueue(runnable)
            }
        } else {
            handler.post {
                runOrEnqueue(runnable)
            }
        }
    }

    @MainThread
    private fun enqueue(runnable: T) {
        check(queue.offer(runnable)) {
            throw IllegalStateException("cannot enqueue any more runnables")
        }
    }

    private fun assertLooper() {
        check(isHandlerThread()) {
            "must call this method in the looper thread"
        }
    }

    private fun isHandlerThread() = Looper.myLooper() == handler.looper
}