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
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SmallTest
@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
class WithLifecycleStateTest(
    private val targetState: Lifecycle.State
) {
    private val testScope = CoroutineScope(Job() + Dispatchers.Default)

    private val mainExecutor = Executors.newSingleThreadExecutor()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
    }

    @After
    fun clearMainDispatcher() {
        mainExecutor.shutdownNow()
        mainExecutor.awaitTermination(10, TimeUnit.SECONDS)
        Dispatchers.resetMain()
    }

    @After
    fun cancelScope() {
        testScope.cancel()
    }

    @Test(expected = IllegalArgumentException::class)
    fun badTargetState() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            withStateAtLeast(
                lifecycle = owner.lifecycle,
                state = Lifecycle.State.DESTROYED
            ) {
                "nope"
            }
        }
    }

    @Test
    fun exactState() {
        val owner = FakeLifecycleOwner(targetState)
        Truth.assertThat(runBlocking {
            withStateAtLeast(
                lifecycle = owner.lifecycle,
                state = targetState
            ) {
                true
            }
        }).isTrue()
    }

    @Test
    fun nextState() {
        val owner = FakeLifecycleOwner(targetState.nextOrSelf())
        Truth.assertThat(runBlocking {
            withStateAtLeast(
                lifecycle = owner.lifecycle,
                state = targetState
            ) {
                true
            }
        }).isTrue()
    }

    @Test
    fun checkThread() {
        val owner = FakeLifecycleOwner(targetState)
        // obtain main thread via executor
        val mainThread = runBlocking(Dispatchers.Main) {
            Thread.currentThread()
        }
        val runThread = runBlocking(Dispatchers.IO) {
            withStateAtLeast(
                lifecycle = owner.lifecycle,
                state = targetState
            ) {
                Thread.currentThread()
            }
        }
        assertThat(runThread).isSameAs(mainThread)
    }

    @LargeTest
    @Test
    fun waitUntilReady() {
        Assume.assumeTrue(targetState != Lifecycle.State.INITIALIZED)
        val latch = CountDownLatch(1)
        val owner = FakeLifecycleOwner(targetState.previous())
        testScope.launch(Dispatchers.IO) {
            withStateAtLeast(
                lifecycle = owner.lifecycle,
                state = targetState
            ) {
                latch.countDown()
            }
        }
        // should not run yet
        Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse()
        runBlocking(Dispatchers.Main) {
            owner.setState(targetState)
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun alreadyDestroyed() {
        val owner = FakeLifecycleOwner(Lifecycle.State.DESTROYED)
        runBlocking {
            val action = testScope.async {
                withStateAtLeast(
                    lifecycle = owner.lifecycle,
                    state = targetState
                ) {
                    "foo"
                }
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).isNotNull()
        }
    }

    @Test
    fun destroyedWhileWaiting() {
        val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
        runBlocking {
            val action = async {
                withStateAtLeast(
                    lifecycle = owner.lifecycle,
                    state = targetState
                ) {
                    "doing something"
                }
            }
            withContext(Dispatchers.Main) {
                owner.setState(Lifecycle.State.DESTROYED)
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).isNotNull()
        }
    }

    @Test
    fun blockThrowsException() {
        val owner = FakeLifecycleOwner(targetState)
        runBlocking {
            val action = testScope.async {
                withStateAtLeast(
                    lifecycle = owner.lifecycle,
                    state = targetState
                ) {
                    throw RuntimeException("foo")
                }
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameAs("foo")
        }
    }

    @Test
    fun removeLifecycleObserverIfCancelled() {
        Assume.assumeTrue(targetState > Lifecycle.State.INITIALIZED)
        val owner = FakeLifecycleOwner(targetState.previous())
        runBlocking(testScope.coroutineContext) {
            val job = testScope.launch {
                withStateAtLeast(
                    lifecycle = owner.lifecycle,
                    state = targetState
                ) {
                    throw AssertionError("should've never run")
                }
            }
            assertThat(owner.awaitExactObserverCount(1)).isTrue()
            job.cancel()
            // cancel the job, ensure that we unsubscribe from the lifecycle
            assertThat(owner.awaitExactObserverCount(0)).isTrue()
        }
    }

    @Test
    fun avoidReentrance() {
        val runCount = AtomicInteger(0)
        Assume.assumeTrue(targetState < Lifecycle.State.RESUMED)
        val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)

        runBlocking {
            val job = testScope.launch {
                withStateAtLeast(
                    lifecycle = owner.lifecycle,
                    state = targetState
                ) {
                    runCount.incrementAndGet()
                    val next = owner.lifecycle.currentState.nextOrSelf()
                    if (next != targetState) {
                        owner.setState(next)
                    }
                }
            }
            owner.setState(targetState)
            job.join()
        }
        assertThat(runCount.get()).isEqualTo(1)
    }

    private fun Lifecycle.State.previous() = Lifecycle.State.values()[ordinal - 1]
    private fun Lifecycle.State.nextOrSelf(): Lifecycle.State {
        return if (this == Lifecycle.State.RESUMED) {
            Lifecycle.State.RESUMED
        } else {
            Lifecycle.State.values()[ordinal + 1]
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "targetState={0}")
        fun params() = listOf(
            Lifecycle.State.INITIALIZED,
            Lifecycle.State.CREATED,
            Lifecycle.State.STARTED,
            Lifecycle.State.RESUMED
        )
    }
}