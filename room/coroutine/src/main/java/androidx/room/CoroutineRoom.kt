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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import java.util.concurrent.Executor

/**
 * A helper class for supporting Kotlin Coroutines in Room.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CoroutineRoom {

    private var executorDispatcher: ExecutorDispatcher? = null

    internal fun getDispatcher(executor: Executor): CoroutineDispatcher {
        val executorDispatcher = executorDispatcher?.let {
            if (it.executor != executor) {
                ExecutorDispatcher(executor)
            } else {
                it
            }
        } ?: ExecutorDispatcher(executor)
        this.executorDispatcher = executorDispatcher
        return executorDispatcher.dispatcher
    }

    data class ExecutorDispatcher(val executor: Executor) {
        val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()
    }

    companion object {

        private val instance = CoroutineRoom()

        @JvmStatic
        suspend fun <R> execute(db: RoomDatabase, callable: Callable<R>): R {
            return withContext(instance.getDispatcher(db.queryExecutor)) {
                callable.call()
            }
        }
    }
}