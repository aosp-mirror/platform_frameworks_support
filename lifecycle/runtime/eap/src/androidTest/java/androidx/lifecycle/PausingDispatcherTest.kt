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
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@InternalCoroutinesApi
@SmallTest
@RunWith(Parameterized::class)
class PausingDispatcherTest internal constructor(
    internal val testTarget: TestTarget
) {
    val handler = Handler(Looper.getMainLooper())

    val testingScope = CoroutineScope(Dispatchers.Default + Job(null))
    val lifecycleOwner = FakeLifecycleOwner(Lifecycle.State.RESUMED)

    init {
        testTarget.handler = handler
        testTarget.lifecycle = lifecycleOwner.lifecycle
    }

    @After
    fun checkEmptyHandler() {
        assertThat(handler.looper.queue.isIdle).isTrue()
        runBlocking {
            testingScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun basic() {
        val result = runBlocking {
            whenResumed {
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
            whenResumed {
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
                whenResumed {
                    try {
                        startedRunning.countDown()
                        delay(5000)
                        didRun.set(true)
                    } finally {
                        didRunFinally.set(true)
                    }
                }
            }
            assertThat(
                startedRunning.await(5, TimeUnit.SECONDS)
            ).isTrue()
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
                    whenResumed {
                        assertThread()
                        throw IllegalArgumentException(" fail")
                    }
                    @Suppress("UNREACHABLE_CODE")
                    throw AssertionError("should've not run")
                } catch (ignored: IllegalArgumentException) {
                }
                whenResumed {
                    didRunSecond.set(true)
                }
            }
        }
        assertThat(didRunSecond.get()).isTrue()
    }

    @Test
    fun innerThrowException() {
        if (testTarget !is CoroutineDispatcherTarget) {
            throw AssumptionViolatedException("interceptor crashes the app right now")
        }
        val didRun = AtomicBoolean(false)
        val didRunFinally = AtomicBoolean(false)
        val startedRunning = CountDownLatch(1)
        runBlocking {
            val job = testingScope.launch {
                whenResumed {
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
            assertThat(
                startedRunning.await(5, TimeUnit.SECONDS)
            ).isTrue()
            job.cancelAndJoin()
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
            whenResumed {
                didRun.countDown()
            }
        }
        assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
        resume()
        assertThat(didRun.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LargeTest
    fun pause_thenFinish() {
        val didRunFinally = CountDownLatch(1)
        pause()
        val didRun = CountDownLatch(1)
        testingScope.launch {
            whenResumed {
                try {
                    didRun.countDown()
                } finally {
                    didRunFinally.countDown()
                }
            }
        }
        assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
        finish()
        assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
        // never started so shouldn't run finally either
        assertThat(didRunFinally.await(1, TimeUnit.SECONDS)).isFalse()
    }

    @Test
    fun finishWhileDelayed() {
        val didRunFinally = CountDownLatch(1)
        val didRun = CountDownLatch(1)

        testingScope.launch {
            whenResumed {
                try {
                    didRun.countDown()
                    delay(100000)
                } finally {
                    didRunFinally.countDown()
                }
            }
        }
        assertThat(didRun.await(1, TimeUnit.SECONDS)).isTrue()
        finish()
        assertThat(didRunFinally.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LargeTest
    fun pauseThenContinue() {
        val firstRun = CountDownLatch(1)
        val secondRun = CountDownLatch(1)
        testingScope.launch {
            whenResumed {
                firstRun.countDown()
                withContext(Dispatchers.IO) {
                    pause()
                }
                secondRun.countDown()
            }
        }
        assertThat(firstRun.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(secondRun.await(1, TimeUnit.SECONDS)).isFalse()
        resume()
        assertThat(secondRun.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LargeTest
    fun dontRunIfPausedAfterDelay() {
        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        testingScope.launch {
            whenResumed {
                started.countDown()
                delay(1500)
                finished.countDown()
            }
        }

        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue()
        pause()
        assertThat(finished.await(3, TimeUnit.SECONDS)).isFalse()
        resume()
        assertThat(finished.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LargeTest
    fun parentJobCancelled() {
        runBlocking {
            val started = CountDownLatch(1)
            val notExpected = CountDownLatch(1)
            val runFinally = CountDownLatch(1)

            val parent = testingScope.launch {
                whenResumed {
                    try {
                        started.countDown()
                        delay(10000)
                        notExpected.countDown()
                    } finally {
                        runFinally.countDown()
                    }
                }
            }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
            parent.cancelAndJoin()
            assertThat(notExpected.await(2, TimeUnit.SECONDS)).isFalse()
            assertThat(runFinally.await(1, TimeUnit.SECONDS)).isTrue()
            assertThat(notExpected.count).isEqualTo(1)
        }
    }

    @Test
    fun innerJobCancelsParent() {
        try {
            runBlocking(testingScope.coroutineContext) {
                whenResumed {
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
                        whenResumed {
                            throw IllegalStateException("i fail")
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            throw AssertionError("should've not run")
        } catch (ex : IllegalStateException) {
            assertThat(ex).hasMessageThat().isEqualTo("i fail")
        }

    }

    private fun pause() {
        runOnHandlerSync {
            lifecycleOwner.setState(Lifecycle.State.CREATED)
        }
    }

    private fun finish() {
        runOnHandlerSync {
            lifecycleOwner.setState(Lifecycle.State.DESTROYED)
        }
    }

    private fun resume() {
        runOnHandlerSync {
            lifecycleOwner.setState(Lifecycle.State.RESUMED)
        }
    }

    private suspend fun <T> whenResumed(
        block: suspend CoroutineScope.() -> T
    ) = testTarget.whenResumed(block)

    private fun runOnHandlerSync(f: () -> Unit) {
        val done = CountDownLatch(1)
        handler.post {
            f()
            done.countDown()
        }
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue()
    }

    fun assertThread() {
        log("asserting looper")
        assertThat(Looper.myLooper()).isSameAs(handler.looper)
    }

    fun log(msg: Any?) {
        Log.d("TEST-RUN", "[${Thread.currentThread().name}] $msg")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "impl:{0}")
        internal fun params(): List<TestTarget> = listOf(
            ContinuationInterceptorTarget(),
            CoroutineDispatcherTarget()
        )
    }

    internal abstract class TestTarget {
        lateinit var handler: Handler
        lateinit var lifecycle: Lifecycle
        abstract suspend fun <T> whenResumed(block: suspend CoroutineScope.() -> T): T
    }

    // TODO these are not good for nested stuff since they all use a fresh new job, gotta use the
    // ues actual APIs to get good results
    internal class ContinuationInterceptorTarget : TestTarget() {
        override suspend fun <T> whenResumed(block: suspend CoroutineScope.() -> T): T {
            return whenResumedInterceptor(handler, lifecycle, block)
        }

        override fun toString() = "PausingContinuationInterceptor"
    }

    internal class CoroutineDispatcherTarget : TestTarget() {
        override suspend fun <T> whenResumed(block: suspend CoroutineScope.() -> T): T {
            return whenResumedDispatcher(handler, lifecycle, block)
        }

        override fun toString() = "PausingDispatcher"
    }
}