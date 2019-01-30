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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

interface LiveDataScope<in T> {
    // suspends until value is dispatched on the Main dispatcher
    suspend fun yield(out: T)
}

internal class LiveDataScopeImpl<T>(
    val target : LiveData<T>,
    context  : CoroutineContext
) : LiveDataScope<T> {
    val context = context + Dispatchers.Main
    override suspend fun yield(out: T) = withContext(context) {
        target.value = out
    }

}

// other can be named RestartingCoroutineLiveData, that restarts on active ?
internal class OneShotCoroutineLiveData<InputT, ReturnT>(
    private val coroutineScope: CoroutineScope,
    private val input: InputT,
    private val block: suspend LiveDataScope<ReturnT>.(InputT) -> Unit
) : LiveData<ReturnT>() {
    private var running = false
    private val liveDataScope =
        LiveDataScopeImpl(this,coroutineScope.coroutineContext)

    override fun onActive() {
        super.onActive()
        if (running) {
            return
        }
        running = true
        println("input A $input")
        coroutineScope.launch {
            println("input B $input")
            liveDataScope.block(input)
        }
    }
}

@UseExperimental(ExperimentalTypeInference::class)
fun <T, R> LiveData<T>.switchMapLaunch(
    context: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
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

@UseExperimental(ExperimentalTypeInference::class)
fun <T, R> LiveData<T>.flatMapLaunch(
    context: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
) : LiveData<R> {
    val result = MediatorLiveData<R>()
    val flatMapRunner = FlatMapRunner<T, R>(
        context = context,
        liveDataScope = LiveDataScopeImpl(result, context),
        block = block
    )
    result.addSource(this) {
        flatMapRunner.submit(it)
    }
    return result
}

private class FlatMapRunner<T, R>(
    context: CoroutineContext,
    val liveDataScope : LiveDataScope<R>,
    val block: suspend LiveDataScope<R>.(T) -> Unit
) {
    val scope = CoroutineScope(context + Job())
    // MainThread
    private var running = false
    // MainThread
    private val inputs = LinkedList<T>()
    private suspend fun runMapping() : Unit {
        var input = consumeNext()
        while(input != null) {
            // TODO if block crashes but not fail the app, we'll lock as in we'll keep buffering
            // but will never consume. what should we do there? detect it and throw again? ignore?
            liveDataScope.block(input)
            input = consumeNext()
        }
    }

    @MainThread
    fun submit(input : T) {
        inputs.add(input)
        if (!running) {
            running = true
            scope.launch {
                runMapping()
            }
        }
    }

    private suspend fun consumeNext() = withContext(Dispatchers.Main) {
        val res = inputs.poll()
        if (res == null) {
            running = false
        }
        res
    }
}