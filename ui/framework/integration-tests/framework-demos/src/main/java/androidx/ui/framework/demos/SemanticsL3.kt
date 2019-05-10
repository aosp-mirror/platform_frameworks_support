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

package androidx.ui.framework.demos

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.PxPosition

/**
 * This is a level 3 API, where the user uses the [SemanticActionBuilder] to build the action.
 * This component provides default values for all the parameters to the builder, the developer has
 * to just supply the callback lambda.
 */
@Suppress("FunctionName", "Unused")
@Composable
fun ClickInteraction(
    press: SemanticActionBuilder<PxPosition>.() -> Unit,
    release: SemanticActionBuilder<Unit>.() -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val pressedAction = SemanticActionBuilder(
        label = "On Press",
        defaultParam = PxPosition.Origin
    ).apply(press).build()

    val releasedAction = SemanticActionBuilder(
        label = "On Release",
        defaultParam = Unit
    ).apply(release).build()

    Semantics(actions = setOf(pressedAction, releasedAction)) {
        PressGestureDetectorWithActions(
            onPress = pressedAction,
            onRelease = releasedAction,
            onCancel = releasedAction
        ) { children() }
    }
}

/**
 * Builder to create a semantic action.
 */
class SemanticActionBuilder<T>(
    var label: String,
    var defaultParam: T,
    var types: Set<ActionType> = setOf(),
    var action: (ActionParam<T>) -> Unit = {}
) {
    fun build() = SemanticAction(label, defaultParam, types, action)
}

/**
 * Extension function to make the syntax cleaner when we use all default parameters for the action.
 */
fun <T> SemanticActionBuilder<T>.action(action: (ActionParam<T>) -> Unit = {}) {
    this.action = action
}
