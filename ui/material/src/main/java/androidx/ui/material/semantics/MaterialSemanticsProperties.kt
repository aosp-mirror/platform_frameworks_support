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

package androidx.ui.material.semantics

import androidx.ui.semantics.SemanticsProperty
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics properties that apply to the Material design framework.  Used for making assertions in
 * testing.
 */
class MaterialSemanticsProperties {
    companion object {
        val InMutuallyExclusiveGroup = SemanticsPropertyKey<Boolean>("InMutuallyExclusiveGroup")
        // TODO(ryanmentley): Can we think of a better name?
        val Selected = SemanticsPropertyKey<Boolean>("Selected")
        val ToggleState = SemanticsPropertyKey<Boolean>("ToggleState")
    }
}

internal var SemanticsPropertyReceiver.inMutuallyExclusiveGroup
        by SemanticsProperty(MaterialSemanticsProperties.InMutuallyExclusiveGroup)

internal var SemanticsPropertyReceiver.selected
        by SemanticsProperty(MaterialSemanticsProperties.Selected)

internal var SemanticsPropertyReceiver.toggleState
        by SemanticsProperty(MaterialSemanticsProperties.ToggleState)