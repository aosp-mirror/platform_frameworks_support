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

import androidx.paging.ItemKeyedDataSource.LoadInitialParams
import androidx.paging.ItemKeyedDataSource.LoadParams
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
class RxItemKeyedDataSourceTest {
    private val trivialDataSource = object : RxItemKeyedDataSource<Int, Int>() {
        override fun getKey(item: Int): Int = item

        override fun loadInitial(params: LoadInitialParams<Int>): Single<LoadInitialResponse<Int>> =
            Single.just(
                LoadInitialResponse(
                    ((params.requestedInitialKey ?: 0)..((params.requestedInitialKey
                        ?: 0) + params.requestedLoadSize - 1)).toList(),
                    params.requestedInitialKey ?: 0
                )
            )

        override fun loadBefore(params: LoadParams<Int>): Single<List<Int>> =
            Single.just(((params.key - params.requestedLoadSize)..(params.key - 1)).toList())

        override fun loadAfter(params: LoadParams<Int>): Single<List<Int>> =
            Single.just((params.key..(params.key + params.requestedLoadSize - 1)).toList())
    }

    private val perpetuallyLoadingDataSource = object : RxItemKeyedDataSource<Int, Int>() {
        override fun getKey(item: Int): Int = item

        override fun loadInitial(params: LoadInitialParams<Int>): Single<LoadInitialResponse<Int>> =
            Single.never()

        override fun loadBefore(params: LoadParams<Int>): Single<List<Int>> = Single.never()

        override fun loadAfter(params: LoadParams<Int>): Single<List<Int>> = Single.never()
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult(
                RxItemKeyedDataSource.LoadInitialResponse(List(0) { it }, 0)
            )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams(0, 10, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult(
                RxItemKeyedDataSource.LoadInitialResponse((0..9).toList(), 0)
            )
    }

    @Test
    fun loadAfter_empty() {
        val testParams = LoadParams(0, 0)
        trivialDataSource
            .loadAfter(testParams)
            .test()
            .assertResult((List(0) { it }))
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 10)
        trivialDataSource
            .loadAfter(testParams)
            .test()
            .assertResult((0..9).toList())
    }

    @Test
    fun loadBefore_empty() {
        val testParams = LoadParams(0, 0)
        trivialDataSource
            .loadBefore(testParams)
            .test()
            .assertResult((List(0) { it }))
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 10)
        trivialDataSource
            .loadBefore(testParams)
            .test()
            .assertResult((-10..-1).toList())
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = LoadParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .loadBefore(testParams)
            .subscribe(Consumer<List<Int>> { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = LoadParams(0, 10)
        trivialDataSource
            .loadBefore(testParams)
            .test(true)
            .assertComplete()
    }
}