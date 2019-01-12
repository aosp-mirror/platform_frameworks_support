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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * [CoroutineScope] tied to this [Lifecycle].
 *
 * This scope will be canceled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to  [Dispatchers.Main]
 */
val Lifecycle.lifecycleScope: CoroutineScope
    get() {
        while (true) {
            val existing = mInternalScopeRef.get() as? LifecycleCoroutineScope
            if (existing != null) {
                return existing
            }
            val newScope = LifecycleCoroutineScope(
                this,
                Job() + Dispatchers.Main
            )
            if (mInternalScopeRef.compareAndSet(null, newScope)) {
                newScope.register()
                return newScope
            }
        }
    }

internal class LifecycleCoroutineScope(
    private val lifecycle: Lifecycle,
    context: CoroutineContext
) : CoroutineScope, LifecycleEventObserver {
    override val coroutineContext: CoroutineContext = context

    init {
        // in case we are initialized on a non-main thread, make a best effort check before
        // we return the scope. This is not sync but if developer is launching on a non-main
        // dispatcher, they cannot be 100% sure anyways.
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            coroutineContext.cancel()
        }
    }

    fun register() {
        launch(Dispatchers.Main) {
            if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
                lifecycle.addObserver(this@LifecycleCoroutineScope)
            } else {
                coroutineContext.cancel()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (lifecycle.currentState <= Lifecycle.State.DESTROYED) {
            lifecycle.removeObserver(this)
            coroutineContext.cancel()
        }
    }
}

/**
 *
 */
suspend fun <OUT> withStarted(lifecycle: Lifecycle, block: () -> OUT): OUT =
    withStateAtLeast(
        lifecycle = lifecycle,
        state = Lifecycle.State.STARTED,
        block = block
    )

/**
 * Runs the given [block] on [Dispatchers.Main] when the [Lifecycle] is at least in the
 * given [state] state. If the [Lifecycle] is already destroyed, this method will throw
 * [LifecycleDestroyedException] which is a specific type of [CancellationException].
 *
 * If the [Lifecycle] is less than the given [state], this method will suspend until the
 * [Lifecycle] moves to that [state].
 * If the [Lifecycle] moves to the [Lifecycle.State.DESTROYED] state, this method will fail with
 * [LifecycleDestroyedException].
 *
 * You can call this method in any thread but the provided [block] will always be run on
 * [Dispatchers.Main].
 *
 * @param lifecycle The lifecycle to attach to
 * @param state The target state. Providing [Lifecycle.State.DESTROYED] will throw an
 * [IllegalArgumentException]
 * @param block The code to run when the [Lifecycle] is at least in the desired state.
 * @return The value returned by the [block]
 */
suspend fun <OUT> withStateAtLeast(
    lifecycle: Lifecycle,
    state: Lifecycle.State,
    block: () -> OUT
): OUT =
    withContext(Dispatchers.Main) {
        require(state > Lifecycle.State.DESTROYED) {
            "The state must be at least Lifecycle.State.INITIALIZED. Given $state"
        }
        when {
            lifecycle.currentState >= state -> {
                // fast path
                block()
            }
            lifecycle.currentState == Lifecycle.State.DESTROYED -> {
                throw LifecycleDestroyedException()
            }
            else -> suspendCancellableCoroutine<OUT> { cancellable ->
                lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (source.lifecycle.currentState >= state) {
                            lifecycle.removeObserver(this)
                            cancellable.resumeWith(
                                try {
                                    Result.success(block())
                                } catch (t: Throwable) {
                                    Result.failure<OUT>(t)
                                }
                            )
                        } else if (event == Lifecycle.Event.ON_DESTROY) {
                            lifecycle.removeObserver(this)
                            cancellable.cancel(LifecycleDestroyedException())
                        }
                    }
                })
            }
        }
    }

/**
 * Thrown when a coroutine is cancelled because it is scoped to a [Lifecycle] that got destroyed.
 */
class LifecycleDestroyedException : CancellationException(
    "Attached Lifecycle is destroyed"
)