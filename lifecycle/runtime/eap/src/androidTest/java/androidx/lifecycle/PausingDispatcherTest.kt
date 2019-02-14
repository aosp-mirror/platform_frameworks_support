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

@InternalCoroutinesApi
@SmallTest
@RunWith(Parameterized::class)
class PausingDispatcherTest(
    val testTarget : TestTarget
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
        fun params(): List<TestTarget> = listOf(
            ContinuationInterceptorTarget(), CoroutineDispatcherTarget()
        )
    }

    interface TestTarget {
        fun createScope(handler: Handler): CoroutineScope
        fun pause()
        fun resume()
        fun kill()
    }

    class ContinuationInterceptorTarget : TestTarget {
        override fun createScope(handler: Handler): CoroutineScope {
            return CoroutineScope(handler.asCoroutineDispatcher() + PausingInterceptor(handler))
        }

        override fun pause() {
            TODO("not implemented")
        }

        override fun resume() {
            TODO("not implemented")
        }

        override fun kill() {
            TODO("not implemented")
        }

        override fun toString() = "PausingContinuationInterceptor"
    }

    class CoroutineDispatcherTarget : TestTarget {
        override fun createScope(handler: Handler): CoroutineScope {
            return CoroutineScope(PausingDispatcher(handler))
        }

        override fun pause() {
            TODO("not implemented")
        }

        override fun resume() {
            TODO("not implemented")
        }

        override fun kill() {
            TODO("not implemented")
        }

        override fun toString() = "PausingDispatcher"
    }
}