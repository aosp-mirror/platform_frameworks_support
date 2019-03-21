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

import androidx.paging.PageKeyedDataSource.LoadInitialParams
import androidx.paging.RxPageKeyedDataSource.LoadAfterResponse
import androidx.paging.RxPageKeyedDataSource.LoadBeforeResponse
import androidx.paging.RxPageKeyedDataSource.LoadInitialResponse
import androidx.test.filters.SmallTest
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class RxPageKeyedDataSourceTest {
    private val trivialDataSource = object : RxPageKeyedDataSource<Int, Int>() {
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

        override fun loadBefore(params: LoadParams<Int>): Single<LoadBeforeResponse<Int, Int>> =
            Single.just(
                LoadBeforeResponse(
                    ((params.key - params.requestedLoadSize)..(params.key - 1)).toList(),
                    params.requestedLoadSize
                )
            )

        override fun loadAfter(params: LoadParams<Int>): Single<LoadAfterResponse<Int, Int>> =
            Single.just(
                LoadAfterResponse(
                    (params.key..(params.requestedLoadSize - 1)).toList(),
                    params.requestedLoadSize
                )
            )
    }

    private val perpetuallyLoadingDataSource = object : RxPageKeyedDataSource<Int, Int>() {
        override fun loadInitial(
            params: LoadInitialParams<Int>
        ): Single<LoadInitialResponse<Int, Int>> = Single.never()

        override fun loadBefore(params: LoadParams<Int>): Single<LoadBeforeResponse<Int, Int>> =
            Single.never()

        override fun loadAfter(params: LoadParams<Int>): Single<LoadAfterResponse<Int, Int>> =
            Single.never()
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams<Int>(0, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult(
                LoadInitialResponse(
                    List(0) { it },
                    0,
                    0,
                    0
                )
            )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams<Int>(10, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult(
                LoadInitialResponse(
                    (0..9).toList(),
                    0,
                    -10,
                    10
                )
            )
    }

    @Test
    fun loadAfter_empty() {
        val testParams = PageKeyedDataSource.LoadParams(0, 0)
        trivialDataSource
            .loadAfter(testParams)
            .test()
            .assertResult(LoadAfterResponse(List(0) { it }, 0))
    }

    @Test
    fun loadAfter_simple() {
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        trivialDataSource
            .loadAfter(testParams)
            .test()
            .assertResult(LoadAfterResponse((0..9).toList(), 10))
    }

    @Test
    fun loadBefore_empty() {
        val testParams = PageKeyedDataSource.LoadParams(0, 0)
        trivialDataSource
            .loadBefore(testParams)
            .test()
            .assertResult(LoadBeforeResponse(List(0) { it }, 0))
    }

    @Test
    fun loadBefore_simple() {
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        trivialDataSource
            .loadBefore(testParams)
            .test()
            .assertResult(LoadBeforeResponse((-10..-1).toList(), 10))
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .loadBefore(testParams)
            .subscribe(Consumer<LoadBeforeResponse<Int, Int>> { didSucceed = true })

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
}