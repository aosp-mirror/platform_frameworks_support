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

package androidx.paging.futures

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits for completion of the future without blocking a thread.
 *
 * This suspending function is cancellable.
 *
 * If the `Job` of the current coroutine is cancelled or completed while this suspending function is
 * waiting, this function stops waiting for the future and immediately resumes with
 * [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * This method is intended to be used with one-shot futures, so on coroutine cancellation future is
 * cancelled as well. If cancelling given future is undesired, `future.asDeferred().await()` should
 * be used instead.
 */
internal suspend fun <T> ListenableFuture<T>.await(): T {
    try {
        if (isDone) return get() as T
    } catch (e: ExecutionException) {
        throw e.cause ?: e // unwrap original cause from ExecutionException
    }

    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        val callback = ContinuationCallback(cont)
        this.addCallback(callback, DirectExecutor)
        cont.invokeOnCancellation {
            cancel(false)
            callback.cont = null // clear the reference to continuation from the future's callback
        }
    }
}

private class ContinuationCallback<T>(@Volatile @JvmField var cont: Continuation<T>?) :
    FutureCallback<T> {
    override fun onSuccess(value: T) {
        cont?.resume(value)
    }

    override fun onError(throwable: Throwable) {
        cont?.resumeWithException(throwable)
    }
}
