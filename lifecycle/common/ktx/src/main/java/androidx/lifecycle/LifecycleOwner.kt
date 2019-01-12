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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

private val SCOPE_LOCK = ReentrantLock()

@get:MainThread
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
        runBlocking(Dispatchers.Main) {
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                owner.lifecycle.addObserver(this@LifecycleCoroutineScope)
            } else {
                coroutineContext.cancel()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        coroutineContext.cancel()
    }
}

suspend fun <OUT> LifecycleOwner.whenStarted(f: () -> OUT): OUT = withContext(Dispatchers.Main) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        // fast path
        f()
    } else {
        suspendCancellableCoroutine { cancellable ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                cancellable.resumeWith(Result.success(f()))
            } else if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
                cancellable.resumeWithException(
                    IllegalStateException("lifecycle is already destroyed")
                )
            } else {
                lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        lifecycle.removeObserver(this)
                        cancellable.resumeWithException(
                            IllegalStateException("lifecycle has been destroyed")
                        )
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        lifecycle.removeObserver(this)
                        cancellable.resumeWith(Result.success(f()))
                    }
                })
            }
        }
    }
}