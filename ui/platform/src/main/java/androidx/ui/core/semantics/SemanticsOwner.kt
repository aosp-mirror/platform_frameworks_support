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

package androidx.ui.core.semantics

import androidx.ui.semantics.SemanticsAction
import androidx.ui.semantics.SemanticsPropertyKey

// TODO(ryanmentley): Clean up and integrate this (probably with AndroidCraneView)

/**
 * Owns [SemanticsNode] objects and notifies listeners of changes to the
 * render tree semantics.
 */
class SemanticsOwner {
    internal val dirtyNodes: MutableSet<SemanticsNode> = mutableSetOf()
    internal val nodes: MutableMap<Int, SemanticsNode> = mutableMapOf()
    internal val detachedNodes: MutableSet<SemanticsNode> = mutableSetOf()

    /**
     * The root node of the semantics tree, if any.
     *
     * If the semantics tree is empty, returns null.
     */
    internal val rootSemanticsNode: SemanticsNode?
        get() = nodes[0]

    private fun dispose() {
        dirtyNodes.clear()
        nodes.clear()
        detachedNodes.clear()
    }

    private fun <T : Function<Unit>> getSemanticsActionHandlerForId(
        id: Int,
        action: SemanticsPropertyKey<SemanticsAction<T>>
    ): SemanticsAction<*>? {
        var result: SemanticsNode? = nodes[id]
        if (result != null && result.isPartOfNodeMerging && !result.canPerformAction(action)) {
            result.visitDescendants { node: SemanticsNode ->
                if (node.canPerformAction(action)) {
                    result = node
                    return@visitDescendants false // found node, abort walk
                }
                return@visitDescendants true // continue walk
            }
        }
        if (result?.canPerformAction(action) != true) {
            return null
        }
        return result!!.config[action]
    }

    /**
     * Asks the [SemanticsNode] with the given id to perform the given action.
     *
     * If the [SemanticsNode] has not indicated that it can perform the action,
     * this function does nothing.
     *
     * If the given `action` requires arguments they need to be passed in via
     * the `args` parameter.
     */
    fun <T : Function<Unit>> performAction(id: Int, action: SemanticsPropertyKey<SemanticsAction<T>>, args: Any? = null) {
        TODO()
//        getSemanticsActionHandlerForId(id, action)?.invokeHandler(args)
    }
}