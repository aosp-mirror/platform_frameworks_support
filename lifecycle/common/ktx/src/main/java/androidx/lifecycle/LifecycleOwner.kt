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

val LifecycleOwner.lifecycleScope: CoroutineScope
    get() {
        return (this.lifecycle.internalScope as? LifecycleCoroutineScope) ?: SCOPE_LOCK.withLock {
            (this.lifecycle.internalScope as? LifecycleCoroutineScope) ?: LifecycleCoroutineScope(
                this,
                Job() + Dispatchers.Main
            ).also {
                this.lifecycle.internalScope = it
            }
        }
    }

internal class LifecycleCoroutineScope(
    owner: LifecycleOwner,
    context: CoroutineContext
) : CoroutineScope, DefaultLifecycleObserver {
    override val coroutineContext: CoroutineContext = context

    init {
        // in case we are initialized on a non-main thread, make a best effort check before
        // we return the scope. This is not sync but if developer is launching on a non-main
        // dispatcher, they cannot be 100% sure anyways.
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            coroutineContext.cancel()
        } else {
            launch(Dispatchers.Main) {
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                    owner.lifecycle.addObserver(this@LifecycleCoroutineScope)
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

suspend inline fun <OUT> LifecycleOwner.whenStarted(crossinline block: () -> OUT): OUT =
    withContext(Dispatchers.Main) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            // fast path
            block()
        } else if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            throw LifecycleDestroyedException()
        } else {
            suspendCancellableCoroutine<OUT> { cancellable ->
                // check for lifecycle again as we might've been suspended and lifecycle
                // might've changed
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    runCatching(cancellable, block)
                } else if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    cancellable.resumeWithException(LifecycleDestroyedException())
                } else {
                    lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            lifecycle.removeObserver(this)
                            cancellable.cancel(
                                LifecycleDestroyedException()
                            )
                        }
                        override fun onStart(owner: LifecycleOwner) {
                            lifecycle.removeObserver(this)
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

class LifecycleDestroyedException : CancellationException(
    "Attached Lifecycle is destroyed"
)