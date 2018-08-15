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

package androidx.room.writer.binderprovider

import androidx.room.writer.binder.ShortcutMethodBinder
import javax.lang.model.type.DeclaredType

/**
 * Provider for shortcut method binders
 */
interface ShortcutMethodBinderProvider {

    /**
     * Check whether the [DeclaredType] can be handled by the [ShortcutMethodBinder]
     */
    fun matches(declared: DeclaredType): Boolean

    /**
     * Provider of [ShortcutMethodBinder], based on the [DeclaredType]
     */
    fun provide(declared: DeclaredType): ShortcutMethodBinder
}