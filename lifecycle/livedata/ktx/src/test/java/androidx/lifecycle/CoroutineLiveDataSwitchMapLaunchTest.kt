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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CoroutineLiveDataSwitchMapLaunchTest {
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
    fun simple() {
        runTest {
            val ld = src.switchMapLaunch<Int, String>(testScope.coroutineContext) {
                yield("$it")
                yield("$it-$it")
            }
            val items = ld.collectAsync(2)
            src.value = 1
            assertThat(items.await()).isEqualTo(listOf("1", "1-1"))
        }
    }

    @Test
    fun throwException_thenSucceed() {
        runTest {
            val exception = CompletableDeferred<Throwable>()
            val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                exception.complete(throwable)
            }
            var shouldThrow = true
            val ld =
                src.switchMapLaunch<Int, String>(testScope.coroutineContext + exceptionHandler) {
                    if (shouldThrow) {
                        shouldThrow = false
                        throw IllegalArgumentException("fail i should")
                    } else {
                        yield("hello $it")
                    }
                }
            val items = ld.collectAsync(1)
            src.value = 1
            assertThat(exception.await()).hasMessageThat().contains("fail i should")
            src.value = 2
            assertThat(items.await()).isEqualTo(listOf("hello 2"))
        }
    }

    @Test
    fun multiLevel() {
        runTest {
            val liveData = src.switchMapLaunch<Int, Int>(testScope.coroutineContext) {
                yield(it * it)
            }.switchMapLaunch<Int, String>(Dispatchers.IO) {
                yield("$it-$it")
            }
            val items = liveData.collectAsync(3)
            (0 until 3).forEach {
                val next = liveData.nextValueAsync()
                src.value = it
                next.await()
            }
            assertThat(items.await()).isEqualTo(
                listOf("0-0", "1-1", "4-4")
            )
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
}