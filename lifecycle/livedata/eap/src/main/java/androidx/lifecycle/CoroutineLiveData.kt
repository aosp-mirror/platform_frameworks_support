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
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

interface LiveDataScope<in T> {
    // suspends until value is dispatched on the Main dispatcher
    suspend fun yield(out: T)
}

internal class LiveDataScopeImpl<T>(
    var target: LiveData<T>?,
    context: CoroutineContext
) : LiveDataScope<T> {
    val coroutineContext = context + Dispatchers.Main
    fun cancel() {
       target = null
    }
    override suspend fun yield(out: T) = withContext(coroutineContext) {
        target?.value = out
    }
}

// TODO do we want this? it does not know if active or not :/ We could let [LiveData] notify us
// or we could have a base class that does active notifications. But in that case, we lose the
// ability to attach [LiveDataScopeImpl] to "any" LiveData.
/**
 * Creates a [LiveData] from the given [block] which can [LiveDataScope.yield] values.
 *
 * The [block] is launched when the first time [LiveData] is observed and never stopped afterwards
 * (even if the observer unsubscribes).
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T> CoroutineScope.buildLiveData(
    @BuilderInference block: suspend LiveDataScope<T>.() -> Unit
): LiveData<T> {
    return CoroutineLiveData(
        coroutineScope = this,
        block = block
    )
}

// other can be named RestartingCoroutineLiveData, that restarts on active ?
internal class CoroutineLiveData<ReturnT>(
    val coroutineScope: CoroutineScope,
    val block: suspend LiveDataScope<ReturnT>.() -> Unit
) : LiveData<ReturnT>() {
    private var running = false
    private val liveDataScope =
        LiveDataScopeImpl(this, coroutineScope.coroutineContext)

    override fun onActive() {
        super.onActive()
        if (running) {
            return
        }
        running = true
        coroutineScope.launch {
            liveDataScope.block()
        }
    }
}

/**
 * Launches the given block in the given [context] every time [this] has a new value.
 *
 * If a new value arrives while the [block] is running, the previous block is cancelled and any
 * value dispatched by it after cancellation will be ignored.
 *
 * If the given [context] is cancelled, no new [block] is run.
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T, R> LiveData<T>.switchMap(
    coroutineContext: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
): LiveData<R> {
    val mediatorLiveData = MediatorLiveData<R>()
    var previousContext : CoroutineContext? = null
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

// TODO this is concat map not flatmap
// TODO this implementation is not complete yet,
// it should probably also have an intermediate supervisor job, just like switch map.
@UseExperimental(ExperimentalTypeInference::class)
fun <T, R> LiveData<T>.flatMap(
    context: CoroutineContext,
    @BuilderInference block: suspend LiveDataScope<R>.(T) -> Unit
): LiveData<R> {
    val result = MediatorLiveData<R>()
    val flatMapRunner = FlatMapRunner(
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
    val context: CoroutineContext,
    val liveDataScope: LiveDataScope<R>,
    val block: suspend LiveDataScope<R>.(T) -> Unit
) {
    // MainThread
    private var running = false
    // MainThread
    private val inputs = ArrayDeque<T>()

    private suspend fun runMapping() {
        var input = consumeNext()
        while (input != null) {
            val blockInvocation = liveDataScope.runCatching {
                block(input)
            }
            val failure = blockInvocation.exceptionOrNull()
            if (failure != null) {
                // record failure so that if we are given a context that wants to catch exceptions,
                // we don't go dormant
                onFailed()
                throw failure
            } else {
                input = consumeNext()
            }
        }
    }

    private suspend fun onFailed() = withContext(Dispatchers.Main) {
        running = false
        // in case scope has an exception handler, try to re-run
        consumeQueue()
    }

    @MainThread
    fun submit(input: T) {
        inputs.offerFirst(input)
        consumeQueue()
    }

    private fun consumeQueue() {
        if (!running) {
            running = true
            CoroutineScope(context + Job(context[Job])).launch {
                runMapping()
            }
        }
    }

    private suspend fun consumeNext() = withContext(Dispatchers.Main) {
        val res = inputs.pollFirst()
        if (res == null) {
            running = false
        }
        res
    }
}