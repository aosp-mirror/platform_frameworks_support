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

import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CoroutineLiveDataMapTest : CoroutineTestBase() {
    private val src = MutableLiveData<Int>() // add nullable src tests too
    @Test
    fun oneValue() {
        runTest {
            val ld = src.map(testScope.coroutineContext) {
                "$it"
            }
            val items = ld.collectAsync(1)
            src.value = 1
            assertThat(items.await()).isEqualTo(listOf("1"))
        }
    }

    @Test
    fun twoValues() {
        runTest {
            val ld = src.map(testScope.coroutineContext) {
                ("$it-$it")
            }
            val items = ld.collectAsync(2)
            src.value = 1
            src.value = 2
            assertThat(items.await()).isEqualTo(listOf("1-1", "2-2"))
        }
    }

    @Test
    fun throwException_withHandler_thenSucceed() {
        runTest {
            val exception = CompletableDeferred<Throwable>()
            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                exception.complete(throwable)
            }
            var shouldThrow = true
            val ld =
                src.switchMap(testScope.coroutineContext + exceptionHandler) {
                    if (shouldThrow) {
                        shouldThrow = false
                        throw IllegalArgumentException("fail i should")
                    } else {
                        yield("hello $it")
                    }
                }
            val items = ld.collectAsync(1)
            src.value = 1
            // ensure we dispatch the exception to the launching coroutine context
            assertThat(exception.await()).hasMessageThat().contains("fail i should")
            src.value = 2
            assertThat(items.await()).isEqualTo(listOf("hello 2"))
        }
    }

    @Test
    fun multiLevel() {
        runTest {
            val liveData = src.switchMap(testScope.coroutineContext) {
                yield(it * it)
            }.switchMap(Dispatchers.IO) {
                yield("$it-$it")
            }
            val items = liveData.collectAsync(3)
            (0 until 3).forEach {
                val next = liveData.nextValueAsync()
                src.value = it
                next.await()
            }
            assertThat(items.await()).isEqualTo(
                listOf("0-0", "1-1", "4-4")
            )
        }
    }

    @Test
    @LargeTest
    fun cancelScope() {
        runTest {
            val myScope = CoroutineScope(Job())
            val liveData = src.switchMap(myScope.coroutineContext) {
                yield("$it-$it")
            }
            val items = liveData.collectAsync(capacity = 1, timeout = 1000)
            myScope.coroutineContext.cancel()
            src.value = 3
            val result = runCatching {
                items.await()
            }
            assertThat(result.exceptionOrNull()).isNotNull()
        }
    }

    @Test
    @LargeTest
    fun cancelScope_whileRunning() {
        runTest {
            val myScope = CoroutineScope(Job())
            val liveData = src.switchMap(myScope.coroutineContext) {
                delay(1000)
                yield("$it-$it")
            }
            val items = liveData.collectAsync(capacity = 1, timeout = 2000)
            src.value = 3
            myScope.coroutineContext.cancel()
            val result = runCatching {
                items.await()
            }
            assertThat(result.exceptionOrNull()).isNotNull()

            src.value = 4
            val items2 = liveData.collectAsync(capacity = 1, timeout = 2000)

            val result2 = runCatching {
                items2.await()
            }
            assertThat(result2.exceptionOrNull()).isNotNull()
        }
    }

    @Test
    fun ignoreYieldFromPrevious() {
        runTest {
            val prevScope = CompletableDeferred<LiveDataScope<String>>()
            val liveData = src.switchMap(testScope.coroutineContext) {
                if (!prevScope.isCompleted) {
                    prevScope.complete(this)
                } else {
                    yield("$it")
                }
            }
            val expected = liveData.collectAsync(1)
            src.value = 1
            prevScope.await()
            src.value = 2
            assertThat(expected.await()).containsExactly("2")
            val unexpected = liveData.nextValueAsync()
            prevScope.await().yield("bad")
            val res = kotlin.runCatching {
                unexpected.await()
            }
            assertThat(res.exceptionOrNull()).isInstanceOf(TimeoutCancellationException::class.java)
        }
    }
}