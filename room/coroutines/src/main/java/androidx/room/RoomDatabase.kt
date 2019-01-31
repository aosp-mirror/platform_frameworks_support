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

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KFunction1

/**
 * Calls the specified suspending [block] in a database transaction. The transaction will be
 * marked as successful unless an exception is thrown in the suspending [block].
 *
 * Performing blocking database operations is not permitted in a coroutine context other than the
 * one inherited by the suspending block, it is recommended that all [Dao] function invoked within
 * the [block] be suspending functions.
 */
suspend fun <R> RoomDatabase.runSuspendingTransaction(block: suspend CoroutineScope.() -> R): R {
    // Use inherited transaction context if available, this allows nested suspending transactions.
    val transactionContext =
        coroutineContext[TransactionElement]?.transactionContext ?: createTransactionContext(this)
    return withContext(transactionContext) {
        val transactionElement = coroutineContext[TransactionElement]!!
        tryTransactionMethod(RoomDatabase::beginTransaction, transactionElement)
        try {
            // wrap suspending block in a new scope to wait for any child coroutine
            val result = coroutineScope {
                block.invoke(this)
            }
            setTransactionSuccessful()
            return@withContext result
        } finally {
            tryTransactionMethod(RoomDatabase::endTransaction, transactionElement)
            if (!inTransaction()) {
                // last transaction, release transaction thread
                transactionElement.release()
            }
        }
    }
}

/**
 * Creates a [CoroutineContext] for performing database operations within a coroutine transaction.
 *
 * The context is a combination of a dispatcher, a [TransactionElement] and a thread local element.
 *
 * * The dispatcher will dispatch coroutines to a single thread that is taken over from the Room
 * query executor. If the coroutine context is switched, suspending DAO functions will be able to
 * dispatch to the transaction thread.
 *
 * * The [TransactionElement] serves as an indicator for inherited context, meaning, if there is a
 * switch of context, suspending DAO methods will be able to use the indicator to dispatch the
 * database operation to the transaction thread.
 *
 * * The thread local element serves as a second indicator and marks threads that are used to
 * execute coroutines within the coroutine transaction, more specifically it allows us to identify
 * if a blocking DAO method is invoked within the transaction coroutine. The value itself is
 * currently meaningless, for now all we care is if its present or not.
 */
private suspend fun createTransactionContext(db: RoomDatabase): CoroutineContext {
    val isReleased = AtomicBoolean(false)
    val runnableQueue = LinkedBlockingQueue<Runnable>()
    // suspend until we acquire a thread from the query executor, preventing the queue from being
    // filled until we are ready to dispatch.
    suspendCancellableCoroutine<Unit> { continuation ->
        try {
            db.queryExecutor.execute {
                // thread acquired, resume coroutine
                continuation.resume(Unit)
                do {
                    val block = runnableQueue.take()
                    if (block == TransactionElement.RELEASE) {
                        break
                    } else {
                        block.run()
                    }
                } while (true)
            }
        } catch (ex: RejectedExecutionException) {
            // couldn't acquire a thread, cancel coroutine
            continuation.cancel(IllegalStateException(
                "Unable to acquire a thread to perform the database transaction.", ex))
        }
    }
    val dispatcher = Executor { command ->
        if (isReleased.get()) {
            // thread has been released, reject execution to allow dispatcher to resume finally
            // blocks and cancel the coroutine.
            throw RejectedExecutionException()
        } else {
            runnableQueue.put(command)
        }
    }.asCoroutineDispatcher()
    val transactionElement = TransactionElement(dispatcher, isReleased, runnableQueue)
    val threadLocalElement =
        db.suspendingTransactionId.asContextElement(System.identityHashCode(dispatcher))
    return dispatcher + transactionElement + threadLocalElement
}

/**
 * Safely invoke one of the transaction methods, releasing the transaction thread if they happen
 * to throw.
 */
private fun RoomDatabase.tryTransactionMethod(
    method: KFunction1<RoomDatabase, Unit>,
    transaction: TransactionElement
) {
    try {
        method.invoke(this)
    } catch (ex: Throwable) {
        transaction.release()
        throw ex
    }
}

/**
 * A [CoroutineContext.Element] that indicates there is an on-going database transaction.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TransactionElement(
    internal val transactionContext: CoroutineContext,
    private val transactionReleasedIndicator: AtomicBoolean,
    private val transactionQueue: BlockingQueue<Runnable>
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement

    fun release() {
        if (transactionReleasedIndicator.get()) {
            throw IllegalStateException("Release was already called on this transaction.")
        } else {
            transactionReleasedIndicator.set(true)
            transactionQueue.put(RELEASE)
        }
    }

    object RELEASE : Runnable {
        override fun run() {
            throw IllegalStateException("The release runnable should never be run.")
        }
    }
}