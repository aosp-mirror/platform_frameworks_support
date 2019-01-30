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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface LiveDataScope<in T> {
    // suspends until value is dispatched on the Main dispatcher
    suspend fun yield(out: T)
}

// other can be named RestartingCoroutineLiveData, that restarts on active ?
internal class OneShotCoroutineLiveData<InputT, ReturnT>(
    private val coroutineScope: CoroutineScope,
    private val input: InputT,
    private val block: suspend LiveDataScope<ReturnT>.(InputT) -> Unit
) : LiveData<ReturnT>() {
    private var running = false
    private val liveDataScope = object : LiveDataScope<ReturnT> {
        override suspend fun yield(out: ReturnT) {
            // TODO use immediate here
            coroutineScope.launch(Dispatchers.Main) {
                value = out
            }
        }

    }

    override fun onActive() {
        super.onActive()
        if (running) {
            return
        }
        running = true

        coroutineScope.launch {
            liveDataScope.block(input)
        }
    }
}

fun <T, R> LiveData<T>.switchMapLaunch(
    context: CoroutineContext,
    block: suspend LiveDataScope<R>.(T) -> Unit
) : LiveData<R> {
    var previousScope: CoroutineScope? = null
    return Transformations.switchMap(this) { input ->
        previousScope?.coroutineContext?.get(Job)?.cancel()
        val scope = CoroutineScope(context + Job())
        previousScope = scope
        OneShotCoroutineLiveData<T, R>(
            coroutineScope = scope,
            input = input,
            block = block
        )
    }
}