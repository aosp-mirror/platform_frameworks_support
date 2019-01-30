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

import androidx.arch.core.executor.ArchTaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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