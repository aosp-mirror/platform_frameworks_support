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

import androidx.ui.internal.Unicode
import androidx.ui.text.style.TextDirection
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SemanticsActionKey<T : Function<Unit>>(name: String) :
    SemanticsPropertyKey<SemanticsAction<T>>(name = name, compareByPresence = true)

object SemanticsProperties {
    val Label = object : SemanticsPropertyKey<String>("Label") {
        override fun canMerge(parentValue: String, childValue: String) = true

        override fun merge(parentValue: String, childValue: String): String {
            // TODO(ryanmentley): Needs TextDirection, probably needs to pass both nodes
            //  to retrieve it
            return parentValue + "\n" + childValue
        }
    }

    val Value = SemanticsPropertyKey<String>("Value")

    val Enabled = SemanticsPropertyKey<Boolean>("Enabled")

    val Hidden = SemanticsPropertyKey<Boolean>("Hidden")

    val TestTag = SemanticsPropertyKey<String>("TestTag")

    val TextDirection = SemanticsPropertyKey<TextDirection>("TextDirection")
}

class SemanticsActions {
    companion object {
        val OnClick = SemanticsActionKey<() -> Unit>("OnClick")

        val CustomActions = SemanticsPropertyKey<List<SemanticsAction<() -> Unit>>>("CustomActions")
    }
}

var SemanticsPropertyReceiver.label by SemanticsProperties.Label

var SemanticsPropertyReceiver.value by SemanticsProperties.Value

var SemanticsPropertyReceiver.enabled by SemanticsProperties.Enabled

var SemanticsPropertyReceiver.hidden by SemanticsProperties.Hidden

var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

var SemanticsPropertyReceiver.textDirection by SemanticsProperties.TextDirection

var SemanticsPropertyReceiver.onClick by SemanticsActions.OnClick

fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Unit) {
    this[SemanticsActions.OnClick] = SemanticsAction(label, action)
}

var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/*
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
*/
