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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * DANGER
 * DANGER
 * DANGER
 *
 * this is super experimental and probably a bad idea so do not copy.
 */
@RunWith(JUnit4::class)
class ResultLiveDataFlatMapLaunchTest : CoroutineTestBase() {

    private val src = MutableLiveData<Int>()//add nullable src tests too

    @Test
    fun basicFlatMap() {
        runTest {
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.success(2))
        }
    }

    @Test
    fun flatMapWithDelay() {
        runTest {
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                delay(1500)
                yield(it * 2)
            }.collectAsync(2)
            src.value = 1
            delay(500)
            src.value = 2
            assertThat(data.await()).containsExactly(Resource.success(2), Resource.success(4))
        }
    }

    @Test
    fun flatMapTwoValues() {
        runTest {
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(2)
            src.value = 1
            delay(500)
            src.value = 2
            assertThat(data.await()).containsExactly(Resource.success(2), Resource.success(4))
        }
    }

    @Test
    fun flatMapTwoLevels() {
        runTest {
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                yield(it * 2)
                yield(it * 4)
            }.flatMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(2)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.success(4), Resource.success(8))
        }
    }

    @Test
    fun switchMapException() {
        runTest {
            @Suppress("UNREACHABLE_CODE")
            val data = src.toResource().flatMap(testScope.coroutineContext) {
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
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                yield(it * 2)
            }.flatMap(testScope.coroutineContext) {
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
            val data = src.toResource().flatMap(testScope.coroutineContext) {
                throw MyException("bad")
                yield(2)
            }.flatMap(testScope.coroutineContext) {
                yield(it * 2)
            }.collectAsync(1)
            src.value = 1
            assertThat(data.await()).containsExactly(Resource.error<Int>(MyException("bad")))
        }
    }

    // exception with equals check for the msg
    private data class MyException(val msg: String) : Throwable("test exception $msg")
}

