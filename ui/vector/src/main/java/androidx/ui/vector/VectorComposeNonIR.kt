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

package androidx.ui.vector

import androidx.compose.*
import androidx.ui.graphics.vectorgraphics.Group
import androidx.ui.graphics.vectorgraphics.VNode
import androidx.ui.graphics.vectorgraphics.Vector
import java.util.WeakHashMap

private val VECTOR_TREE_ROOTS = WeakHashMap<Vector, VectorTree>()

class VectorScope(val composer: VectorComposition)

private fun obtainVectorTree(container: Vector): VectorTree {
    var vectorTree = VECTOR_TREE_ROOTS[container]
    if (vectorTree == null) {
        vectorTree = VectorTree()
        VECTOR_TREE_ROOTS[container] = vectorTree
    }
    return vectorTree
}

fun composeVector(container: Vector, parent: CompositionReference?= null,
                  composable: @Composable() VectorScope.() -> Unit) {
    var root = VECTOR_TREE_ROOTS[container]
    if (root == null) {
        lateinit var composer: VectorComposer
        root = obtainVectorTree(container)
        root.context = CompositionContext.prepare(root, parent) {
            VectorComposer(container.root, this).also { composer = it }
        }
        root.scope = VectorScope(VectorComposition(composer))
    }
    root.composable = composable
    root.context.compose()
}


class VectorComposer(
    val root: VNode,
    recomposer: Recomposer
) : Composer<VNode>(SlotTable(), Applier(root, VectorApplyAdapter()), recomposer)

fun disposeVector(container: Vector, parent: CompositionReference? = null) {
    composeVector(container, parent) {}
    VECTOR_TREE_ROOTS.remove(container)
}

// TODO (njawad) Do we need this wrapper component?
private class VectorTree: Component() {

    lateinit var scope: VectorScope
    lateinit var composable: @Composable() VectorScope.() -> Unit
    lateinit var context: CompositionContext

    override fun compose() {
        with(context.composer) {
            startGroup(0) // TODO (njawad) what key should be used here?
            scope.composable()
            endGroup()
        }
    }
}

@PublishedApi
internal val VectorGroupKey = Object()

internal class VectorApplyAdapter: ApplyAdapter<VNode> {
    override fun VNode.start(instance: VNode) {
        //NO-OP
    }

    override fun VNode.insertAt(index: Int, instance: VNode) {
        obtainGroup().insertAt(index, instance)
    }

    override fun VNode.removeAt(index: Int, count: Int) {
        obtainGroup().remove(index, count)
    }

    override fun VNode.move(from: Int, to: Int, count: Int) {
        obtainGroup().move(from, to, count)
    }

    override fun VNode.end(instance: VNode, parent: VNode) {
        // NO-OP
    }

    fun VNode.obtainGroup(): Group {
        return when (this) {
            is Group -> this
            else -> throw IllegalArgumentException("Cannot only insert VNode into Group")
        }
    }
}

typealias VectorUpdater<T> = ComposerUpdater<VNode, T>

class VectorComposition(val composer: VectorComposer) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <V> Effect<V>.unaryPlus(): V = resolve(this@VectorComposition.composer)

    inline fun emit(
            key: Any,
            /*crossinline*/
            ctor: () -> VNode,
            update: VectorUpdater<VNode>.() -> Unit
    ) = with(composer) {
        startNode(key)
        @Suppress("UNCHECKED_CAST") val node = if (inserting) ctor().also { emitNode(it) }
        else useNode()
        VectorUpdater(this, node).update()
        endNode()
    }

    inline fun emit(
            key: Any,
            /*crossinline*/
            ctor: () -> VNode,
            update: VectorUpdater<VNode>.() -> Unit,
            children: () -> Unit
    ) = with(composer) {
        startNode(key)
        @Suppress("UNCHECKED_CAST")val node = if (inserting) ctor().also { emitNode(it) }
        else useNode()
        VectorUpdater(this, node).update()
        children()
        endNode()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun joinKey(left: Any, right: Any?): Any = composer.joinKey(left, right)

    inline fun call(
            key: Any,
            /*crossinline*/
            invalid: ViewValidator.() -> Boolean,
            block: () -> Unit
    ) = with(composer) {
        startGroup(key)
        if (ViewValidator(composer).invalid() || inserting) {
            startGroup(0)
            block()
            endGroup()
        } else {
            skipGroup(0)
        }
        endGroup()
    }

    inline fun <T> call(
            key: Any,
            /*crossinline*/
            ctor: () -> T,
            /*crossinline*/
            invalid: ViewValidator.(f: T) -> Boolean,
            block: (f: T) -> Unit
    ) = with(composer) {
        startGroup(key)
        val f = cache(true, ctor)
        if (ViewValidator(this).invalid(f) || inserting) {
            startGroup(0)
            block(f)
            endGroup()
        } else {
            skipGroup(0)
        }
        endGroup()
    }
}