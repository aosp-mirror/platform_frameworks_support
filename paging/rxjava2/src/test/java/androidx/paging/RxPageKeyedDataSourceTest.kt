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

package androidx.paging

import androidx.paging.ListenablePageKeyedDataSource.LoadInitialParams
import androidx.paging.ListenablePageKeyedDataSource.LoadParams
import androidx.test.filters.SmallTest
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class RxPageKeyedDataSourceTest {
    private val trivialDataSource: RxPageKeyedDataSource<Int, Int>
    private val perpetuallyLoadingDataSource: RxPageKeyedDataSource<Int, Int>
    private val errorDataSource: RxPageKeyedDataSource<Int, Int>

    init {
        trivialDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun loadInitial(
                params: LoadInitialParams<Int>
            ): Single<LoadInitialResponse<Int, Int>> = Single.just(
                LoadInitialResponse(
                    (0..(params.requestedLoadSize - 1)).toList(),
                    0,
                    -params.requestedLoadSize,
                    params.requestedLoadSize
                )
            )

            override fun loadBefore(params: LoadParams<Int>): Single<LoadBeforeResponse<Int, Int>> {
                val start = params.key - params.requestedLoadSize
                val end = params.key - 1
                return Single.just(
                    LoadBeforeResponse(
                        (start..end).toList(),
                        params.requestedLoadSize
                    )
                )
            }

            override fun loadAfter(params: LoadParams<Int>): Single<LoadAfterResponse<Int, Int>> {
                val start = params.key
                val end = params.requestedLoadSize - 1
                return Single.just(
                    LoadAfterResponse((start..end).toList(), params.requestedLoadSize)
                )
            }
        }

        perpetuallyLoadingDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun loadInitial(
                params: LoadInitialParams<Int>
            ): Single<LoadInitialResponse<Int, Int>> = Single.never()

            override fun loadBefore(params: LoadParams<Int>): Single<LoadBeforeResponse<Int, Int>> =
                Single.never()

            override fun loadAfter(params: LoadParams<Int>): Single<LoadAfterResponse<Int, Int>> =
                Single.never()
        }

        errorDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun loadInitial(
                params: LoadInitialParams<Int>
            ): Single<LoadInitialResponse<Int, Int>> = Single.error(NotImplementedError())

            override fun loadBefore(
                params: LoadParams<Int>
            ): Single<LoadBeforeResponse<Int, Int>> = Single.error(NotImplementedError())

            override fun loadAfter(
                params: LoadParams<Int>
            ): Single<LoadAfterResponse<Int, Int>> = Single.error(NotImplementedError())
        }

        PagedListTestUtil.initDataSource(trivialDataSource)
        PagedListTestUtil.initDataSource(perpetuallyLoadingDataSource)
        PagedListTestUtil.initDataSource(errorDataSource)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams<Int>(0, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.InitialResult<Int, Int>(
                    List(0) { it },
                    0,
                    0,
                    0,
                    0
                )
            )
        )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams<Int>(10, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.InitialResult<Int, Int>(
                    (0..9).toList(),
                    0,
                    10,
                    -10,
                    10
                )
            )
        )
    }

    @Test
    fun loadAfter_empty() {
        val testParams = LoadParams(0, 0)
        val actual = trivialDataSource
            .loadAfter(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.Result<Int, Int>(
                    List(0) { it },
                    0
                )
            )
        )
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 10)
        val actual = trivialDataSource
            .loadAfter(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.Result<Int, Int>(
                    (0..9).toList(),
                    10
                )
            )
        )
    }

    @Test
    fun loadBefore_empty() {
        val testParams = LoadParams(0, 0)
        val actual = trivialDataSource
            .loadBefore(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.Result<Int, Int>(
                    List(0) { it },
                    0
                )
            )
        )
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 10)
        val actual = trivialDataSource
            .loadBefore(testParams)
            .get()

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.Result<Int, Int>(
                    (-10..-1).toList(),
                    10
                )
            )
        )
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .loadBefore(testParams)
            .subscribe(Consumer<RxPageKeyedDataSource.LoadBeforeResponse<Int, Int>> {
                didSucceed = true
            })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        trivialDataSource
            .loadBefore(testParams)
            .test(true)
            .assertComplete()
    }

    @Test(expected = ExecutionException::class)
    fun error() {
        val testParams = LoadParams(0, 10)
        val future = errorDataSource.loadBefore(testParams)
        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: ExecutionException) {
            // Ignored.
        }
    }
}