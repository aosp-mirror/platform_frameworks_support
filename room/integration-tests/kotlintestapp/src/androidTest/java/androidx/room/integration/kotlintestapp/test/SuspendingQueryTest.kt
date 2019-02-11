/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.integration.kotlintestapp.NewThreadDispatcher
import androidx.room.runSuspendingTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SuspendingQueryTest : TestDatabaseTest() {
    @Test
    fun bookByIdSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId))
                .isEqualTo(TestUtil.BOOK_1)
        }
    }

    @Test
    fun allBookSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val books = booksDao.getBooksSuspend()

            assertThat(books.size).isEqualTo((2))
            assertThat(books[0]).isEqualTo(TestUtil.BOOK_1)
            assertThat(books[1]).isEqualTo(TestUtil.BOOK_2)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction() {
        runBlocking {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        runBlocking {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction_blockingDaoMethods() {
        runBlocking {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                booksDao.addBooks(TestUtil.BOOK_1.copy(salesCnt = 0), TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        runBlocking {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction_newThreadDispatcher() {
        runBlocking(NewThreadDispatcher()) {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun suspendingBlock_blockingDaoMethods() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1))
        }
    }

    @Test
    fun runSuspendingTransaction() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_newThreadDispatcher() {
        runBlocking(NewThreadDispatcher()) {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
        }
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_contextSwitch() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                withContext(Dispatchers.IO) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_exception() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1)
            }

            try {
                database.runSuspendingTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    throw RuntimeException("Boom!")
                }
            } catch (ex: RuntimeException) {
                // Ignored on-purpose
            }

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1))
        }
    }

    @Test
    fun runSuspendingTransaction_nested() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                database.runSuspendingTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_nested_contextSwitch() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                withContext(Dispatchers.IO) {
                    database.runSuspendingTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_childCoroutine_defaultDispatcher() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                launch {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_childCoroutine_ioDispatcher() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                launch(Dispatchers.IO) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_cancelCoroutine() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )
            booksDao.insertBookSuspend(TestUtil.BOOK_1)

            val job = launch(Dispatchers.IO) {
                database.runSuspendingTransaction {
                    delay(Long.MAX_VALUE)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }

            yield()
            job.cancelAndJoin()

            booksDao.insertBookSuspend(TestUtil.BOOK_3)

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_3))
        }
    }

    @Test
    fun runSuspendingTransaction_blockingDaoMethods() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.addBooks(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.addBooks(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun runSuspendingTransaction_blockingDaoMethods_contextSwitch() {
        runBlocking {
            database.runSuspendingTransaction {
                // normal query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.getBook("b1")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // delete or update shortcut
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.deleteUnsoldBooks()
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // insert shortcut
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.insertPublisherVoid("p1", "publisher1")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // shared prepared query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.addPublishers(TestUtil.PUBLISHER)
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // prepared query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.deleteBookWithIds("b1", "b2")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }
            }
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun runSuspendingTransaction_async() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                async {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
                async(Dispatchers.Default) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                async(Dispatchers.IO) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_3)
                }
            }

            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun runSuspendingTransaction_multipleTransactions() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            async {
                database.runSuspendingTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }

            async {
                database.runSuspendingTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }

        runBlocking {
            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }
}