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
import java.util.ArrayDeque
import java.util.Queue

@JvmField
internal var MAIN_HANDLER = Handler(Looper.getMainLooper())

// for internal testing
internal fun setMainHandler(handler: Handler) {
    MAIN_HANDLER = handler
}

// for internal testing
internal fun resetMainHandler() {
    MAIN_HANDLER = Handler(Looper.getMainLooper())
}

/**
 * Helper class for [PausingDispatcher] that tracks runnables which are enqueued to the dispatcher
 * and also calls back the [PausingDispatcher] when the runnable should run.
 */
internal class DispatchQueue(
    val handler: Handler = MAIN_HANDLER
) {
    // handler thread
    private var paused: Boolean = false
    // handler thread
    private var finished: Boolean = false

    private val queue: Queue<Runnable> = ArrayDeque<Runnable>()

    @MainThread
    fun pause() {
        assertLooper()
        paused = true
    }

    @MainThread
    fun resume() {
        assertLooper()
        if (!paused) {
            return
        }
        check(!finished) {
            "Cannot resume a finished dispatcher"
        }
        paused = false
        consumeEnqueued()
    }

    @MainThread
    fun finish() {
        assertLooper()
        finished = true
        consumeEnqueued()
    }

    @MainThread
    private fun consumeEnqueued() {
        while (queue.isNotEmpty()) {
            queue.poll().run()
        }
    }

    @MainThread
    private fun canRun() = finished || !paused

    @AnyThread
    @SuppressLint("WrongThread") // false negative, we are checking the thread
    fun runOrEnqueue(runnable: Runnable) {
        if (isHandlerThread()) {
            if (canRun()) {
                runnable.run()
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
    private fun enqueue(runnable: Runnable) {
        check(queue.offer(runnable)) {
            "cannot enqueue any more runnables"
        }
    }

    private fun assertLooper() {
        check(isHandlerThread()) {
            "must call this method in the looper thread"
        }
    }

    private fun isHandlerThread() = Looper.myLooper() == handler.looper
}