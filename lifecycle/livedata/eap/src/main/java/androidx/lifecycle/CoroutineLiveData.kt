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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference

interface LiveDataScope<T> {
    // suspends until value is dispatched on the Main dispatcher
    suspend fun yield(out: T)
    suspend fun yieldSource(source: LiveData<T>)
    suspend fun currentValue(): T?
}

internal class LiveDataScopeImpl<T>(
    private var target: CoroutineLiveData<T>,
    context: CoroutineContext
) : LiveDataScope<T> {
    val coroutineContext = context + Dispatchers.Main
    override suspend fun yieldSource(source: LiveData<T>) = withContext(Dispatchers.Main) {
        target.yieldSource(source)
    }

    override suspend fun currentValue() = withContext(Dispatchers.Main) {
        target.value
    }

    override suspend fun yield(out: T) = withContext(coroutineContext) {
        target.clearYieldedSource()
        target.value = out
    }
}

internal typealias Block<T> = suspend LiveDataScope<T>.() -> Unit

internal class CoroutineLiveData<T>(
    private val context: CoroutineContext = EmptyCoroutineContext,
    private val timeoutInMs: Long = 5000,
    block: Block<T>
) : MediatorLiveData<T>() {
    // use an intermediate supervisor job so that if we cancel inidividual runs due to losing
    // observers, it won't cancel the given context as we only cancel w/ the intention of possibly
    // relaunching using the same context
    private val supervisorJob = SupervisorJob(context[Job])
    // this is nullable so that we can clear the block reference once done
    // MainThread confined
    private var block: Block<T>? = block
    // currently running job, if exists.
    private var runningJob: Job? = null
    // the deferred action that will try to cancel the runningJob. This action gets cancelled
    // if LiveData re-activates within the timeout.
    private var cancellationAction: Job? = null

    // the main scope for this LiveData where we launch every block Job. Having this intermediate
    // supervisor avoids cancelling the build in context when we just want to cancel the block
    // due to LiveData inactivity.
    private val scope = CoroutineScope(Dispatchers.Main + context + supervisorJob)

    // the source we are delegated to, sent from LiveDataScope#yieldSource
    private var yieldedSource: LiveData<T>? = null

    @MainThread
    internal fun yieldSource(source: LiveData<T>) {
        clearYieldedSource()
        yieldedSource = source
        addSource(source) {
            value = it
        }
    }

    @MainThread
    internal fun clearYieldedSource() {
        yieldedSource?.let {
            removeSource(it)
            yieldedSource = null
        }
    }

    override fun onActive() {
        super.onActive()
        if (isDone()) {
            return
        }
        val pendingCancellation = cancellationAction
        cancellationAction = null
        pendingCancellation?.cancel()
        val prevJob = runningJob
        if (prevJob?.isCancelled == false) {
            return // already have a running job, let it run
        }
        runningJob = scope.launch {
            // join on the previous one to avoid running in parallel
            prevJob?.cancelAndJoin()
            val theBlock = block
            if (theBlock == null) {
                return@launch
            }
            // create a LiveDataScope with the current launch's context to handle cancellation
            // in yield
            val liveDataScope = LiveDataScopeImpl(this@CoroutineLiveData, coroutineContext)
            theBlock(liveDataScope)
            // once block is done successfully, go to the main dispatcher and null it to mark this
            // as done.
            withContext(Dispatchers.Main) {
                block = null
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (isDone()) {
            return
        }
        cancellationAction = scope.launch(Dispatchers.Main) {
            delay(timeoutInMs)
            runningJob?.cancel()
            runningJob = null
            cancellationAction = null
        }
    }

    @Suppress("NOTHING_TO_INLINE") // save a method
    @MainThread
    internal inline fun isDone() = block == null
}

/**
 * Builds a LiveData that has values yielded from the given [block] that executes on a
 * [LiveDataScope].
 *
 * The [block] starts executing when the returned [LiveData] becomes active ([LiveData.onActive]).
 * If the [LiveData] becomes inactive ([LiveData.onInactive]) while the [block] is executing, it
 * will be cancelled after [timeoutInMs] milliseconds unless the [LiveData] becomes active again
 * before that timeout. Any value [LiveDataScope.yield]ed from a cancelled [block] will be ignored.
 *
 * After a cancellation, if the [LiveData] becomes active again, the [block] will be re-executed
 * from the beginning. If you would like to continue the opeartions based on where it was stopped
 * last, you can use the [LiveDataScope.currentValue] function to get the last
 * [LiveDataScope.yield]ed value.
 *
 * If the [block] completes successfully *or* is cancelled due to reasons other than [LiveData]
 * becoming inactive, it *will not* be re-executed even after [LiveData] goes through active
 * inactive cycle.
 *
 * As a best practice, it is important for the [block] to cooperate in cancellation. See kotlin
 * coroutines documentation for details
 * https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html.
 *
 * ```
 * // a simple LiveData that receives value 3, 3 seconds after being observed for the first time.
 * val liveData : LiveData<Int> = buildLiveData {
 *     delay(3000)
 *     yield(3)
 * }
 *
 * // a LiveData that fetches a `User` object based on a `userId` and refreshes it every 30 seconds
 * // as long as it is observed
 * val userId : LiveData<String> = ...
 * val user = userId.switchMap { id ->
 *     while(true) {
 *     // note that `while(true)` is fine because the `delay(30_000)` below will cooperate in
 *     // cancellation if LiveData is not actively observed anymore
 *         val data = api.fetch(id) // errors are ignored for brevity
 *         yield(data)
 *         delay(30_000)
 *     }
 * }
 *
 * // a LiveData that tries to load the `User` from local cache first and then tries to fetch
 * // from the server and also yields the updated value
 * val user = buildLiveData {
 *     // dispatch loading first
 *     yield(LOADING(id))
 *     // check local storage
 *     val cached = cache.loadUser(id)
 *     if (cached != null) {
 *         yield(cached)
 *     }
 *     if (cached == null || cached.isStale()) {
 *         val fresh = api.fetch(id) // errors are ignored for brevity
 *         cache.save(fresh)
 *         yield(fresh)
 *     }
 * }
 *
 * // a LiveData that immediately receives a LiveData<User> from the database and yields it as a
 * // source but also tries to update from the server
 * val user = buildLiveData {
 *     val fromDb : LiveData<User> = roomDatabase.loadUser(id)
 *     yieldSource(fromDb)
 *     val updated = api.fetch(id) // errors are ignored for brevity
 *     // Since we are using Room here, updating the database will update the `fromDb` LiveData
 *     // that was obtained above. See Room's documentation for more details.
 *     // https://developer.android.com/training/data-storage/room/accessing-data#query-observable
 *     roomDatabase.insert(updated)
 * }
 * ```
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T> buildLiveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = 5000,
    @BuilderInference block: suspend LiveDataScope<T>.() -> Unit
): LiveData<T> = CoroutineLiveData(context, timeoutInMs, block)