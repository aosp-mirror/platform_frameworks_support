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
package androidx.ui.core

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.semantics.SemanticsPropertyReceiver

// TODO(ryanmentley): This file is in the wrong package, move it as a standalone CL

@Composable
@Suppress("UNUSED_PARAMETER")
fun ReplaceSemantics(
    properties: SemanticsPropertyReceiver.() -> Unit,
    @Children children: @Composable() () -> Unit
) {
    children()
}

// @Composable
// fun TestTag(tag: String, @Children children: @Composable() () -> Unit) {
//    ReplaceSemantics(properties = {
//        testTag = tag
//    }) {
//        children()
//    }
//
//    ReplaceSemantics(properties = {
//        onClick = { /* some action code */ }
//    }) {
//        ComponentThatNeedsItsOnClickReplaced()
//    }
// }

@Composable
@Suppress("PLUGIN_ERROR")
fun Semantics(
    /**
     * If 'container' is true, this component will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors.
     *
     * Whether descendants of this component can add their semantic information
     * to the [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    container: Boolean = false,
    /**
     * Whether descendants of this component are allowed to add semantic
     * information to the [SemanticsNode] annotated by this widget.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    explicitChildNodes: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)? = null,
    @Children children: @Composable() () -> Unit
) {
    val providedTestTag = +ambient(TestTagAmbient)
    <SemanticsComponentNode
        container
        explicitChildNodes
        properties>
        TestTag(tag=DefaultTestTag) {
            children()
        }
    </SemanticsComponentNode>
}
