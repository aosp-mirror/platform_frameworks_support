<<<<<<< HEAD   (c6a768 Merge "Merge empty history for sparse-5330139-L6850000027064)
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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable

/**
 * A helper class for supporting Kotlin Coroutines in Room.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class CoroutinesRoom {

    companion object {
        @JvmStatic
        suspend fun <R> execute(db: RoomDatabase, callable: Callable<R>): R {
            if (db.isOpen && db.inTransaction()) {
                return callable.call()
            }
            return withContext(db.queryExecutor.asCoroutineDispatcher()) {
                callable.call()
            }
        }
    }
}
=======
>>>>>>> BRANCH (085152 Merge "Merge cherrypicks of [922394] into sparse-5359448-L96)
