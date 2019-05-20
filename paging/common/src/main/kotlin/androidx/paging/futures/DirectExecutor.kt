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

package androidx.paging.futures

import androidx.annotation.RestrictTo

import java.util.concurrent.Executor

/**
 * Executor that runs each task in the thread that invokes [execute][Executor.execute]
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DirectExecutor private constructor() : Executor {
    override fun execute(runnable: Runnable) {
        runnable.run()
    }

    companion object {
        /**
         * Returns an [Executor] that runs each task in the thread that invokes [ ][Executor.execute].
         *
         *
         * This instance is equivalent to:
         *
         * <pre>`final class DirectExecutor implements Executor {
         * public void execute(Runnable r) {
         * r.run();
         * }
         * }
        `</pre> *
         */
        var INSTANCE = DirectExecutor()
    }
}
