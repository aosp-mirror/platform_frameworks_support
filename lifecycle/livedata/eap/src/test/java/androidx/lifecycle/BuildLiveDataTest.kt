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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    fun multipleValuesAndObservers() {
        runTest {
            val ld = buildLiveData {
                yield(3)
                yield(4)
            }
            assertThat(ld.collect(2)).containsExactly(3, 4)
            // re-observe and get the latest value only
            assertThat(ld.collect(1)).containsExactly(4)
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