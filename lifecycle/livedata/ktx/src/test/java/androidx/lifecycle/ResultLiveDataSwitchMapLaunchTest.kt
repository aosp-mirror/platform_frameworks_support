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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * DANGER
 * DANGER
 * DANGER
 *
 * this is super experimental and probably a bad idea so do not copy.
 */
@RunWith(JUnit4::class)
class ResultLiveDataSwitchMapLaunchTest {

    private var mainThread: Thread? = null
    private val mainExecutor = Executors.newSingleThreadExecutor {
        Thread(it).also {
            mainThread = it
        }
    }
    private val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val src = MutableLiveData<Int>()//add nullable src tests too
    private val archTaskExecutor = object : TaskExecutor() {
        override fun executeOnDiskIO(runnable: Runnable) {
            testScope.launch(Dispatchers.IO) {
                runnable.run()
            }
        }

        override fun postToMainThread(runnable: Runnable) {
            mainExecutor.submit(runnable)
        }

        override fun isMainThread(): Boolean {
            return mainThread == Thread.currentThread()
        }

    }

    @ExperimentalCoroutinesApi
    @Before
    fun setMainDispatcher() {
        ArchTaskExecutor.getInstance().setDelegate(archTaskExecutor)
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
    }

    @ExperimentalCoroutinesApi
    @After
    fun clearMainDispatcher() {
        mainExecutor.shutdownNow()
        mainExecutor.awaitTermination(10, TimeUnit.SECONDS)
        testScope.cancel()
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun basicSwitchMap() {
        runTest {
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.success(2))
        }
    }

    @Test
    fun switchMapWithDelay() {
        runTest {
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                delay(1500)
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            delay(500)
            src.value = 2
            assertThat(data.await()).containsExactly(Resource.success(4))
        }
    }

    @Test
    fun switchMapTwoValues() {
        runTest {
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(2)
            src.value = 1
            delay(500)
            src.value = 2
            assertThat(data.await()).containsExactly(Resource.success(2), Resource.success(4))
        }
    }

    @Test
    fun switchMapTwoLevels() {
        runTest {
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.success(4))
        }
    }

    @Test
    fun switchMapException() {
        runTest {
            @Suppress("UNREACHABLE_CODE")
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                throw MyException("bad")
                yield(2)
            }.collectAsync(1)
            src.value = 1
            val res = data.await()
            assertThat(res).containsExactly(Resource.error<Int>(MyException("bad")))
        }
    }

    @Test
    fun switchMapException_transformation2() {
        runTest {
            @Suppress("UNREACHABLE_CODE")
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.switchMap(testScope.coroutineContext) {
                throw MyException("bad")
                yield(2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.error<Int>(MyException("bad")))
        }
    }

    @Test
    fun switchMapException_transformation1() {
        runTest {
            @Suppress("UNREACHABLE_CODE")
            val data = src.toResource().switchMap(testScope.coroutineContext) {
                throw MyException("bad")
                yield(2)
            }.switchMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.error<Int>(MyException("bad")))
        }
    }

    private fun runTest(
        f: suspend () -> Unit
    ) {
        runBlocking(testScope.coroutineContext + Dispatchers.Main) {
            launch {
                f()
            }.join()
        }
    }
    // exception with equals check for the msg
    private data class MyException(val msg : String) : Throwable("test exception $msg")
}

