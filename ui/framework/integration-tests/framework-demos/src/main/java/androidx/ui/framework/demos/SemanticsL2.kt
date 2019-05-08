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

/**
 * This is a level 2 API. This component makes it easier to find/discover available properties.
 */
@Suppress("FunctionName")
@Composable
fun SemanticProperties(
    label: String = "",
    visibility: Visibility = Visibility.Undefined,
    actions: Set<SemanticAction<out Any?>> = setOf(),
    @Children children: @Composable() () -> Unit
) {
    val propertySet = mutableSetOf<SemanticProperty<out Any>>()

    if (!label.isEmpty()) {
        propertySet.add(Label(label))
    }

    if (visibility != Visibility.Undefined) {
        propertySet.add(visibility)
    }

    Semantics(properties = propertySet, actions = actions) { children() }
}

/**
 * This is a component that emits a semantic node with a single action.
 *
 * Since this example does not have node merging implemented, we just creates an semantic action
 * using the supplied parameters and then invokes the supplied lambda.
 *
 * SemanticAction(params) { semanticAction->
 * ...
 * }
 *
 * For now the [Properties] component accepts a set of actions, but once this is finally
 * implemented, we will merge the nodes automatically.
 */
@Suppress("FunctionName")
@Composable
fun <T> SemanticAction(
    phrase: String = "",
    defaultParam: T,
    types: Set<ActionType> = setOf(),
    action: (ActionParam<T>) -> Unit,
    @Children block: @Composable() (SemanticAction<T>) -> Unit
) {
    val semanticAction = SemanticAction<T>(phrase, defaultParam, types, action)
    block.invoke(semanticAction)
}

/**
 * [GroupSemantics] calls a lambda wih a [GroupSemanticActions] parameter. This gives the child
 * block access to the group actions.
 */
class GroupSemanticActions(
    val enter: SemanticAction<Unit>,
    val exit: SemanticAction<Unit>,
    val next: SemanticAction<Unit>,
    val previous: SemanticAction<Unit>,
    val up: SemanticAction<Unit>,
    val down: SemanticAction<Unit>,
    val goTo: SemanticAction<Int>
)

/**
 * [GroupSemantics] is a wrapper around a [Semantics] component that adds group action types to
 * the semantic actions passed by the user.
 */
@Suppress("FunctionName", "Unused")
@Composable
fun GroupSemantics(
    onEnter: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onExit: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onNext: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onPrevious: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onUp: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onDown: SemanticAction<Unit> = SemanticAction(defaultParam = Unit, action = {}),
    onGoTo: SemanticAction<Int> = SemanticAction(defaultParam = 0, action = {}),
    @Children children: @Composable() (GroupSemanticActions) -> Unit
) {
    // Add action types to the actions (If they aren't added).
    val groupSemanticActions = GroupSemanticActions(
        enter = onEnter.setPhraseAndType("Enter", GroupActions.Enter),
        exit = onExit.setPhraseAndType("Exit", GroupActions.Exit),
        next = onNext.setPhraseAndType("Next", GroupActions.Next),
        previous = onPrevious.setPhraseAndType("Previous", GroupActions.Previous),
        up = onUp.setPhraseAndType("Up", GroupActions.Up),
        down = onDown.setPhraseAndType("Down", GroupActions.Down),
        goTo = onGoTo.setPhraseAndType("Go To", GroupActions.GoTo)
    )

    Semantics(
        actions = setOf(
            groupSemanticActions.enter,
            groupSemanticActions.exit,
            groupSemanticActions.next,
            groupSemanticActions.previous,
            groupSemanticActions.up,
            groupSemanticActions.down,
            groupSemanticActions.goTo
        )
    ) {
        children(groupSemanticActions)
    }
}

private fun <T> SemanticAction<T>.setPhraseAndType(phrase: String, type: ActionType) =
    SemanticAction(
        phrase = phrase,
        defaultParam = defaultParam,
        action = action,
        types = setOf(type)
    )
