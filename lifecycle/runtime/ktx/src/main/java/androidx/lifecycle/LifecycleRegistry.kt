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

import androidx.annotation.MainThread

/**
 * Provides property access to the current state via [Lifecycle.getCurrentState] and
 * [LifecycleRegistry.markState].
 *
 * Note changing the state in response to a lifecycle event should continue to be
 * done via [LifecycleRegistry.handleLifecycleEvent].
 */
@get:MainThread
@set:MainThread
var LifecycleRegistry.state: Lifecycle.State
    get() = currentState
    set(state) = markState(state)
