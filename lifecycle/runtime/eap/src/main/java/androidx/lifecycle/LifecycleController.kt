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

/**
 * Attaches to a lifecycle and connects it to a state manager.
 */
@MainThread
internal class LifecycleController(
    val lifecycle: Lifecycle,
    val minState : Lifecycle.State,
    val manager: StateManager<*>
) {
    private val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                manager.finish()
                source.lifecycle.removeObserver(this)
            } else if (source.lifecycle.currentState < minState) {
                manager.pause()
            } else {
                manager.resume()
            }
        }
    }
    init {
        lifecycle.addObserver(observer)
    }

    @MainThread
    fun dispose() {
        lifecycle.removeObserver(observer)
    }
}