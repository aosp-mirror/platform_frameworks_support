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
fun SingleNodeQuery.assertIsVisible() =
    verify({ "The component is not visible!" }) {
        !it.isHidden
    }

/**
 * Asserts that current component is hidden. This requires that the component actually exists in
 * the hierarchy and is hidden. If you want to actually verify that the component does not  exist
 * at all, please use [assertDoesNotExist]
 */
fun SingleNodeQuery.assertIsHidden() =
    verify({ "The component is visible!" }) {
        it.isHidden
    }

/**
 * Asserts that there is no component that was matched by the query. If the component exists but is
 * hidden use [assertIsHidden] instead.
 */
fun SingleNodeQuery.assertDoesNotExist(): SingleNodeQuery {
    val foundNodes = mBaseNodeQuery.findAllMatching()
    if (foundNodes.isNotEmpty()) {
        throw AssertionError("Found '${foundNodes.size}' nodes but 0 was expected!")
    }
    return this
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun SingleNodeQuery.assertIsChecked() =
    // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }

fun SingleNodeQuery.assertIsNotChecked() =
    // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is checked!" }) {
        it.isChecked != true
    }

fun SingleNodeQuery.assertIsSelected(expected: Boolean) =
    // TODO(pavlis): Throw exception if component is not selectable
    verify({ "The component is expected to be selected = '$expected', but it's not!" }) {
        it.isSelected == expected
    }

fun SingleNodeQuery.assertIsInMutuallyExclusiveGroup() =
    // TODO(pavlis): Throw exception if component is not selectable
    verify({ "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }

fun SingleNodeQuery.assertValueEquals(value: String) =
    verify({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }

fun SingleNodeQuery.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SingleNodeQuery {
    mBaseNodeQuery.verifyNoThrowInternal(::sanityCheck) {
        it.assertEquals(expectedProperties)
    }

    return this
}

fun MultipleNodesQuery.assertAreChecked() =
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }

internal fun SingleNodeQuery.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
): SingleNodeQuery {
    mBaseNodeQuery.verifyInternal(
        ::sanityCheck,
        assertionMessage,
        condition
    )

    return this
}

internal fun MultipleNodesQuery.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
): MultipleNodesQuery {
    mBaseNodeQuery.verifyInternal(
        ::sanityCheck,
        assertionMessage,
        condition
    )

    return this
}

internal fun BaseNodeQuery.verifyNoThrowInternal(
    sanityCheck: (List<SemanticsTreeNode>) -> Unit,
    condition: (SemanticsConfiguration) -> Unit
) {
    val foundNodes = findAllMatching()
    sanityCheck(foundNodes)

    foundNodes.forEach {
        condition.invoke(it.data)
    }
}

internal fun BaseNodeQuery.verifyInternal(
    sanityCheck: (List<SemanticsTreeNode>) -> Unit,
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    val foundNodes = findAllMatching()
    sanityCheck(foundNodes)

    foundNodes.forEach {
        if (!condition.invoke(it.data)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(it.data)}")
        }
    }
}