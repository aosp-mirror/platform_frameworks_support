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

package androidx.ui.baseui.selection

import androidx.ui.core.Semantics
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick
import androidx.ui.semantics.value

@Composable
fun Toggleable(
    value: ToggleableState = ToggleableState.Checked,
    onToggle: (() -> Unit)? = null,
    @Children children: @Composable() () -> Unit
) {
    PressReleasedGestureDetector(
        onRelease = onToggle,
        consumeDownOnStart = false
    ) {
        // TODO: enabled should not be hardcoded
        // TODO(pavlis): Handle multiple states for Semantics
        Semantics(properties = {
            this.value = if (value == ToggleableState.Checked) "Checked" else "Unchecked"
            this.enabled = true
            if (onToggle != null) {
                onClick(action = onToggle)
            }
        }) {
            children()
        }
    }
}

enum class ToggleableState {
    Checked,
    Unchecked,
    Indeterminate
}

internal fun ToggleableState(checked: Boolean) = when (checked) {
    true -> ToggleableState.Checked
    false -> ToggleableState.Unchecked
}