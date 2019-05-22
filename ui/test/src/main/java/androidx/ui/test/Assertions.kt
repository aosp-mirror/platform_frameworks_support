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

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * Asserts that current component is visible.
 */
// TODO(b/123702531): Provide guarantees of being visible VS being actually displayed
fun SingleNodeQuery.assertIsVisible(): SingleNodeQuery {
    verify(findExactlyOne(), { "The component is not visible!" }) {
        !it.isHidden
    }
    return this
}

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [assertDoesNotExist]
 */
fun SingleNodeQuery.assertIsHidden(): SingleNodeQuery {
    verify(findExactlyOne(), { "The component is visible!" }) {
        it.isHidden
    }
    return this
}

/**
 * Asserts that there is no component that was matched by the query. If the component exists but is
 * hidden use [assertIsHidden] instead.
 */
fun SingleNodeQuery.assertDoesNotExist(): SingleNodeQuery {
    verify(findNone(), { "The component does exist!" }) {
        true
    }
    return this
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun SingleNodeQuery.assertIsChecked(): SingleNodeQuery {
        // TODO(pavlis): Throw exception if component is not checkable
    verify(findExactlyOne(), { "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

fun SingleNodeQuery.assertIsNotChecked(): SingleNodeQuery {
        // TODO(pavlis): Throw exception if component is not checkable
    verify(findExactlyOne(), { "The component is checked!" }) {
        it.isChecked != true
    }
    return this
}

fun SingleNodeQuery.assertIsSelected(expected: Boolean): SingleNodeQuery {
        // TODO(pavlis): Throw exception if component is not selectable
    verify(
        findExactlyOne(),
        { "The component is expected to be selected = '$expected', but it's not!" }) {
        it.isSelected == expected
    }
    return this
}

fun SingleNodeQuery.assertIsInMutuallyExclusiveGroup(): SingleNodeQuery {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        findExactlyOne(),
        { "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

fun SingleNodeQuery.assertValueEquals(value: String): SingleNodeQuery {
        verify(findExactlyOne(), { node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }
    return this
}

fun SingleNodeQuery.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SingleNodeQuery {
    verifyNoThrow(findExactlyOne()) {
        it.assertEquals(expectedProperties)
    }
    return this
}

fun MultipleNodesQuery.assertAreChecked(): MultipleNodesQuery {
    verify(findAtLeastOne(), { "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

internal fun verifyNoThrow(
    nodes: List<SemanticsTreeNode>,
    condition: (SemanticsConfiguration) -> Unit
) {
    nodes.forEach {
        condition.invoke(it.data)
    }
}

internal fun verify(
    nodes: List<SemanticsTreeNode>,
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    nodes.forEach {
        if (!condition.invoke(it.data)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(it.data)}")
        }
    }
}