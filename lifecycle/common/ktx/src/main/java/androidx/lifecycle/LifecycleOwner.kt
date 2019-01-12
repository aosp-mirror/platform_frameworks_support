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

package androidx.lifecycle

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * [CoroutineScope] tied to this [LifecycleOwner]'s [Lifecycle].
 *
 * This scope will be canceled when the [Lifecycle] is destroyed.
 *
 * This scope is bound to [Dispatchers.Main]
 */
val LifecycleOwner.scope: CoroutineScope
    get() = lifecycle.scope

/**
 * Runs the given [block] on [Dispatchers.Main] when the [LifecycleOwner]'s [Lifecycle] is at
 * least in the [Lifecycle.State.STARTED] state. If the [Lifecycle] is already destroyed, this
 * method will throw [LifecycleDestroyedException] which is a specific type of
 * [CancellationException]. If the [Lifecycle] is not started yet, this
 * method will suspend until the [Lifecycle] starts. If it gets destroyed before it starts, this
 * method will fail with [LifecycleDestroyedException].
 *
 * The provided [block] is always run on the main dispatcher.
 *
 * see [Lifecycle.whenStarted]
 */

suspend inline fun <OUT> LifecycleOwner.whenStarted(
    crossinline block: () -> OUT
) = lifecycle.whenStarted(block)
