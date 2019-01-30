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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// these are probably good candidates to be released as a testing library

// skips current one and gets the next one
suspend fun <T> LiveData<T>.nextValueAsync(
    timeout: Long = 5000L
): Deferred<T> =
    withContext(Dispatchers.Main) {
        withTimeout(timeout) {
            val def = CompletableDeferred<T>()
            var registered = false
            val observer = object : Observer<T> {
                override fun onChanged(t: T) {
                    if (registered) {
                        def.complete(t)
                        removeObserver(this)
                    }
                }
            }
            observeForever(observer)
            registered = true
            def
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
        val latch = CountDownLatch(capacity)
        val res = arrayListOf<T>()
        val observer = Observer<T> {
            res.add(it)
            latch.countDown()
        }
        observeForever(observer)
        // TODO avoid global scope here, do we need a test scope ?
        val deferred = GlobalScope.async(Dispatchers.IO) {
            val success = latch.await(timeout, TimeUnit.MILLISECONDS)
            withContext(Dispatchers.Main) {
                removeObserver(observer)
            }
            if (success) {
                res
            } else {
                throw IllegalStateException("didn't receive $capacity values. Received $res")
            }
        }
        deferred
    }