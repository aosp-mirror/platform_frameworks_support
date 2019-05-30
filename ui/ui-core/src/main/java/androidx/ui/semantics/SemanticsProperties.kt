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

package androidx.ui.semantics

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SemanticsPropertyKey<T>(val name: String, val compareByPresence: Boolean = true) :
    ReadWriteProperty<SemanticsPropertyReceiver, T> {
    // Most properties can't be merged
    open fun canMerge(parentValue: T, childValue: T): Boolean = false

    /**
     * Subclasses that wish to implement merging should override this to output the merged value
     *
     * This implementation always throws IllegalStateException, as it should always be overridden if
     * [canMerge] returns `true`
     */
    open fun merge(parentValue: T, childValue: T): T {
        throw IllegalStateException(
            "Superclass merge function called - did you forget to override merge?"
        )
    }

    final override fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        return thisRef[this]
    }

    final override fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }
}

data class SemanticsAction<T : Function<Unit>>(val label: String?, val action: T)

interface SemanticsPropertyReceiver {
    /**
     * Retrieves the value for the given property, if one has been set.
     * If a value has not been set, throws [IllegalStateException]
     */
    operator fun <T> get(key: SemanticsPropertyKey<T>): T

    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}
