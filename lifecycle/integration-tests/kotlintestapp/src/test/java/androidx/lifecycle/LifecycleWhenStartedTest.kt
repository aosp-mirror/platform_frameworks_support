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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class LifecycleWhenStartedTest {
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

    @Test
    fun checkThread() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        // obtain main thread via executor
        val mainThread = runBlocking(Dispatchers.Main) {
            Thread.currentThread()
        }
        val runThread = runBlocking(Dispatchers.IO) {
            owner.whenStarted {
                Thread.currentThread()
            }
        }
        assertThat(runThread).isSameAs(mainThread)
    }

    @Test
    fun simple() {
        val latch = CountDownLatch(1)
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        testScope.launch(Dispatchers.IO) {
            owner.whenStarted {
                latch.countDown()
            }
        }
        // should not run yet
        latch.assertFalse()
        runBlocking(Dispatchers.Main) {
            owner.setState(Lifecycle.State.STARTED)
        }
        latch.assertTrue()
    }

    @Test
    fun whenStarted_alreadyDestroyed() {
        val owner = FakeLifecycleOwner(Lifecycle.State.DESTROYED)
        runBlocking {
            val action = testScope.async {
                owner.whenStarted {
                    "foo"
                }
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).isNotNull()
        }
    }

    @Test
    fun whenStarted_destroyedWhileWaiting() {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        runBlocking {
            val action = async {
                owner.whenStarted {
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
    fun whenStarted_throwException() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = testScope.async {
                owner.whenStarted {
                    throw RuntimeException("foo")
                }
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameAs("foo")
        }
    }
}