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

import androidx.paging.ListenableItemKeyedDataSource.InitialResult
import androidx.paging.ListenableItemKeyedDataSource.LoadInitialParams
import androidx.paging.ListenableItemKeyedDataSource.LoadParams
import androidx.paging.ListenableItemKeyedDataSource.Result
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
class RxItemKeyedDataSourceTest {
    private val trivialDataSource: RxItemKeyedDataSource<Int, Int>
    private val perpetuallyLoadingDataSource: RxItemKeyedDataSource<Int, Int>
    private val errorDataSource: RxItemKeyedDataSource<Int, Int>

    init {
        // Trivial example with key == value.
        trivialDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> {
                val initialKey = params.requestedInitialKey ?: 0
                val data = (initialKey..(initialKey + params.requestedLoadSize - 1))
                    .toList()
                return Single.just(InitialResult(data, initialKey, data.size))
            }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> {
                val start = params.key
                val end = params.key + params.requestedLoadSize - 1
                return Single.just(Result((start..end).toList()))
            }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> {
                val start = params.key - params.requestedLoadSize
                val end = params.key - 1
                return Single.just(Result((start..end).toList()))
            }
        }

        perpetuallyLoadingDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> =
                Single.never()

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> = Single.never()

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> = Single.never()
        }

        errorDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> =
                Single.error(NotImplementedError())

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> =
                Single.error(NotImplementedError())

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> =
                Single.error(NotImplementedError())
        }

        PagedListTestUtil.initDataSource(trivialDataSource)
        PagedListTestUtil.initDataSource(perpetuallyLoadingDataSource)
        PagedListTestUtil.initDataSource(errorDataSource)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(InitialResult<Int>(List(0) { it }, 0, 0))
        )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams(0, 10, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(InitialResult<Int>((0..9).toList(), 0, 10))
        )
    }

    @Test
    fun loadAfter_empty() {
        val testParams = LoadParams(0, 0)
        val actual = trivialDataSource
            .loadAfter(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(Result<Int>(List(0) { it }))
        )
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 10)
        val actual = trivialDataSource
            .loadAfter(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(Result<Int>((0..9).toList()))
        )
    }

    @Test
    fun loadBefore_empty() {
        val testParams = LoadParams(0, 0)
        val actual = trivialDataSource
            .loadBefore(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(Result<Int>(List(0) { it }))
        )
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 10)
        val actual = trivialDataSource
            .loadBefore(testParams)
            .get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(Result<Int>((-10..-1).toList()))
        )
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = ItemKeyedDataSource.LoadParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .onLoadBefore(testParams)
            .subscribe(Consumer { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = ItemKeyedDataSource.LoadParams(0, 10)
        trivialDataSource
            .onLoadBefore(testParams)
            .test(true)
            .assertComplete()
    }

    @Test
    fun error() {
        val testParams = LoadInitialParams(0, 0, false)
        val future = errorDataSource.loadInitial(testParams)

        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: ExecutionException) {
            // Ignored.
        }
    }
}