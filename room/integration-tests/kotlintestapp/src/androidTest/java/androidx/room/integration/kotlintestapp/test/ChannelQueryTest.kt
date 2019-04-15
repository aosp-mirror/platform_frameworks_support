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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ChannelQueryTest : TestDatabaseTest() {

    @Test
    fun receiveBooks() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val channel = booksDao.getBooksReceiveChannel()
            assertThat(channel.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
            assertThat(channel.isEmpty).isTrue()
        }
    }

    @Test
    fun receiveBooksUpdate() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val channel = booksDao.getBooksReceiveChannel()

            assertThat(channel.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

            booksDao.insertBookSuspend(TestUtil.BOOK_3)

            assertThat(channel.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
            assertThat(channel.isEmpty).isTrue()
        }
    }

    @Test
    fun receiveBooksLatestUpdate() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val channel = booksDao.getBooksReceiveChannel()

            assertThat(channel.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

            booksDao.insertBookSuspend(TestUtil.BOOK_3)
            booksDao.deleteBookSuspend(TestUtil.BOOK_1)

            assertThat(channel.receive())
                .isEqualTo(listOf(TestUtil.BOOK_2, TestUtil.BOOK_3))
            assertThat(channel.isEmpty).isTrue()
        }
    }

    @Test
    fun receiveBooksUpdate_closed() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val channel = coroutineScope {
                booksDao.getBooksReceiveChannel()
            }

            assertThat(channel.isClosedForReceive).isTrue()
            try {
                channel.receive()
                fail("receive() should have thrown a closed exception.")
            } catch (ex: ClosedReceiveChannelException) {
                // no-op
            }
        }
    }
}