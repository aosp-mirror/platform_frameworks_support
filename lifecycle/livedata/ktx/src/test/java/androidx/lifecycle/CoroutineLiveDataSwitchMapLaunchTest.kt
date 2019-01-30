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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CoroutineLiveDataSwitchMapLaunchTest {
    private var mainThread : Thread? = null
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
        Dispatchers.resetMain()
        testScope.cancel()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun simple() {
        runTest {
            val ld= src.switchMapLaunch<Int, String>(testScope.coroutineContext) {
                yield("$it")
                yield("$it-$it")
            }
            val items = testScope.async {
                ld.collect(2)
            }
            src.value = 1
            Truth.assertThat(items.await()).isEqualTo(listOf("1", "1-1"))
        }

    }

    suspend fun <T> LiveData<T>.collect(capacity : Int) : List<T> = withContext(Dispatchers.Main) {
        val full = Mutex(true)
        val res = arrayListOf<T>()
        val observer = Observer<T> {
            res.add(it)
            if (res.size == capacity) {
                full.unlock()
            }
        }
        observeForever(observer)
        full.lock()
        removeObserver(observer)
        res
    }

    fun runTest(f : suspend () -> Unit) {
        runBlocking {
            testScope.launch(Dispatchers.Main) {
                f()
            }
        }
    }
}