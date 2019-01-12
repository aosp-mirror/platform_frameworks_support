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

import androidx.annotation.MainThread
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
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
import java.util.concurrent.CancellationException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
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
        runBlocking {
            owner.lifecycleScope.launch {
                // do nothing
                throw AssertionError("should not run")
            }.join()
        }
    }

    @Test
    fun launchOnMainInsideMain() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.STARTED
        }
        val latch = CountDownLatch(1)
        mainExecutor.submit {
            owner.lifecycleScope.launch(Dispatchers.Main) {
                latch.countDown()
            }
        }
        latch.assertTrue()
    }

    @Test
    fun launchOnIO() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.STARTED
        }
        val latch = CountDownLatch(1)
        owner.lifecycleScope.launch(Dispatchers.IO) {
            latch.countDown()
        }
        latch.assertTrue()
    }

    @Test
    fun destroyWhileRunning() {
        val startLatch = CountDownLatch(1)
        val continueLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(1)
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.STARTED
        }
        owner.lifecycleScope.launch(Dispatchers.IO) {
            startLatch.countDown()
            continueLatch.assertTrue()
            if (isActive) {
                endLatch.countDown()
            }
        }
        startLatch.assertTrue()
        runBlocking(Dispatchers.Main) {
            owner.destroy()
        }
        continueLatch.countDown()
        endLatch.assertFalse()
    }

    @Test
    fun whenStarted_assertThread() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.STARTED
        }
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
    fun whenStarted_simple() {
        val latch = CountDownLatch(1)
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.CREATED
        }
        GlobalScope.launch(Dispatchers.IO) {
            owner.whenStarted {
                latch.countDown()
            }
        }
        // should not run yet
        latch.assertFalse()
        runBlocking(Dispatchers.Main) {
            owner.start()
        }
        latch.assertTrue()
    }

    @Test
    fun whenStarted_alreadyDestroyed() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.DESTROYED
        }
        try {
            runBlocking {
                owner.whenStarted {
                    "2"
                }
            }
            throw AssertionError("should've failed")
        } catch (cancelled : CancellationException) {
            // expected
        }
    }

    @Test
    fun whenStarted_destroyedWhileWaiting() {
        val owner = FakeLifecycleOwner().also {
            it.myLifecycle.state = Lifecycle.State.CREATED
        }
        runBlocking {
            val async = async {
                owner.whenStarted {
                    "doing something"
                }
            }
            withContext(Dispatchers.Main) {
                owner.destroy()
            }
            try {
                async.await()
                throw AssertionError("should've been cancelled")
            } catch (ex : CancellationException) {
                // expected
            }
        }
    }

    private fun CountDownLatch.assertTrue() =
        assertThat(await(10, TimeUnit.SECONDS)).isTrue()

    private fun CountDownLatch.assertFalse() =
        assertThat(await(2, TimeUnit.SECONDS)).isFalse()
}

internal class FakeLifecycleOwner : LifecycleOwner {
    val myLifecycle = FakeLifecycle(this)
    override fun getLifecycle() = myLifecycle

    @MainThread
    fun destroy() {
        myLifecycle.destroy()
    }

    @MainThread
    fun start() {
        myLifecycle.start()
    }
}

internal class FakeLifecycle(val owner  : LifecycleOwner) : Lifecycle() {
    var state : State = State.INITIALIZED
    val observers = CopyOnWriteArrayList<DefaultLifecycleObserver>()
    override fun addObserver(observer: LifecycleObserver) {
        // test sanity, we only support default here
        assertThat(observer).isInstanceOf(DefaultLifecycleObserver::class.java)
        observers.add(observer as DefaultLifecycleObserver)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    override fun getCurrentState() = state
    fun destroy() {
        state = State.DESTROYED
        observers.forEach {
            (it as DefaultLifecycleObserver).onDestroy(owner)
        }
    }

    fun start() {
        state = State.STARTED
        observers.forEach {
            (it as DefaultLifecycleObserver).onStart(owner)
        }
    }
}