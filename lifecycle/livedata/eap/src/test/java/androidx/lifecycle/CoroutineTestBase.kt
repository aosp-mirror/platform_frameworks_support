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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

abstract class CoroutineTestBase {
    private var mainThread: Thread? = null
    private val mainExecutor = Executors.newSingleThreadExecutor {
        Thread(it).also {
            mainThread = it
        }
    }
    protected val testScope = CoroutineScope(Dispatchers.Main)
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

    @Before
    fun setMainDispatcher() {
        ArchTaskExecutor.getInstance().setDelegate(archTaskExecutor)
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
    }

    @ExperimentalCoroutinesApi
    @After
    fun clearMainDispatcher() {
        testScope.cancel()
        mainExecutor.shutdown()
        Truth.assertThat(mainExecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    fun runTest(
        context: CoroutineContext = testScope.coroutineContext + Dispatchers.Main,
        f: suspend () -> Unit
    ) {
        val exception = AtomicReference<Throwable>(null)
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.set(throwable)
        }

        runBlocking(context + exceptionHandler) {
            launch {
                f()
            }.join()
        }
        exception.get()?.let {
            throw it
        }
    }
}