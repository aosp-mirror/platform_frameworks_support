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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class RxItemKeyedDataSourceTest {
    private lateinit var trivialDataSource: RxItemKeyedDataSource<Int, Int>
    private lateinit var perpetuallyLoadingDataSource: RxItemKeyedDataSource<Int, Int>

    @Before
    fun setUp() {
        trivialDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun loadInitial(
                params: LoadInitialParams<Int>
            ): Single<LoadInitialResponse<Int>> {
                val initialKey = params.requestedInitialKey ?: 0
                return Single.just(
                    LoadInitialResponse(
                        (initialKey..(initialKey + params.requestedLoadSize - 1)).toList(),
                        initialKey
                    )
                )
            }

            override fun loadBefore(params: LoadParams<Int>): Single<List<Int>> {
                val start = params.key - params.requestedLoadSize
                val end = params.key - 1
                return Single.just((start..end).toList())
            }

            override fun loadAfter(params: LoadParams<Int>): Single<List<Int>> {
                val start = params.key
                val end = params.key + params.requestedLoadSize - 1
                return Single.just((start..end).toList())
            }
        }

        perpetuallyLoadingDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun loadInitial(
                params: LoadInitialParams<Int>
            ): Single<LoadInitialResponse<Int>> = Single.never()

            override fun loadBefore(params: LoadParams<Int>): Single<List<Int>> = Single.never()

            override fun loadAfter(params: LoadParams<Int>): Single<List<Int>> = Single.never()
        }

        PagedListTestUtil.initDataSource(trivialDataSource)
        PagedListTestUtil.initDataSource(perpetuallyLoadingDataSource)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, false)
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
        val testParams = LoadInitialParams(0, 10, false)
        val actual = trivialDataSource
            .loadInitial(testParams)
            .get()

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
            .get()

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
            .get()

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
            .get()

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
            .get()

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
            .loadBefore(testParams)
            .subscribe(Consumer<List<Int>> { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = ItemKeyedDataSource.LoadParams(0, 10)
        trivialDataSource
            .loadBefore(testParams)
            .test(true)
            .assertComplete()
    }
}