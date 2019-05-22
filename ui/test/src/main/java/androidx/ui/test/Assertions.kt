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
fun NodeQuery.assertVisible(): NodeQuery {
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
fun NodeQuery.assertHidden(): NodeQuery {
    verify({ "The component is visible!" }) {
        it.isHidden
    }

    return this
}

/**
 * Asserts that there is no component that was matched by the query. If the component exists but is
 * hidden use [assertHidden] instead.
 */
fun NodeQuery.assertDoesNotExist() {
    val nodes = findAndCheckExpectation()

    if (nodes.isNotEmpty()) {
        throw AssertionError("Assert failed: Found '${nodes.size}' nodes. Expected '0' nodes.")
    }
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun NodeQuery.assertChecked(): NodeQuery {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

fun NodeQuery.assertNotChecked(): NodeQuery {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is checked!" }) {
        it.isChecked != true
    }
    return this
}

fun NodeQuery.assertSelected(expected: Boolean): NodeQuery {
        // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be selected = '$expected', but it's not!" }) {
        it.isSelected == expected
    }
    return this
}

fun NodeQuery.assertInMutuallyExclusiveGroup(): NodeQuery {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

fun NodeQuery.assertValueEquals(value: String): NodeQuery {
        verify({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }
    return this
}

fun NodeQuery.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): NodeQuery {
    verifyNoThrow {
        it.assertEquals(expectedProperties)
    }
    return this
}

internal fun NodeQuery.verifyNoThrow(
    condition: (SemanticsConfiguration) -> Unit
) {
    val nodes = findAndCheckExpectation()

    nodes.forEach {
        condition.invoke(it.data)
    }
}

internal fun NodeQuery.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    val nodes = findAndCheckExpectation()

    nodes.forEach {
        if (!condition.invoke(it.data)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(it.data)}")
        }
    }
}