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
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

@RunWith(JUnit4::class)
class BuildLiveDataTest : CoroutineTestBase() {
    @Test
    @LargeTest
    fun oneShot() {
        val started = CountDownLatch(1)
        val liveData = buildLiveData {
            yield(3)
        }
        started.assertFalse()
        runTest {
            assertThat(liveData.collect(1)).containsExactly(3)
        }
    }

    @Test
    @LargeTest
    fun multipleValuesAndObservers() {
        runTest {
            val ld = buildLiveData {
                yield(3)
                yield(4)
            }
            assertThat(ld.collect(2)).containsExactly(3, 4)
            // re-observe and get the latest value only
            assertThat(ld.collect(1)).containsExactly(4)
            assertThat(ld.nextValue(1000)).isNull()
        }
    }

    @Test
    @LargeTest
    fun removeObserverInBetween() {
        runTest {
            val ld = buildLiveData(timeoutInMs = 10) {
                yield(1)
                yield(2)
                delay(1000)
                yield(3)
            }
            assertThat(ld.collect(2)).containsExactly(1, 2)
            delay(500) // trigger cancellation
            // expect a restart due to cancellation
            assertThat(ld.collect(4)).containsExactly(2, 1, 2, 3)
        }
    }

    @Test
    @LargeTest
    fun removeObserverInBetween_largeTimeout() {
        runTest {
            val ld = buildLiveData(timeoutInMs = 10000) {
                yield(1)
                yield(2)
                delay(1000)
                yield(3)
            }
            assertThat(ld.collect(2)).containsExactly(1, 2)
            // wait in between is not large enough to trigger cancellation
            assertThat(ld.collect(2)).containsExactly(2, 3)
        }
    }

    @Test
    @LargeTest
    fun ignoreCancelledYields() {
        runTest {
            val cancelled = CountDownLatch(1)
            val continuedAfterYield = CountDownLatch(1)
            val ld = buildLiveData(timeoutInMs = 0, context = Dispatchers.IO) {
                yield(1)
                cancelled.assertTrue()
                yield(2)
                continuedAfterYield.countDown()
            }
            assertThat(ld.collect(1)).containsExactly(1)
            cancelled.countDown()
            continuedAfterYield.assertFalse()
            // ensure 2 is not set as value
            assertThat(ld.value).isEqualTo(1)
            // now because it was cancelled, re-observing should dispatch 1,1,2
            assertThat(ld.collect(3)).containsExactly(1, 1, 2)
        }
    }

    @Test
    fun readCurrentValue() {
        runTest {
            val currentValue = CompletableDeferred<Int>()
            val ld = buildLiveData<Int>(Dispatchers.IO) {
                currentValue.complete(currentValue()!!)
            }
            ld.value = 3
            ld.observeForever { }
            assertThat(currentValue.await()).isEqualTo(3)
        }
    }

    @Test
    fun yieldSource_simple() {
        runTest {
            val odds = buildLiveData {
                (1..9 step 2).forEach {
                    yield(it)
                }
            }
            val ld = buildLiveData {
                yieldSource(odds)
            }
            assertThat(ld.collect(5)).containsExactly(1, 3, 5, 7, 9)
        }
    }

    @Test
    fun yieldSource_switchTwo() {
        runTest {
            val doneOddsYield = CountDownLatch(1)
            val odds = buildLiveData(timeoutInMs = 0) {
                (1..9 step 2).forEach {
                    yield(it)
                }
                doneOddsYield.countDown()
                delay(2000)
                error("should've not reached here")
            }
            val evens = buildLiveData {
                (2..10 step 2).forEach {
                    yield(it)
                }
            }
            val ld = buildLiveData(Dispatchers.IO) {
                yieldSource(odds)
                doneOddsYield.assertTrue()
                yieldSource(evens)
            }
            assertThat(ld.collect(10))
                .containsExactly(1, 3, 5, 7, 9, 2, 4, 6, 8, 10)
        }
    }

    @Test
    fun yieldSource_yieldValue() {
        runTest {
            val doneOddsYield = CountDownLatch(1)
            val odds = buildLiveData(timeoutInMs = 0) {
                (1..9 step 2).forEach {
                    yield(it)
                }
                doneOddsYield.countDown()
                delay(2000)
                error("should've not reached here")
            }
            val ld = buildLiveData(Dispatchers.IO) {
                yieldSource(odds)
                doneOddsYield.assertTrue()
                yield(10)
            }
            assertThat(ld.collect(6))
                .containsExactly(1, 3, 5, 7, 9, 10)
        }
    }

    @Test
    @MediumTest
    fun blockThrows() {
        val exception = CompletableDeferred<Throwable>()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.complete(throwable)
        }
        val unexpected = CountDownLatch(1)
        val ld = buildLiveData(Dispatchers.IO + exceptionHandler, 10) {
            if (exception.isActive) {
                throw IllegalArgumentException("i like to fail")
            } else {
                unexpected.countDown()
                yield(3)
            }
        }
        runTest {
            val obs1 = Observer<Int> {}
            ld.observeForever(obs1)
            assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            ld.removeObserver(obs1)
            delay(100) // to ensure cancelation
            unexpected.assertFalse()
        }
    }

    @Test
    @MediumTest
    fun blockCancelsItself() {
        val didCancel = CountDownLatch(1)
        val unexpected = CountDownLatch(1)

        val ld = buildLiveData<Int>(Dispatchers.IO, 10) {
            if (didCancel.count == 1L) {
                didCancel.countDown()
                coroutineContext.cancel()
            } else {
                unexpected.countDown()
            }
        }
        runTest {
            val obs1 = Observer<Int> {}
            ld.observeForever(obs1)
            didCancel.assertTrue()
            ld.removeObserver(obs1)
            delay(100) // to ensure cancelation
            ld.observeForever(obs1)
            unexpected.assertFalse()
        }
    }

    @Test
    @MediumTest
    fun blockThrows_switchMap() {
        val exception = CompletableDeferred<Throwable>()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.complete(throwable)
        }
        val src = MutableLiveData<Int>()
        val ld = src.switchMap {
            buildLiveData(Dispatchers.IO + exceptionHandler) {
                if (exception.isActive) {
                    throw IllegalArgumentException("i like to fail")
                } else {
                    yield(3)
                }
            }
        }
        runTest {
            val items = ld.collectAsync(1)
            src.value = 1
            assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            src.value = 2
            assertThat(items.await()).containsExactly(3)
        }
    }

    private fun CountDownLatch.assertFalse(
        timeout: Long = 1,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ) {
        assertThat(await(timeout, timeUnit)).isFalse()
    }

    private fun CountDownLatch.assertTrue(
        timeout: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ) {
        assertThat(await(timeout, timeUnit)).isTrue()
    }
}