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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CopyOnWriteArrayList
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
        mainExecutor.shutdown()
        mainExecutor.awaitTermination(10, TimeUnit.SECONDS)
        Dispatchers.resetMain()
    }

    @Test
    fun initialization() {
        val owner = FakeLifecycleOwner()
        val scope = owner.lifecycleScope
        assertThat(owner.myLifecycle.internalScope).isSameAs(scope)
        val scope2 = owner.lifecycleScope
        assertThat(scope).isSameAs(scope2)
        assertThat(owner.myLifecycle.observers).containsExactly(scope)
    }

    @Test
    fun simpleLaunch() {
        val owner = FakeLifecycleOwner()
        runBlocking {
            owner.lifecycleScope.launch {
                // do nothing
            }
        }
    }

    @Test
    fun launchAfterDestroy() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.DESTROYED
        }
        try {
            runBlocking {
                owner.lifecycleScope.launch {
                    // do nothing
                    throw AssertionError("should not run")
                }
            }
            throw AssertionError("should've failed")
        } catch (illegalState : IllegalStateException) {
            // expected
        }
    }

    @Test
    fun launchOnMain() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.STARTED
        }
        mainExecutor.submit {

        }.get(10, TimeUnit.SECONDS)
        try {
            runBlocking {
                owner.lifecycleScope.launch {
                    // do nothing
                    throw AssertionError("should not run")
                }
            }
            throw AssertionError("should've failed")
        } catch (illegalState : IllegalStateException) {
            // expected
        }
    }

}

internal class FakeLifecycleOwner : LifecycleOwner {
    val myLifecycle = FakeLifecycle()
    override fun getLifecycle() = myLifecycle
}

internal class FakeLifecycle : Lifecycle() {
    var state : State = State.INITIALIZED
    val observers = CopyOnWriteArrayList<LifecycleObserver>()
    override fun addObserver(observer: LifecycleObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    override fun getCurrentState() = state
}