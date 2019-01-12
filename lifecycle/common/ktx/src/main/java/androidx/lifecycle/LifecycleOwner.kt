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

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val SCOPE_LOCK = ReentrantLock()

/**
 * [CoroutineScope] tied to this [LifecycleOwner]'s [Lifecycle].
 *
 * This scope will be canceled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to [Dispatchers.Main]
 */
val LifecycleOwner.lifecycleScope: CoroutineScope
    get() = lifecycle.scope

/**
 * [CoroutineScope] tied to this [Lifecycle].
 *
 * This scope will be canceled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to  [Dispatchers.Main]
 */
val Lifecycle.scope: CoroutineScope
    get() {
        return (this.internalScope as? LifecycleCoroutineScope) ?: SCOPE_LOCK.withLock {
            (this.internalScope as? LifecycleCoroutineScope) ?: LifecycleCoroutineScope(
                this,
                Job() + Dispatchers.Main
            ).also {
                this.internalScope = it
            }
        }
    }

internal class LifecycleCoroutineScope(
    lifecycle: Lifecycle,
    context: CoroutineContext
) : CoroutineScope, DefaultLifecycleObserver {
    override val coroutineContext: CoroutineContext = context

    init {
        // in case we are initialized on a non-main thread, make a best effort check before
        // we return the scope. This is not sync but if developer is launching on a non-main
        // dispatcher, they cannot be 100% sure anyways.
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            coroutineContext.cancel()
        } else {
            launch(Dispatchers.Main) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                    lifecycle.addObserver(this@LifecycleCoroutineScope)
                } else {
                    coroutineContext.cancel()
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        coroutineContext.cancel()
    }
}

/**
 * Runs the given [block] on [Dispatchers.Main] when the [LifecycleOwner]'s [Lifecycle] is at
 * least in the [Lifecycle.State.STARTED] state. If the [Lifecycle] is already destroyed, this
 * method will throw [LifecycleDestroyedException] which is a specific type of
 * [CancellationException]. If the [Lifecycle] is not started yet, this
 * method will suspend until the [Lifecycle] starts. If it gets destroyed before it starts, this
 * method will fail with [LifecycleDestroyedException].
 *
 * The provided [block] is always run on the main dispatcher.
 *
 * see [Lifecycle.whenStarted]
 */

suspend inline fun <OUT> LifecycleOwner.whenStarted(
    crossinline block: () ->OUT
) = lifecycle.whenStarted(block)

/**
 * Runs the given [block] on [Dispatchers.Main] when the [Lifecycle] is at least in the
 * [Lifecycle.State.STARTED] state. If the [Lifecycle] is already destroyed, this method will throw
 * [LifecycleDestroyedException] which is a specific type of [CancellationException].
 *
 * If the [Lifecycle] is not started yet, this method will suspend until the [Lifecycle] starts.
 * If it gets destroyed before it starts, this method will fail with [LifecycleDestroyedException].
 *
 * You can call this method in any thread but the provided [block] will always be run on the main
 * dispatcher.
 */
suspend inline fun <OUT> Lifecycle.whenStarted(crossinline block: () -> OUT): OUT =
    withContext(Dispatchers.Main) {
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            // fast path
            block()
        } else if (currentState == Lifecycle.State.DESTROYED) {
            throw LifecycleDestroyedException()
        } else {
            suspendCancellableCoroutine<OUT> { cancellable ->
                // check for lifecycle again as we might've been suspended and lifecycle
                // might've changed
                if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    runCatching(cancellable, block)
                } else if (currentState == Lifecycle.State.DESTROYED) {
                    cancellable.resumeWithException(LifecycleDestroyedException())
                } else {
                    addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            removeObserver(this)
                            cancellable.cancel(
                                LifecycleDestroyedException()
                            )
                        }

                        override fun onStart(owner: LifecycleOwner) {
                            removeObserver(this)
                            runCatching(cancellable, block)
                        }
                    })
                }
            }
        }
    }

@PublishedApi
internal inline fun <OUT> runCatching(
    cancellable: CancellableContinuation<OUT>,
    crossinline block: () -> OUT
) {
    try {
        cancellable.resume(block())
    } catch (t: Throwable) {
        if (cancellable.isActive) {
            cancellable.resumeWithException(t)
        }
    }
}

/**
 * Thrown when a coroutine is cancelled because it is scoped to a [Lifecycle] that got destroyed.
 */
class LifecycleDestroyedException : CancellationException(
    "Attached Lifecycle is destroyed"
)