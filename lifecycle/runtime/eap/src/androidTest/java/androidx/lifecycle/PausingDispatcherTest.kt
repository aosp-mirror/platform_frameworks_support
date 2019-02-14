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
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor

@InternalCoroutinesApi
@SmallTest
@RunWith(Parameterized::class)
class PausingDispatcherTest internal constructor(
    internal val testTarget : TestTarget
) {
    val handler = Handler(Looper.getMainLooper())
    val testScope = testTarget.createScope(handler)
    @Test
    fun basic() {
        val result = runTest {
            assertThread()
            log("will return 3")
            3
        }
        log("returned $result")
        assertThat(result).isEqualTo(3)
    }

    @Test
    fun moveToAnotherDispatcher() {
        val result = runTest {
            assertThread()
            val innerResult = withContext(Dispatchers.IO) {
                log("running inner")
                "hello"
            }
            assertThread()
            log("received inner result $innerResult")
            innerResult + innerResult
        }
        assertThat(result).isEqualTo("hellohello")
    }

    @Test
    fun cancel() {
        val didRun = AtomicBoolean(false)
        val didRunFinally = AtomicBoolean(false)
        val startedRunning = CountDownLatch(1)
        runBlocking {
            val job = testScope.launch {
                try {
                    startedRunning.countDown()
                    delay(5000)
                    didRun.set(true)
                } finally {
                    didRunFinally.set(true)
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
            withContext(Dispatchers.IO) {
                try {
                    withContext(testScope.coroutineContext) {
                        assertThread()
                        throw IllegalArgumentException(" fail")
                    }
                    @Suppress("UNREACHABLE_CODE")
                    throw AssertionError("should've not run")
                } catch (ignored: IllegalArgumentException) {
                }
                withContext(testScope.coroutineContext) {
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
            val job = testScope.launch {
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
        runBlocking {
            pause()
            val didRun = CountDownLatch(1)
            testScope.launch {
                didRun.countDown()
            }
            assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
            resume()
            assertThat(didRun.await(1, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    @LargeTest
    fun pause_thenFinish() {
        runBlocking {
            val didRunFinally = CountDownLatch(1)
            pause()
            val didRun = CountDownLatch(1)
            testScope.launch {
                try {
                    didRun.countDown()
                } finally {
                    didRunFinally.countDown()
                }
            }
            assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
            finish()
            assertThat(didRun.await(1, TimeUnit.SECONDS)).isFalse()
            // never started so shouldn't run finally eihther
            assertThat(didRunFinally.await(1, TimeUnit.SECONDS)).isFalse()
        }
    }

    @Test
    fun finishWhileRunning() {
        runBlocking {
            val didRunFinally = CountDownLatch(1)
            val didRun = CountDownLatch(1)
            testScope.launch {
                try {
                    didRun.countDown()
                    delay(10000)
                } finally {
                    didRunFinally.countDown()
                }
            }
            assertThat(didRun.await(1, TimeUnit.SECONDS)).isTrue()
            finish()
            assertThat(didRunFinally.await(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    @LargeTest
    fun pauseThenContinue() {
        runBlocking {
            val firstRun = CountDownLatch(1)
            val secondRun = CountDownLatch(1)
            testScope.launch {
                firstRun.countDown()
                withContext(Dispatchers.IO) {
                    pause()
                }
                secondRun.countDown()
            }
            assertThat(firstRun.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(secondRun.await(1, TimeUnit.SECONDS)).isFalse()
            resume()
            assertThat(secondRun.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    private fun pause() {
        runOnHandlerSync {
            testTarget.pause()
        }
    }

    private fun finish() {
        runOnHandlerSync {
            testTarget.finish()
        }
    }

    private fun resume() {
        runOnHandlerSync {
            testTarget.resume()
        }
    }

    private fun runOnHandlerSync(f : () -> Unit) {
        val done = CountDownLatch(1)
        handler.post {
            f()
            done.countDown()
        }
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue()
    }

    private fun <T> runTest(f: suspend () -> T): T {
        return runBlocking {
            withContext(testScope.coroutineContext) {
                f()
            }
        }
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
            ContinuationInterceptorTarget(), CoroutineDispatcherTarget()
        )
    }

    internal abstract class TestTarget {
        lateinit var stateManager: StateManager<*>
        abstract fun createScope(handler: Handler): CoroutineScope
        fun pause() {
            stateManager.pause()
        }
        fun resume() {
            stateManager.resume()
        }
        fun finish() {
            stateManager.finish()
        }
    }

    internal class ContinuationInterceptorTarget : TestTarget() {

        override fun createScope(handler: Handler): CoroutineScope {
            val context = createContextWithPausingInterceptor(handler)
            stateManager = context[ContinuationInterceptor.Key] as StateManager<*>
            return CoroutineScope(context)
        }
        override fun toString() = "PausingContinuationInterceptor"
    }

    internal class CoroutineDispatcherTarget : TestTarget() {
        override fun createScope(handler: Handler): CoroutineScope {
            val context = createContextWithPausingDispatcher(handler)
            stateManager = context[ContinuationInterceptor.Key] as StateManager<*>
            return CoroutineScope(context)
        }
        override fun toString() = "PausingDispatcher"
    }
}