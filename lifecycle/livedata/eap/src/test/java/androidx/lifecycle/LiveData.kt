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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// these are probably good candidates to be released as a testing library

// skips current one and gets the next one
suspend fun <T> LiveData<T>.nextValue(
    timeout: Long = 5000L
): T? =
    withContext(Dispatchers.Main) {
        val startVersion = version
        val def = CompletableDeferred<T>()
        val observer = Observer<T> { t ->
            if (version > startVersion) {
                def.complete(t)
            }
        }
        try {
            observeForever(observer)
            withTimeoutOrNull(timeout) {
                def.await()
            }
        } finally {
            removeObserver(observer)
        }
    }

suspend fun <T> LiveData<T>.collect(
    capacity: Int,
    timeout: Long = 5000L
): List<T> = collectAsync(capacity, timeout).await()

suspend fun <T> LiveData<T>.collectAsync(
    capacity: Int,
    timeout: Long = 5000L
): Deferred<List<T>> =
    withContext(Dispatchers.Main) {
        val done = CompletableDeferred<List<T>>()
        val res = mutableListOf<T>()
        val observer = object : Observer<T> {
            override fun onChanged(t: T) {
                res.add(t)
                if (res.size == capacity) {
                    removeObserver(this) // avoid adding more
                    done.complete(res)
                }
            }
        }
        observeForever(observer)
        val cancellation = CoroutineScope(coroutineContext + Job() + Dispatchers.Main).launch {
            delay(timeout)
            if (done.isActive) {
                removeObserver(observer)
                done.completeExceptionally(
                    IllegalStateException("didn't receive $capacity values. Received $res")
                )
            }
        }
        done.invokeOnCompletion { error ->
            if (error == null) {
                cancellation.cancel()
            }
        }
        done
    }