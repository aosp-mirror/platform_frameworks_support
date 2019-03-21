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

import androidx.paging.ListenablePositionalDataSource.InitialResult
import androidx.paging.ListenablePositionalDataSource.LoadInitialParams
import androidx.paging.ListenablePositionalDataSource.LoadRangeParams
import androidx.paging.ListenablePositionalDataSource.RangeResult
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
class RxPositionalDataSourceTest {
    private val trivialDataSource: RxPositionalDataSource<Int>
    private val perpetuallyLoadingDataSource: RxPositionalDataSource<Int>
    private val errorDataSource: RxPositionalDataSource<Int>

    init {
        // Trivial example with position == value.
        trivialDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> {
                val start = params.requestedStartPosition
                val end = params.requestedStartPosition + params.requestedLoadSize - 1
                val data = (start..end).toList()
                return Single.just(InitialResult(data, start, data.size))
            }

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> {
                val start = params.startPosition
                val end = params.startPosition + params.loadSize - 1
                return Single.just(RangeResult((start..end).toList()))
            }
        }

        perpetuallyLoadingDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> =
                Single.never()

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> =
                Single.never()
        }

        errorDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> =
                Single.error(NotImplementedError())

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> =
                Single.error(NotImplementedError())
        }

        PagedListTestUtil.initDataSource(trivialDataSource)
        PagedListTestUtil.initDataSource(perpetuallyLoadingDataSource)
        PagedListTestUtil.initDataSource(errorDataSource)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, 5, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get()

        assertThat(
            actual,
            equalTo(InitialResult<Int>(List(0) { it }, 0, 0))
        )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams(0, 10, 5, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get()

        assertThat(
            actual,
            equalTo(InitialResult<Int>((0..9).toList(), 0, 10))
        )
    }

    @Test
    fun loadRange_empty() {
        val testParams = LoadRangeParams(5, 0)
        val actual = trivialDataSource
            .loadRange(testParams)
            .get()

        assertThat(
            actual,
            equalTo(RangeResult<Int>(List(0) { it }))
        )
    }

    @Test
    fun loadRange_simple() {
        val testParams = LoadRangeParams(0, 10)
        val actual = trivialDataSource
            .loadRange(testParams)
            .get()

        assertThat(
            actual,
            equalTo(RangeResult<Int>((0..9).toList()))
        )
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .onLoadRange(testParams)
            .subscribe(Consumer { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        trivialDataSource
            .onLoadRange(testParams)
            .test(true)
            .assertComplete()
    }

    @Test(expected = ExecutionException::class)
    fun error() {
        val testParams = LoadRangeParams(0, 10)
        val future = errorDataSource.loadRange(testParams)

        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: ExecutionException) {
            // Ignored.
        }
    }
}