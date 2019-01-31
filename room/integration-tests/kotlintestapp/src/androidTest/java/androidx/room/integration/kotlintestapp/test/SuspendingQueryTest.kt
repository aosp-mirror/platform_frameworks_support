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

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.NewThreadDispatcher
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.runSuspendingTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class SuspendingQueryTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle (transaction thread released).
        countingTaskExecutorRule.drainTasks(100, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

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
    fun runSuspendingTransaction_ioDispatcher() {
        runBlocking(Dispatchers.IO) {
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
                    throw IOException("Boom!")
                }
                fail("An exception should have been thrown.")
            } catch (ex: IOException) {
                assertThat(ex).hasMessageThat()
                    .contains("Boom")
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
    fun runSuspendingTransaction_nested_exception() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                try {
                    database.runSuspendingTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        throw IOException("Boom!")
                    }
                    fail("An exception should have been thrown.")
                } catch (ex: IOException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Boom")
                }
            }

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(emptyList<Book>())
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

            var insertAttempted = false
            val job = launch(Dispatchers.IO) {
                database.runSuspendingTransaction {
                    // insert before delaying, to then assert transaction is not committed
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    insertAttempted = true
                    // delay so we can cancel
                    delay(Long.MAX_VALUE)
                }
            }

            delay(200)
            job.cancelAndJoin()

            booksDao.insertBookSuspend(TestUtil.BOOK_3)

            assertThat(insertAttempted).isTrue() // make sure we attempted to insert
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_3))
        }
    }

    @Test
    fun runSuspendingTransaction_cancelCoroutine_beforeThreadAcquire() {
        runBlocking {
            val job = launch {
                database.runSuspendingTransaction {
                    fail("This coroutine should never run.")
                }
            }

            yield()
            job.cancelAndJoin()
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
    fun runSuspendingTransaction_async_ioDispatcher() {
        runBlocking {
            database.runSuspendingTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                withContext(Dispatchers.IO) {
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    }
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_3)
                    }
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

    @Test
    @Suppress("DeferredResultUnused")
    fun runSuspendingTransaction_multipleTransactions_multipleThreads() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            async(newSingleThreadContext("asyncThread1")) {
                database.runSuspendingTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }

            async(newSingleThreadContext("asyncThread2")) {
                database.runSuspendingTransaction {
                    delay(100)
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

    @Test
    @Suppress("DeferredResultUnused")
    fun runSuspendingTransaction_leakTransactionContext_async() {
        runBlocking {
            val leakedContext = database.runSuspendingTransaction {
                coroutineContext
            }

            async(leakedContext) {
                fail("This coroutine should never run.")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_leakTransactionContext_launch() {
        runBlocking {
            val leakedContext = database.runSuspendingTransaction {
                coroutineContext
            }

            launch(leakedContext) {
                fail("This coroutine should never run.")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_leakTransactionContext_withContext() {
        runBlocking {
            val leakedContext = database.runSuspendingTransaction {
                coroutineContext
            }

            try {
                withContext(leakedContext) {
                    fail("This coroutine should never run.")
                }
                fail("An exception should have been thrown by withContext")
            } catch (ex: CancellationException) {
                // Ignored on-purpose
            }
        }
    }

    @Test
    fun runSuspendingTransaction_leakTransactionContext_runBlocking() {
        runBlocking {
            val leakedContext = database.runSuspendingTransaction {
                coroutineContext
            }

            try {
                runBlocking(leakedContext) {
                    fail("This coroutine should never run.")
                }
                fail("An exception should have been thrown by runBlocking")
            } catch (ex: CancellationException) {
                // Ignored on-purpose
            }
        }
    }

    @Test
    fun runSuspendingTransaction_busyExecutor() {
        runBlocking {
            val executorService = Executors.newSingleThreadExecutor()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .setQueryExecutor(executorService)
                .build()

            // Simulate a busy executor, no thread to acquire for transaction.
            val busyLatch = CountDownLatch(1)
            executorService.execute {
                busyLatch.await()
            }

            val job = async {
                localDatabase.runSuspendingTransaction {
                    fail("This coroutine should never run.")
                }
            }

            delay(200) // delay for async, transaction block shouldn't run (executor is busy)
            job.cancelAndJoin()

            // free busy thread
            busyLatch.countDown()
            executorService.awaitTermination(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun runSuspendingTransaction_shutdownExecutor() {
        runBlocking {
            val executorService = Executors.newCachedThreadPool()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .setQueryExecutor(executorService)
                .build()

            executorService.shutdownNow()

            try {
                localDatabase.runSuspendingTransaction {
                    fail("This coroutine should never run.")
                }
                fail("An exception should have been thrown by runSuspendingTransaction")
            } catch (ex: IllegalStateException) {
                assertThat(ex).hasMessageThat()
                    .contains("Unable to acquire a thread to perform the database transaction")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_databaseOpenError() {
        runBlocking {
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // this causes all transaction methods to throw, this can happen IRL
                        throw RuntimeException("Error opening Database.")
                    }
                })
                .build()

            try {
                localDatabase.runSuspendingTransaction {
                    fail("This coroutine should never run.")
                }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat()
                    .contains("Error opening Database.")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_beginTransaction_error() {
        runBlocking {
            // delegate and delegate just so that we can throw in beginTransaction()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .openHelperFactory(
                    object : SupportSQLiteOpenHelper.Factory {
                        val factoryDelegate = FrameworkSQLiteOpenHelperFactory()
                        override fun create(
                            configuration: SupportSQLiteOpenHelper.Configuration?
                        ): SupportSQLiteOpenHelper {
                            val helperDelegate = factoryDelegate.create(configuration)
                            return object : SupportSQLiteOpenHelper by helperDelegate {
                                override fun getWritableDatabase(): SupportSQLiteDatabase {
                                    val databaseDelegate = helperDelegate.writableDatabase
                                    return object : SupportSQLiteDatabase by databaseDelegate {
                                        override fun beginTransaction() {
                                            throw RuntimeException("Error beginning transaction.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                .build()

            try {
                localDatabase.runSuspendingTransaction {
                    fail("This coroutine should never run.")
                }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat()
                    .contains("Error beginning transaction")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_setTransactionSuccessful_error() {
        runBlocking {
            try {
                database.runSuspendingTransaction {
                    // ending transaction prematurely so that setTransactionSuccessful() invoked by
                    // runSuspendingTransaction throws.
                    database.endTransaction()
                }
            } catch (ex: IllegalStateException) {
                assertThat(ex).hasMessageThat()
                    .contains("Cannot perform this operation because there is no current " +
                            "transaction")
            }
        }
    }

    @Test
    fun runSuspendingTransaction_endTransaction_error() {
        runBlocking {
            try {
                database.runSuspendingTransaction {
                    // ending transaction prematurely and quickly throwing so that endTransaction()
                    // invoked by runSuspendingTransaction throws.
                    database.endTransaction()
                    // this exception will get swallowed by the exception thrown in endTransaction()
                    throw RuntimeException()
                }
            } catch (ex: IllegalStateException) {
                assertThat(ex).hasMessageThat()
                    .contains("Cannot perform this operation because there is no current " +
                            "transaction")
            }
        }
    }
}