<<<<<<< HEAD   (be0ce7 Merge "Merge empty history for sparse-5662278-L1600000033295)
=======
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

import java.util.concurrent.Executor

/**
 * An [Executor] that runs each task in the thread that invokes [execute][Executor.execute]
 *
 * This instance is equivalent to:
 *```
 * final class DirectExecutor implements Executor {
 *     public void execute(Runnable r) {
 *         r.run();
 *     }
 * }
 *```
 */
internal val DirectExecutor = Executor { runnable: Runnable -> runnable.run() }
>>>>>>> BRANCH (e55c95 Merge "Merge cherrypicks of [990151, 990154] into sparse-568)
