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

/**
 * Structure to hold the relationship between nodes and models.
 *
 * You can add a model to the node, iterate through all the models added for the node or
 * clear the models for the node.
 */
internal class NodesModelHolder<Node> {

    private val nodeToModels = mutableMapOf<Node, MutableList<Any>>()

    /**
     * Associate the [model] with this [node].
     */
    fun addModel(node: Node, model: Any) {
        val models = nodeToModels.getOrElse(node) {
            val set = mutableListOf<Any>()
            nodeToModels[node] = set
            set
        }
        models += model
    }

    /**
     * Iterate through the nodes associated with this [model].
     */
    fun forEachNode(model: Any, action: (Node) -> Unit) {
        // linear complexity, unfortunately we can't have a map <Model, Node> as the keys hashCode
        // can be changed every time a mutable @Model data class has been modified.
        nodeToModels.forEach { entry ->
            if (entry.value.contains(model)) {
                action(entry.key)
            }
        }
    }

    /**
     * Clear all the models previously associated with the [node].
     */
    fun clearModels(node: Node) {
        nodeToModels.remove(node)
    }
}
