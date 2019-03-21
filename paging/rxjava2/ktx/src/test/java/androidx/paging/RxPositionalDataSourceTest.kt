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
class RxPositionalDataSourceTest {
    private val trivialDataSource = object : RxPositionalDataSource<Int>() {
        override fun loadInitial(params: LoadInitialParams): Single<List<Int>> {
            val startPosition = params.requestedStartPosition
            return Single.just(
                (startPosition..(startPosition + params.requestedLoadSize - 1)).toList()
            )
        }

        override fun loadRange(params: LoadRangeParams): Single<List<Int>> = Single.just(
            (params.startPosition..(params.startPosition + params.loadSize - 1)).toList()
        )
    }

    private val perpetuallyLoadingDataSource = object : RxPositionalDataSource<Int>() {
        override fun loadInitial(params: LoadInitialParams): Single<List<Int>> = Single.never()

        override fun loadRange(params: LoadRangeParams): Single<List<Int>> = Single.never()
    }

    @Test
    fun loadInitial_empty() {
        val testParams = PositionalDataSource.LoadInitialParams(5, 0, 5, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult(List(0) { it })
    }

    @Test
    fun loadInitial_simple() {
        val testParams = PositionalDataSource.LoadInitialParams(0, 10, 5, false)
        trivialDataSource
            .loadInitial(testParams)
            .test()
            .assertResult((0..9).toList())
    }

    @Test
    fun loadRange_empty() {
        val testParams = PositionalDataSource.LoadRangeParams(5, 0)
        trivialDataSource
            .loadRange(testParams)
            .test()
            .assertResult(List(0) { it })
    }

    @Test
    fun loadRange_simple() {
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        trivialDataSource
            .loadRange(testParams)
            .test()
            .assertResult((0..9).toList())
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .loadRange(testParams)
            .subscribe(Consumer<List<Int>> { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test
    fun cancel() {
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        trivialDataSource
            .loadRange(testParams)
            .test(true)
            .assertComplete()
    }
}