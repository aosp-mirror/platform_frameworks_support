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

import androidx.annotation.MainThread
import kotlinx.coroutines.Job

/**
 * Attaches to a lifecycle and controls the [DispatchQueue]'s execution.
 */
@MainThread
internal class LifecycleController(
    private val lifecycle: Lifecycle,
    private val minState: Lifecycle.State,
    private val dispatchQueue: DispatchQueue,
    parentJob: Job
) {
    private val observer = LifecycleEventObserver { source, event ->
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // cancel job before resuming remaining coroutines so that they run in cancelled
            // state
            parentJob.cancel()
            finish()
        } else if (source.lifecycle.currentState < minState) {
            dispatchQueue.pause()
        } else {
            dispatchQueue.resume()
        }
    }

    init {
        lifecycle.addObserver(observer)
    }

    /**
     * Removes the observer and also marks the [DispatchQueue] as finished so that any remaining
     * runnables can be executed.
     */
    @MainThread
    fun finish() {
        lifecycle.removeObserver(observer)
        dispatchQueue.finish()
    }
}