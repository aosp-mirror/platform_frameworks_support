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
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.surface.Surface
import androidx.ui.material.themeColor

/**
 * This is a level 3 API, where the user uses the [SemanticActionBuilder] to build the action.
 * This component provides default values for all the parameters to the builder, the developer has
 * to just supply the callback lambda.
 */
@Suppress("FunctionName", "Unused")
@Composable
fun ClickInteraction(
    click: SemanticActionBuilder<Unit>.() -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val clickAction = SemanticActionBuilder(phrase = "Click", defaultParam = Unit)
        .apply(click)
        .also {
            if (it.types.none { type -> type is AccessibilityAction }) {
                it.types += AccessibilityAction.Primary
            }
        }.build()

    Semantics(actions = setOf(clickAction)) {
        PressGestureDetectorWithActions(onRelease = clickAction) { children() }
    }
}

/**
 * Builder to create a semantic action.
 */
class SemanticActionBuilder<T>(
    var phrase: String,
    var defaultParam: T,
    var types: Set<ActionType> = setOf(),
    var action: (ActionParam<T>) -> Unit = {}
) {
    fun build() = SemanticAction(phrase, defaultParam, types, action)
}

/**
 * The Level 3 API provides a [Group] component, and we can add [GroupRow]s for each item we
 * want to navigate to.
 *
 * This component emits group semantics for a set of child composables.
 */
@Suppress("FunctionName", "Unused")
@Composable
fun Group(
    @Children block: GroupItemCollector.() -> Unit
) {
    val groupItems = GroupItemCollector().apply(block).groupItems
    val max = groupItems.count()

    val focusedIndex = +state { 0 }

    GroupSemantics(
        onNext = SemanticAction<Unit>(
            defaultParam = Unit,
            action = { focusedIndex.value = rollingIncrement(focusedIndex.value, max) }
        ),
        onPrevious = SemanticAction<Unit>(
            defaultParam = Unit,
            action = { focusedIndex.value = rollingDecrement(focusedIndex.value, max) }
        ),
        onDown = SemanticAction<Unit>(
            defaultParam = Unit,
            action = { focusedIndex.value = rollingIncrement(focusedIndex.value, max) }
        ),
        onUp = SemanticAction<Unit>(
            defaultParam = Unit,
            action = { focusedIndex.value = rollingDecrement(focusedIndex.value, max) }
        ),
        onGoTo = SemanticAction<Int>(
            defaultParam = focusedIndex.value,
            action = { focusedIndex.value = it.value }
        )) { groupActions ->
        MaterialTheme {
            Column {
                for (index in 0 until groupItems.size) {
                    PressGestureDetectorWithActions(
                        onRelease = SemanticAction(
                            phrase = "Select this item",
                            defaultParam = Unit,
                            action = {
                                groupActions.goTo.invoke(ActionCaller.PointerInput, index)
                            })
                    ) {
                        Surface(
                            shape = RoundedRectangleBorder(),
                            color = +themeColor {
                                if (focusedIndex.value == index) secondary else background
                            }) {
                            groupItems[index]()
                        }
                    }
                }
            }
        }
    }
}

private fun rollingIncrement(current: Int, max: Int) = (current + 1) % max

private fun rollingDecrement(current: Int, max: Int) = (current + max - 1) % max

/**
 * The [GroupItemCollector] is used by [Group] to collect all the child composables.
 */
class GroupItemCollector {
    private val _groupItems: MutableList<@Composable() () -> Unit> = mutableListOf()

    val groupItems: List<@Composable() () -> Unit>
        get() = _groupItems.toList()

    fun GroupRow(groupItem: @Composable() () -> Unit) {
        _groupItems.add(groupItem)
    }
}
