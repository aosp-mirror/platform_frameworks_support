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

/**
 * Asserts that current component is visible.
 */
// TODO(b/123702531): Provide guarantees of being visible VS being actually displayed
fun SemanticsNodeInteraction.assertVisible(): SemanticsNodeInteraction {
    verify({ "The component is not visible!" }) {
        !it.isHidden
    }
    return this
}

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [assertDoesNotExist]
 */
fun SemanticsNodeInteraction.assertHidden(): SemanticsNodeInteraction {
    verify({ "The component is visible!" }) {
        it.isHidden
    }

    return this
}

/**
 * Asserts that the component isn't part of the component tree anymore. If the component exists but
 * is hidden use [assertHidden] instead.
 */
fun SemanticsNodeInteraction.assertDoesNotExist() {
    val foundNodes = semanticsTreeInteraction.findAllMatching()

    if (foundNodes.contains(this)) {
        throw AssertionError("Assert failed: The component does exist!")
    }
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun SemanticsNodeInteraction.assertChecked(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

fun SemanticsNodeInteraction.assertNotChecked(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is checked!" }) {
        it.isChecked != true
    }
    return this
}

fun SemanticsNodeInteraction.assertSelected(): SemanticsNodeInteraction {
        // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be selected" }) {
        it.isSelected == true
    }
    return this
}

fun SemanticsNodeInteraction.assertNotSelected(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to not be selected!" }) {
        it.isSelected == false
    }
    return this
}

fun SemanticsNodeInteraction.assertInMutuallyExclusiveGroup(): SemanticsNodeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction {
        verify({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }
    return this
}

fun SemanticsNodeInteraction.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SemanticsNodeInteraction {
    semanticsTreeNode.data.assertEquals(expectedProperties)

    return this
}

fun List<SemanticsNodeInteraction>.assertCountEquals(
    count: Int
): List<SemanticsNodeInteraction> {
    if (size != count) {
        throw AssertionError("Found '$size' nodes but exactly '$count' was expected!")
    }

    return this
}

internal fun SemanticsNodeInteraction.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    if (!condition.invoke(semanticsTreeNode.data)) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: ${assertionMessage(semanticsTreeNode.data)}")
    }
}