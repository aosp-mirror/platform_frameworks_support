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
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class RxPageKeyedDataSourceTest {
    private val executor = TestExecutor()
    private val trivialDataSource: RxPageKeyedDataSource<Int, Int>
    private val perpetuallyLoadingDataSource: RxPageKeyedDataSource<Int, Int>
    private val errorDataSource: RxPageKeyedDataSource<Int, Int>

    private lateinit var disposable: Disposable

    init {
        trivialDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> {
                val data = (0..(params.requestedLoadSize - 1)).toList()
                val prevPage = -params.requestedLoadSize
                val nextPage = params.requestedLoadSize
                return Single.just(InitialResult(data, 0, data.size, prevPage, nextPage))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> {
                val start = params.key - params.requestedLoadSize
                val end = params.key - 1
                return Single.just(Result((start..end).toList(), params.requestedLoadSize))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> {
                val start = params.key
                val end = params.requestedLoadSize - 1
                return Single.just(Result((start..end).toList(), params.requestedLoadSize))
                    .doOnSubscribe { disposable = it }
            }
        }

        perpetuallyLoadingDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { InitialResult<Int, Int>(Collections.emptyList(), 0, 0, 0, 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int, Int>(Collections.emptyList(), 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int, Int>(Collections.emptyList(), 0) }
                    .doOnSubscribe { disposable = it }
        }

        errorDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> =
                Single.error<InitialResult<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.error<Result<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.error<Result<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }
        }

        PagedListTestUtil.initDataSource(trivialDataSource, executor)
        PagedListTestUtil.initDataSource(perpetuallyLoadingDataSource, executor)
        PagedListTestUtil.initDataSource(errorDataSource, executor)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams<Int>(0, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.InitialResult<Int, Int>(List(0) { it }, 0, 0, 0, 0)
            )
        )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams<Int>(10, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(
                ListenablePageKeyedDataSource.InitialResult<Int, Int>(
                    (0..9).toList(), 0, 10, -10, 10
                )
            )
        )
    }

    @Test
    fun loadInitial_cancel() {
        val testParams = LoadInitialParams<Int>(10, false)
        val future = perpetuallyLoadingDataSource.loadInitial(testParams)
        future.cancel(true)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: CancellationException) {
            // Don't re-throw since entering this block means the test passed!
        }

        assertThat(disposable.isDisposed, `is`(true))
    }

    @Test
    fun loadAfter_empty() {
        val testParams = LoadParams(0, 0)
        val future = trivialDataSource.loadAfter(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(ListenablePageKeyedDataSource.Result<Int, Int>(List(0) { it }, 0))
        )
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 10)
        val future = trivialDataSource.loadAfter(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(ListenablePageKeyedDataSource.Result<Int, Int>((0..9).toList(), 10))
        )
    }

    @Test
    fun loadAfter_cancel() {
        val testParams = LoadParams(0, 10)
        val future = perpetuallyLoadingDataSource.loadAfter(testParams)
        future.cancel(true)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: CancellationException) {
            // Don't re-throw since entering this block means the test passed!
        }

        assertThat(disposable.isDisposed, `is`(true))
    }

    @Test
    fun loadBefore_empty() {
        val testParams = LoadParams(0, 0)
        val future = trivialDataSource.loadBefore(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(ListenablePageKeyedDataSource.Result<Int, Int>(List(0) { it }, 0))
        )
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 10)
        val future = trivialDataSource.loadBefore(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertThat(
            actual,
            equalTo(ListenablePageKeyedDataSource.Result<Int, Int>((-10..-1).toList(), 10))
        )
    }

    @Test
    fun loadBefore_cancel() {
        val testParams = LoadParams(0, 10)
        val future = perpetuallyLoadingDataSource.loadBefore(testParams)
        future.cancel(true)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: CancellationException) {
            // Don't re-throw since entering this block means the test passed!
        }

        assertThat(disposable.isDisposed, `is`(true))
    }

    @Test
    fun dispose() {
        var didSucceed = false
        val testParams = PageKeyedDataSource.LoadParams(0, 10)
        val testSubscription = perpetuallyLoadingDataSource
            .onLoadBefore(testParams)
            .subscribe(Consumer { didSucceed = true })

        testSubscription.dispose()
        assertThat(testSubscription.isDisposed, `is`(true))
        assertThat(didSucceed, `is`(false))
    }

    @Test()
    fun error() {
        val testParams = LoadParams(0, 10)
        val future = errorDataSource.loadBefore(testParams)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
        } catch (expected: ExecutionException) {
            // Don't re-throw since entering this block means the test passed!
        }
    }
}