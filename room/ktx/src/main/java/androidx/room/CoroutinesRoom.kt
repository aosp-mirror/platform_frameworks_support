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

package androidx.room

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import kotlin.coroutines.coroutineContext

/**
 * A helper class for supporting Kotlin Coroutines in Room.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class CoroutinesRoom private constructor() {

    companion object {

        @JvmStatic
        suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            callable: Callable<R>
        ): R {
            if (db.isOpen && db.inTransaction()) {
                return callable.call()
            }

            // Use the transaction dispatcher if we are on a transaction coroutine, otherwise
            // use the database dispatchers.
            val context = coroutineContext[TransactionElement]?.transactionDispatcher
                ?: if (inTransaction) db.transactionDispatcher else db.queryDispatcher
            return withContext(context) {
                callable.call()
            }
        }

        @JvmStatic
        @ExperimentalCoroutinesApi
        fun <R> createChannel(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            callable: Callable<R>
        ): ReceiveChannel<@JvmSuppressWildcards R> {
            val context = if (inTransaction) db.transactionDispatcher else db.queryDispatcher
            val channel = Channel<R>(Channel.CONFLATED)
            val job = GlobalScope.launch(context) {
                if (channel.isClosedForSend) {
                    // Channel got closed even before this coroutine started, just complete early.
                    return@launch
                }

                // Observer channel receives signals from the invalidation tracker to perform query.
                val observerChannel = Channel<Unit>(Channel.CONFLATED)
                val observer = object : InvalidationTracker.Observer(tableNames) {
                    override fun onInvalidated(tables: MutableSet<String>) {
                        if (!channel.isClosedForSend) {
                            observerChannel.offer(Unit)
                        }
                    }
                }

                db.invalidationTracker.addObserver(observer)
                observerChannel.offer(Unit) // Initial signal to perform first query.
                try {
                    // Iterate until cancelled, transforming observer signals to query results and
                    // sending them to the receiver channel.
                    for (signal in observerChannel) {
                        channel.offer(callable.call())
                    }
                } finally {
                    db.invalidationTracker.removeObserver(observer)
                }
            }
            channel.invokeOnClose {
                job.cancel()
            }
            return channel
        }

        @JvmStatic
        @FlowPreview
        @ExperimentalCoroutinesApi
        fun <R> createFlow(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            callable: Callable<R>
        ): Flow<@JvmSuppressWildcards R> = flow {
            val channel = createChannel(db, inTransaction, tableNames, callable)
            try {
                // Iterate until cancelled emitting query channel received results.
                for (result in channel) {
                    emit(result)
                }
            } finally {
                channel.cancel()
            }
        }
    }
}

/**
 * Gets the query coroutine dispatcher.
 *
 * @hide
 */
internal val RoomDatabase.queryDispatcher: CoroutineDispatcher
    get() = backingFieldMap.getOrPut("QueryDispatcher") {
        queryExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher

/**
 * Gets the transaction coroutine dispatcher.
 *
 * @hide
 */
internal val RoomDatabase.transactionDispatcher: CoroutineDispatcher
    get() = backingFieldMap.getOrPut("TransactionDispatcher") {
        transactionExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher
