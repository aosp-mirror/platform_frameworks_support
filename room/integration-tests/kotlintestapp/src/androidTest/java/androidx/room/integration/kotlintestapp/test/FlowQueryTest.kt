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

package androidx.room.integration.kotlintestapp.test

import androidx.room.integration.kotlintestapp.vo.Book
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit

@SmallTest
@FlowPreview
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FlowQueryTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle.
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun collectBooks_takeOne() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        booksDao.getBooksFlow().take(1).collect {
            assertThat(it)
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun collectBooks_async() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val latch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            booksDao.getBooksFlow().collect {
                assertThat(it)
                    .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
                latch.countDown()
            }
        }

        latch.await()
        job.cancelAndJoin()
    }

    @Test
    fun receiveBooks_async_update() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val barrier = Phaser(2)
        val results = mutableListOf<List<Book>>()
        val job = async(Dispatchers.IO) {
            booksDao.getBooksFlow().collect {
                when (results.size) {
                    0 -> {
                        results.add(it)
                        barrier.arrive()
                    }
                    1 -> {
                        results.add(it)
                        barrier.arrive()
                    }
                    else -> fail("Should have only collected 2 results.")
                }
            }
        }

        barrier.arriveAndAwaitAdvance()
        booksDao.insertBookSuspend(TestUtil.BOOK_3)

        barrier.arriveAndAwaitAdvance()
        assertThat(results.size).isEqualTo(2)
        assertThat(results[0])
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        assertThat(results[1])
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))

        job.cancelAndJoin()
    }
}