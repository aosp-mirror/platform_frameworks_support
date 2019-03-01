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
import androidx.arch.core.executor.ArchTaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

interface LiveDataScope<in T> {
    // suspends until value is dispatched on the Main dispatcher
    suspend fun yield(out: T)
}

internal class LiveDataScopeImpl<T>(
    private var target: LiveData<T>?,
    context: CoroutineContext
) : LiveDataScope<T> {
    val coroutineContext = context + Dispatchers.Main

    override suspend fun yield(out: T) = withContext(coroutineContext) {
        target?.value = out
    }
}

/**
 * Launches the given block in the given [coroutineContext] every time [this] has a new value.
 *
 * If a new value arrives while the [block] is running, the previous block is cancelled and any
 * value dispatched by it after cancellation will be ignored.
 *
 * If the given [coroutineContext] is cancelled, no new [block] is run.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T, R> LiveData<T>.switchMap(
    coroutineContext: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
): LiveData<R> {
    val mediatorLiveData = MediatorLiveData<R>()
    var previousContext: CoroutineContext? = null
    // parentJob may not exist (maybe they just want to specify a dispatcher)
    val parentJob = coroutineContext[Job]
    // create an intermediate supervisor job so that canceling the previous run will not cancel
    // the parent context's Job. We'll cancel this SupervisorJob only if we are cancelled due to an
    // exception throw in the block
    val mySupervisorJob = SupervisorJob(parentJob)
    val observer = Observer<T> { input ->
        if (parentJob?.isActive != false) {
            // cancel the previous block
            previousContext?.cancel()
            val newContext = coroutineContext + Job(mySupervisorJob)
            val liveDataScope = LiveDataScopeImpl(mediatorLiveData, newContext)
            previousContext = newContext
            CoroutineScope(newContext).also {
                it.launch {
                    liveDataScope.block(input)
                }
            }
        } else {
            mediatorLiveData.removeSource(this@switchMap)
        }
    }
    mediatorLiveData.addSource(this, observer)
    mySupervisorJob.invokeOnCompletion {
        ArchTaskExecutor.getInstance().executeOnMainThread {
            mediatorLiveData.removeSource(this@switchMap)
        }
    }
    return mediatorLiveData
}

fun <T, R> LiveData<T>.map(
    coroutineContext: CoroutineContext,
    bufferSize: Int = Int.MAX_VALUE,
    block: suspend (T) -> (R)
): LiveData<R> {
    if (bufferSize < 0) {
        throw IllegalArgumentException("Buffer size must be at least 0. Buffer Size: $bufferSize")
    }
    val mediator = MediatorLiveData<R>()
    val liveDataScope = LiveDataScopeImpl(mediator, coroutineContext)
    val mapRunner = MapRunner(
        bufferSize = bufferSize,
        coroutineContext = coroutineContext,
        block = block,
        liveDataScope = liveDataScope
    )
    mediator.addSource(this) {
        if (!mapRunner.submit(it)) {
            mediator.removeSource(this@map)
        }
    }
    // eager removal even if no values are added
    mapRunner.supervisorJob.invokeOnCompletion {
        mediator.removeSource(this)
    }
    return mediator
}

class MapRunner<T, R>(
    private val bufferSize: Int = 0,
    coroutineContext: CoroutineContext,
    private val block: suspend (T) -> R,
    private val liveDataScope: LiveDataScope<R>
) {
    // we could use an actor or a channel but both are experimental and Actors are likely to
    // change significantly https://github.com/Kotlin/kotlinx.coroutines/issues/87
    val supervisorJob = SupervisorJob(coroutineContext[Job])
    private val blockScope = CoroutineScope(coroutineContext + supervisorJob)
    private val scopedMainContext = coroutineContext + Dispatchers.Main

    // main dispatcher
    private val queue = ArrayDeque<T>()

    // main dispatcher
    private var running = false

    @MainThread
    fun submit(item: T) : Boolean {
        if (!supervisorJob.isActive) {
            return false
        }
        // this is intentionally bufferSize + 1
        while (queue.size > bufferSize) queue.pop()
        queue.offer(item)
        maybeLaunch()
        return true
    }

    @MainThread
    private fun maybeLaunch() {
        if (!running) {
            running = true
            val job = blockScope.launch {
                consume()
            }
            job.invokeOnCompletion {
                // if it failed but developer caught the exception via an exception handler,
                // we should continue execution
                running = false
                ArchTaskExecutor.getMainThreadExecutor().execute {
                    maybeLaunch()
                }
            }
        }
    }

    private suspend fun next(): T? = withContext(scopedMainContext) {
        queue.poll()
    }

    private suspend fun consume() {
        var input = next()
        while (input != null) {
            liveDataScope.yield(block(input))
            input = next()
        }
    }
}