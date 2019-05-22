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
fun SemanticsTreeInteraction.assertVisible(): SemanticsTreeInteraction {
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
fun SemanticsTreeInteraction.assertHidden(): SemanticsTreeInteraction {
    verify({ "The component is visible!" }) {
        it.isHidden
    }

    return this
}

/**
 * Asserts that there is no component that was matched by the query. If the component exists but is
 * hidden use [assertHidden] instead.
 */
fun SemanticsTreeInteraction.assertDoesNotExist() {
    val foundNodes = find(ignoreCountLimit = true)

    if (foundNodes.isNotEmpty()) {
        throw AssertionError("Assert failed: Found '${foundNodes.size}' nodes. Expected '0' nodes.")
    }
}

/**
 * Asserts that current component is visible.
 */
// TODO(pavlis): Provide guarantees of being visible VS being actually displayed
fun SemanticsTreeInteraction.assertChecked(): SemanticsTreeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is not checked!" }) {
        it.isChecked == true
    }
    return this
}

fun SemanticsTreeInteraction.assertNotChecked(): SemanticsTreeInteraction {
        // TODO(pavlis): Throw exception if component is not checkable
    verify({ "The component is checked!" }) {
        it.isChecked != true
    }
    return this
}

fun SemanticsTreeInteraction.assertSelected(): SemanticsTreeInteraction {
        // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be selected" }) {
        it.isSelected == true
    }
    return this
}

fun SemanticsTreeInteraction.assertNotSelected(): SemanticsTreeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to not be selected!" }) {
        it.isSelected == false
    }
    return this
}

fun SemanticsTreeInteraction.assertInMutuallyExclusiveGroup(): SemanticsTreeInteraction {
    // TODO(pavlis): Throw exception if component is not selectable
    verify(
        { "The component is expected to be mutually exclusive group, but it's not!" }) {
        it.isInMutuallyExclusiveGroup
    }
    return this
}

fun SemanticsTreeInteraction.assertValueEquals(value: String): SemanticsTreeInteraction {
        verify({ node -> "Expected value: $value Actual value: ${node.value}" }) {
        it.value == value
    }
    return this
}

fun SemanticsTreeInteraction.assertSemanticsIsEqualTo(
    expectedProperties: SemanticsConfiguration
): SemanticsTreeInteraction {
    val nodes = find(false)

    nodes.forEach {
        it.data.assertEquals(expectedProperties)
    }

    return this
}

internal fun SemanticsTreeInteraction.verify(
    assertionMessage: (SemanticsConfiguration) -> String,
    condition: (SemanticsConfiguration) -> Boolean
) {
    val nodes = find(false)

    nodes.forEach {
        if (!condition.invoke(it.data)) {
            // TODO(b/133217292)
            throw AssertionError("Assert failed: ${assertionMessage(it.data)}")
        }
    }
}