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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@InternalCoroutinesApi
@SmallTest
@RunWith(AndroidJUnit4::class)
class PausingDispatcherTest {
    val handler = Handler(Looper.getMainLooper())
    val testingScope = CoroutineScope(Dispatchers.Default + Job(null))
    val owner = FakeLifecycleOwner(Lifecycle.State.RESUMED)

    @ExperimentalCoroutinesApi
    @Before
    fun updateMainHandlerAndDispatcher() {
        setMainHandler(handler)
        Dispatchers.setMain(handler.asCoroutineDispatcher())
    }

    @ExperimentalCoroutinesApi
    @After
    fun clenarHandlerAndDispatcher() {
        Dispatchers.resetMain()
        assertThat(handler.looper.queue.isIdle).isTrue()
        resetMainHandler()
        runBlocking {
            testingScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun basic() {
        val result = runBlocking {
            owner.whenResumed {
                assertThread()
                log("will return 3")
                3
            }
        }
        assertThat(result).isEqualTo(3)
    }

    @Test
    fun moveToAnotherDispatcher() {
        val result = runBlocking {
            owner.whenResumed {
                assertThread()
                val innerResult = withContext(Dispatchers.IO) {
                    log("running inner")
                    "hello"
                }
                assertThread()
                log("received inner result $innerResult")
                innerResult + innerResult
            }
        }
        assertThat(result).isEqualTo("hellohello")
    }

    @Test
    fun cancel() {
        val didRun = AtomicBoolean(false)
        val didRunFinally = AtomicBoolean(false)
        val startedRunning = CountDownLatch(1)
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    try {
                        startedRunning.countDown()
                        delay(5000)
                        didRun.set(true)
                    } finally {
                        didRunFinally.set(true)
                    }
                }
            }
            startedRunning.assertTrue()
            job.cancelAndJoin()
            assertThat(didRun.get()).isEqualTo(false)
            assertThat(didRunFinally.get()).isEqualTo(true)
        }
    }

    @Test
    fun throwException_thenRunAnother() {
        val didRunSecond = AtomicBoolean(false)
        runBlocking {
            withContext(testingScope.coroutineContext) {
                try {
                    owner.whenResumed {
                        assertThread()
                        throw IllegalArgumentException(" fail")
                    }
                    @Suppress("UNREACHABLE_CODE")
                    throw AssertionError("should've not run")
                } catch (ignored: IllegalArgumentException) {
                }
                owner.whenResumed {
                    didRunSecond.set(true)
                }
            }
        }
        assertThat(didRunSecond.get()).isTrue()
    }

    @Test
    fun innerThrowException() {
        val didRun = AtomicBoolean(false)
        val didRunFinally = AtomicBoolean(false)
        val startedRunning = CountDownLatch(1)
        runBlocking {
            val job = testingScope.launch {
                val res = runCatching {
                    owner.whenResumed {
                        try {
                            startedRunning.countDown()
                            withContext(Dispatchers.IO) {
                                throw IllegalStateException("i fail")
                            }
                        } finally {
                            didRunFinally.set(true)
                        }
                        @Suppress("UNREACHABLE_CODE")
                        didRun.set(true)
                    }
                }
                assertThat(res.exceptionOrNull()).hasMessageThat().isEqualTo("i fail")
            }
            job.join()
            startedRunning.assertTrue()
            assertThat(didRun.get()).isEqualTo(false)
            assertThat(didRunFinally.get()).isEqualTo(true)
        }
    }

    @Test
    @LargeTest
    fun pause_thenResume() {
        pause()
        val didRun = CountDownLatch(1)
        testingScope.launch {
            owner.whenResumed {
                didRun.countDown()
            }
        }
        didRun.assertFalse()
        resume()
        didRun.assertTrue()
    }

    @Test
    @LargeTest
    fun pause_thenFinish() {
        val didRunFinally = CountDownLatch(1)
        pause()
        val didRun = CountDownLatch(1)
        testingScope.launch {
            owner.whenResumed {
                try {
                    didRun.countDown()
                } finally {
                    didRunFinally.countDown()
                }
            }
        }
        didRun.assertFalse()
        finish()
        didRun.assertFalse()
        // never started so shouldn't run finally either
        didRunFinally.assertFalse()
    }

    @Test
    @LargeTest
    fun finishWhileDelayed() {
        val didRunFinally = CountDownLatch(1)
        val didSucceed = CountDownLatch(1)
        val didRun = CountDownLatch(1)

        testingScope.launch {
            owner.whenResumed {
                try {
                    didRun.countDown()
                    delay(100000)
                    didSucceed.countDown()
                } finally {
                    assertThat(isActive).isFalse()
                    didRunFinally.countDown()
                }
            }
        }
        didRun.assertTrue()
        finish()
        didRunFinally.assertTrue()
        didSucceed.assertFalse()
    }

    @Test
    @LargeTest
    fun catchFinishWhileDelayed() {
        val didRunFinally = CountDownLatch(1)
        val didSucceed = CountDownLatch(1)
        val didRun = CountDownLatch(1)
        val didCatch = CountDownLatch(1)

        testingScope.launch {
            owner.whenResumed {
                try {
                    didRun.countDown()
                    delay(100000)
                    didSucceed.countDown()
                } catch (e: Exception) {
                    didCatch.countDown()
                    assertThat(isActive).isFalse()
                } finally {
                    didRunFinally.countDown()
                }
            }
        }
        didRun.assertTrue()
        finish()
        didCatch.assertTrue()
        didRunFinally.assertTrue()
        didSucceed.assertFalse()
    }

    @Test
    @LargeTest
    fun pauseThenContinue() {
        val firstRun = CountDownLatch(1)
        val secondRun = CountDownLatch(1)
        testingScope.launch {
            owner.whenResumed {
                firstRun.countDown()
                withContext(Dispatchers.IO) {
                    pause()
                }
                secondRun.countDown()
            }
        }
        firstRun.assertTrue()
        secondRun.assertFalse()
        resume()
        secondRun.assertTrue()
    }

    @Test
    @LargeTest
    fun dontRunIfPausedAfterDelay() {
        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        testingScope.launch {
            owner.whenResumed {
                started.countDown()
                delay(1500)
                finished.countDown()
            }
        }
        started.assertTrue()
        pause()
        finished.assertFalse()
        resume()
        finished.assertTrue()
    }

    @Test
    @LargeTest
    fun parentJobCancelled() {
        runBlocking {
            val started = CountDownLatch(1)
            val notExpected = CountDownLatch(1)
            val runFinally = CountDownLatch(1)

            val parent = testingScope.launch {
                owner.whenResumed {
                    try {
                        started.countDown()
                        delay(10000)
                        notExpected.countDown()
                    } finally {
                        runFinally.countDown()
                    }
                }
            }
            started.assertTrue()
            parent.cancelAndJoin()
            notExpected.assertFalse()
            runFinally.assertTrue()
        }
    }

    @Test
    fun innerJobCancelsParent() {
        try {
            runBlocking(testingScope.coroutineContext) {
                owner.whenResumed {
                    throw IllegalStateException("i fail")
                }
            }
            @Suppress("UNREACHABLE_CODE")
            throw AssertionError("should've failed")
        } catch (ex: IllegalStateException) {
            assertThat(ex).hasMessageThat().isEqualTo("i fail")
        }
    }

    @Test
    fun innerJobCancelsParent_catchInSupervisor() {
        try {
            runBlocking {
                supervisorScope {
                    withContext(testingScope.coroutineContext) {
                        owner.whenResumed {
                            throw IllegalStateException("i fail")
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            throw AssertionError("should've not run")
        } catch (ex: IllegalStateException) {
            assertThat(ex).hasMessageThat().isEqualTo("i fail")
        }
    }

    @Test
    fun lifecycleInsideLifecycle() {
        val runs = arrayOf(CountDownLatch(1), CountDownLatch(1))
        runBlocking {
            owner.whenResumed {
                assertThread()
                runs[0].countDown()
                owner.whenResumed {
                    assertThread()
                    runs[1].countDown()
                }
            }
        }
        runs.assertTrue()
    }

    @Test
    @LargeTest
    fun lifecycleInsideLifecycle_innerFails() {
        val starts = arrayOf(CountDownLatch(1), CountDownLatch(1))
        val dones = arrayOf(CountDownLatch(1), CountDownLatch(1))
        val runFinallies = arrayOf(CountDownLatch(1), CountDownLatch(1))
        runBlocking {
            val res = runCatching {
                owner.whenResumed {
                    try {
                        starts[0].countDown()
                        assertThread()
                        owner.whenResumed {
                            starts[1].countDown()
                            assertThread()
                            try {
                                withContext(testingScope.coroutineContext) {
                                    throw IllegalStateException("i fail")
                                }
                                @Suppress("UNREACHABLE_CODE")
                                dones[1].countDown()
                            } finally {
                                runFinallies[1].countDown()
                            }
                        }
                        dones[1].countDown()
                    } finally {
                        runFinallies[0].countDown()
                    }
                }
            }
            assertThat(res.exceptionOrNull()).hasMessageThat().matches("i fail")
        }
        starts.assertTrue()
        runFinallies.assertTrue()
        dones.assertFalse()
    }

    private fun pause() {
        runOnHandlerSync {
            owner.setState(Lifecycle.State.CREATED)
        }
    }

    private fun finish() {
        runOnHandlerSync {
            owner.setState(Lifecycle.State.DESTROYED)
        }
    }

    private fun resume() {
        runOnHandlerSync {
            owner.setState(Lifecycle.State.RESUMED)
        }
    }

    private fun runOnHandlerSync(f: () -> Unit) {
        val done = CountDownLatch(1)
        handler.post {
            f()
            done.countDown()
        }
        done.assertTrue()
    }

    fun assertThread() {
        log("asserting looper")
        assertThat(Looper.myLooper()).isSameAs(handler.looper)
    }

    private fun Array<CountDownLatch>.assertTrue(seconds: Int = 2) {
        runBlocking {
            map {
                async {
                    it.assertTrue(seconds)
                }
            }.awaitAll()
        }
    }

    private fun Array<CountDownLatch>.assertFalse(seconds: Int = 2) {
        runBlocking {
            map {
                async {
                    it.assertFalse(seconds)
                }
            }.awaitAll()
        }
    }

    private fun CountDownLatch.assertTrue(seconds: Int = 2) {
        assertThat(await(2, TimeUnit.SECONDS)).isTrue()
    }

    private fun CountDownLatch.assertFalse(seconds: Int = 2) {
        assertThat(await(2, TimeUnit.SECONDS)).isFalse()
    }

    fun log(msg: Any?) {
        Log.d("TEST-RUN", "[${Thread.currentThread().name}] $msg")
    }
}