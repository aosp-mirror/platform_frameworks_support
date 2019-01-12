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

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This is just a sanity test for sugar methods to ensure they set the right params.
 */
@RunWith(JUnit4::class)
class WithLifecycleMethodsTest {
    private val testScope = CoroutineScope(Job() + Dispatchers.Default)
    val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
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
    fun withCreatedLifecycle() {
        runBlocking {
            val runEvent = testScope.async {
                withCreated(owner.lifecycle) {
                    true
                }
            }
            runEvent.assertNotFinished()
            owner.setState(CREATED)
            runEvent.assertFinished()
        }
    }

    @Test
    fun withCreatedOwner() {
        runBlocking {
            val runEvent = testScope.async {
                withCreated(owner) {
                    true
                }
            }
            runEvent.assertNotFinished()
            owner.setState(CREATED)
            runEvent.assertFinished()
        }
    }

    @Test
    fun withStartedLifecycle() {
        runBlocking {
            val runEvent = testScope.async {
                withStarted(owner.lifecycle) {
                    true
                }
            }
            owner.setState(CREATED)
            runEvent.assertNotFinished()
            owner.setState(STARTED)
            runEvent.assertFinished()
        }
    }

    @Test
    fun withStartedOwner() {
        runBlocking {
            val runEvent = testScope.async {
                withStarted(owner) {
                    true
                }
            }
            owner.setState(CREATED)
            runEvent.assertNotFinished()
            owner.setState(STARTED)
            runEvent.assertFinished()
        }
    }

    @Test
    fun withResumedLifecycle() {
        runBlocking {
            val runEvent = testScope.async {
                withResumed(owner.lifecycle) {
                    true
                }
            }
            owner.setState(CREATED)
            runEvent.assertNotFinished()
            owner.setState(STARTED)
            runEvent.assertNotFinished()
            owner.setState(RESUMED)
            runEvent.assertFinished()
        }
    }

    @Test
    fun withResumedOwner() {
        runBlocking {
            val runEvent = testScope.async {
                withResumed(owner) {
                    true
                }
            }
            owner.setState(CREATED)
            runEvent.assertNotFinished()
            owner.setState(STARTED)
            runEvent.assertNotFinished()
            owner.setState(RESUMED)
            runEvent.assertFinished()
        }
    }

    private suspend fun Deferred<Boolean>.assertNotFinished() {
        assertThat(withTimeoutOrNull(WAIT_DURATION) {
            await()
        }).isNull()
    }

    private suspend fun Deferred<Boolean>.assertFinished() {
        assertThat(await()).isTrue()
    }

    companion object {
        val WAIT_DURATION = 1000L
    }
}