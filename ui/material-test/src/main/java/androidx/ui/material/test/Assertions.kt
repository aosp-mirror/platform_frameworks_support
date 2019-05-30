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

package androidx.ui.test

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.getOrNull
import androidx.ui.material.semantics.MaterialSemanticsProperties
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.semantics.value

// TODO(i18n): This whole file has hardcoded strings

/**
 * Asserts that the current component is checked.
 */
fun SemanticsNodeInteraction.assertIsChecked(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw nice exception if component is not toggleable?
    verify({ "Component is toggled off" }) {
        it[MaterialSemanticsProperties.ToggleState] == true
    }
    return assertValueEquals("Checked")
}
/**
 * Asserts that the current component is not checked.
 */
fun SemanticsNodeInteraction.assertIsNotChecked(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw nice exception if component is not checkable?
    verify({ "Component is toggled on" }) {
        it[MaterialSemanticsProperties.ToggleState] == false
    }
    return assertValueEquals("Unchecked")
}
/**
 * Asserts that the current component is selected.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw nice exception if component is not selectable?
    verify({ "Component is unselected" }) {
        it[MaterialSemanticsProperties.Selected] == true
    }
    return assertValueEquals("Selected")
}

fun SemanticsNodeInteraction.assertIsNotSelected(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw nice exception if component is not selectable?
    verify({ "Component is selected" }) {
        it[MaterialSemanticsProperties.Selected] == false
    }
    return assertValueEquals("Not selected")
}
