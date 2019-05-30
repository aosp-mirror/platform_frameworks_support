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

import androidx.ui.engine.text.TextDirection
import androidx.ui.internal.Unicode
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// TODO(ryanmentley): Too easy to use the wrong one of this vs SemanticsPropertyKey?
// Probably okay for a low-level API
open class SemanticsActionKey<T : Function<Unit>> :
    SemanticsPropertyKey<SemanticsAction<T>>(compareByPresence = true)


private /* inline */ class SemanticsProperty<T>(val propertyKey: SemanticsPropertyKey<T>) :
    ReadWriteProperty<SemanticsPropertyReceiver, T> {
    override fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        return thisRef[propertyKey]
    }

    override fun setValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>, value: T) {
        thisRef[propertyKey] = value
    }
}

private val labelKey = object : SemanticsPropertyKey<String>() {
    override fun canMerge(parentValue: String, childValue: String) = true

    override fun merge(parentValue: String, childValue: String): String {
        return parentValue + "\n" + childValue
    }
}
var SemanticsPropertyReceiver.label by SemanticsProperty(labelKey)

private val valueKey = object : SemanticsPropertyKey<String>() {
    // values cannot be merged
    override fun canMerge(parentValue: String, childValue: String) = false
}
var SemanticsPropertyReceiver.value by SemanticsProperty(valueKey)
//private val checkedKey = SemanticsPropertyKey<Boolean>()
//var SemanticsPropertyReceiver.checked by SemanticsProperty(checkedKey)

private val enabledKey = SemanticsPropertyKey<Boolean>()
var SemanticsPropertyReceiver.enabled by SemanticsProperty(enabledKey)

private val onClickKey = SemanticsActionKey<() -> Unit>()
var SemanticsPropertyReceiver.onClick by SemanticsProperty(onClickKey)

private val testTagKey = SemanticsPropertyKey<String>()
var SemanticsPropertyReceiver.testTag by SemanticsProperty(testTagKey)

private val textDirectionKey = SemanticsPropertyKey<TextDirection>()
var SemanticsPropertyReceiver.textDirection by SemanticsProperty(textDirectionKey)

// This might be a bad idea, it's easy to write onClick { onClickAction }
// when you mean onClick(onClickAction), thought it generates a compiler warning
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Unit) {
    this[onClickKey] = SemanticsAction(label, action)
}

//fun SemanticsPropertyReceiver.onClick(action: SemanticsAction) {
//    this[onClickKey] = action
//}


private val customActionsKey = SemanticsPropertyKey<List<SemanticsAction<() -> Unit>>>()
var SemanticsPropertyReceiver.customActions by SemanticsProperty(customActionsKey)


private fun concatStrings(
    thisString: String?,
    otherString: String?,
    thisTextDirection: TextDirection?,
    otherTextDirection: TextDirection?
): String? {
    if (otherString.isNullOrEmpty())
        return thisString
    var nestedLabel = otherString
    if (thisTextDirection != otherTextDirection && otherTextDirection != null) {
        nestedLabel = when (otherTextDirection) {
            TextDirection.Rtl -> "${Unicode.RLE}$nestedLabel${Unicode.PDF}"
            TextDirection.Ltr -> "${Unicode.LRE}$nestedLabel${Unicode.PDF}"
        }
    }
    if (thisString.isNullOrEmpty())
        return nestedLabel
    return "$thisString\n$nestedLabel"
}

