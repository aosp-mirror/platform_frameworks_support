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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class LifecycleCoroutineScopeTest {
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

    @Test
    fun initialization() {
        val owner = FakeLifecycleOwner()
        val scope = owner.lifecycleScope
        assertThat(owner.lifecycle.internalScope).isSameAs(scope)
        val scope2 = owner.lifecycleScope
        assertThat(scope).isSameAs(scope2)
        assertThat((owner.lifecycle as LifecycleRegistry).observerCount).isEqualTo(1)
    }

    @Test
    fun simpleLaunch() {
        val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
        assertThat(
            runBlocking {
                owner.lifecycleScope.async {
                    // do nothing
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun launchAfterDestroy() {
        val owner = FakeLifecycleOwner(Lifecycle.State.DESTROYED)
        runBlocking {
            owner.lifecycleScope.launch {
                // do nothing
                throw AssertionError("should not run")
            }.join()
        }
    }

    @Test
    fun launchOnMain() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        assertThat(
            runBlocking(Dispatchers.Main) {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun launchOnIO() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        assertThat(
            runBlocking(Dispatchers.IO) {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun destroyWhileRunning() {
        val startMutex = Mutex(locked = true)
        val alwaysLocked = Mutex(locked = true)
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        val actionWasActive = owner.lifecycleScope.async(Dispatchers.IO) {
            startMutex.unlock()
            alwaysLocked.lock() // wait 4ever
        }
        runBlocking(Dispatchers.Main) {
            startMutex.lock() // wait until it starts
            owner.setState(Lifecycle.State.DESTROYED)
            actionWasActive.join()
            assertThat(actionWasActive.isCancelled).isTrue()
        }
    }

    @Test
    fun throwException() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameAs("foo")
        }
    }

    @Test
    fun throwException_onStart() {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        runBlocking {
            // TODO guarantee later execution
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            withContext(Dispatchers.Main) {
                owner.setState(Lifecycle.State.STARTED)
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameAs("foo")
        }
    }
}
