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

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@FlowPreview
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ChannelQueryTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle.
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun receiveBooks() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this)
        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_update() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this)

        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate

        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_update_multipleChannels() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channels = Array(4) {
            booksDao.getBooksFlow().produceIn(this)
        }

        channels.forEach {
            assertThat(it.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate

        channels.forEach {
            assertThat(it.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
            assertThat(it.isEmpty).isTrue()
            it.cancel()
        }
    }

    @Test
    fun receiveBooks_latestUpdateOnly() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this, Channel.CONFLATED)

        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate
        booksDao.deleteBookSuspend(TestUtil.BOOK_1)
        drain() // drain async invalidate

        yield() // yield for channel producer

        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_2, TestUtil.BOOK_3))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_closedChannel() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this)
        channel.cancel()

        assertThat(channel.isClosedForReceive).isTrue()
        try {
            channel.receive()
            fail("receive() should have thrown a closed exception.")
        } catch (ex: ClosedReceiveChannelException) {
            // no-op
        }

        channel.cancel()
    }

    @Test
    fun receiveBooks_async() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val latch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            for (result in booksDao.getBooksFlow().produceIn(this)) {
                assertThat(result)
                    .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
                latch.countDown()
            }
        }

        latch.await()
        job.cancelAndJoin()
    }
}