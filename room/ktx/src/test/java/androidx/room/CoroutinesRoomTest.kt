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

package androidx.room

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import java.util.concurrent.Callable
import kotlin.coroutines.ContinuationInterceptor

@FlowPreview
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class CoroutinesRoomTest {

    @Mock lateinit var database: RoomDatabase
    @Mock lateinit var invalidationTracker: InvalidationTracker

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(database.invalidationTracker).thenReturn(invalidationTracker)
    }

    @Test
    fun testCreateFlow() = testRun {
        var callableExecuted = false
        val flow = CoroutinesRoom.createFlow(
            db = database,
            inTransaction = false,
            tableNames = arrayOf("Pet"),
            callable = Callable { callableExecuted = true }
        )

        verifyZeroInteractions(invalidationTracker)
        assertThat(callableExecuted).isFalse()

        val job = async {
            flow.single()
        }
        yield(); yield() // yield for async and flow

        verify(invalidationTracker).addObserver(any<InvalidationTracker.Observer>())
        assertThat(callableExecuted).isTrue()

        job.cancelAndJoin()
        verify(invalidationTracker).removeObserver(any<InvalidationTracker.Observer>())
    }

    // Use runBlocking dispatcher as query dispatchers, keeps the tests consistent.
    private fun testRun(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        `when`(database.backingFieldMap)
            .thenReturn(mapOf("QueryDispatcher" to coroutineContext[ContinuationInterceptor]))
        block.invoke(this)
    }
}
