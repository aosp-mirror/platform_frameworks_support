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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@FlowPreview
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FlowQueryTest : TestDatabaseTest() {

    @Test
    fun collectBooks_takeOne() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            booksDao.getBooksFlow().take(1).collect { result ->
                assertThat(result)
                    .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
            }
        }
    }

    @Test
    fun receiveBooks_update() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val job = async {
                booksDao.getBooksFlow().take(2).toList()
            }

            // TODO: Plz rewrite this
            delay(500)
            booksDao.insertBookSuspend(TestUtil.BOOK_3)
            delay(500)

            val results = job.await()
            assertThat(results.size).isEqualTo(2)
            assertThat(results[0])
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
            assertThat(results[1])
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        }
    }
}