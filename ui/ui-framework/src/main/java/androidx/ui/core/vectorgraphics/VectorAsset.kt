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

package androidx.ui.core.vectorgraphics

import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import java.util.Stack

/**
 * Builder used to construct a Vector graphic tree.
 * This is useful for caching the result of expensive operations used to construct
 * a vector graphic for compose.
 * For example, the vector graphic could be serialized and downloaded from a server and represented
 * internally in a VectorAsset before it is composed through [DrawVector]
 * The generated VectorAsset is recommended to be memoized across composition calls to avoid
 * doing redundant work
 **/
class VectorAssetBuilder(
    val name: String = DefaultGroupName,
    val defaultWidth: Float,
    val defaultHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float
) {

    private val nodes = Stack<VectorGroup>()

    private var root = VectorGroup()
    private var isConsumed = false

    private var currentGroup: VectorGroup = root
        private set
        get() = nodes.peek()

    init {
        nodes.add(root)
    }

    /**
     * Create a new group and push it to the front of the stack of VectorAsset nodes
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun pushGroup(
        name: String = DefaultGroupName,
        rotate: Float = DefaultRotate,
        pivotX: Float = DefaultPivotX,
        pivotY: Float = DefaultPivotY,
        scaleX: Float = DefaultScaleX,
        scaleY: Float = DefaultScaleY,
        translateX: Float = DefaultTranslateX,
        translateY: Float = DefaultTranslateY,
        clipPathData: PathData = EmptyPath
    ): VectorAssetBuilder {
        ensureNotConsumed()
        val group = VectorGroup(
                name,
                rotate,
                pivotX,
                pivotY,
                scaleX,
                scaleY,
                translateX,
                translateY,
                clipPathData
        )
        nodes.add(group)
        currentGroup.addNode(group)
        return this
    }

    /**
     * Pops the topmost VectorGroup from this VectorAssetBuilder. This is used to indicate
     * that no additional VectorAsset nodes will be added to the current VectorGroup
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun popGroup(): VectorAssetBuilder {
        ensureNotConsumed()
        nodes.pop()
        return this
    }

    /**
     * Add a path to the VectorAsset graphic. This represents a leaf node in the VectorAsset graphics
     * tree structure
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun addPath(
        name: String = DefaultPathName,
        pathData: PathData,
        fill: BrushType = EmptyBrush,
        fillAlpha: Float = DefaultAlpha,
        stroke: BrushType = EmptyBrush,
        strokeAlpha: Float = DefaultAlpha,
        strokeLineWidth: Float = DefaultStrokeLineWidth,
        strokeLineCap: StrokeCap = DefaultStrokeLineCap,
        strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
        strokeLineMiter: Float = DefaultStrokeLineMiter
    ): VectorAssetBuilder {
        ensureNotConsumed()
        currentGroup.addNode(
                VectorPath(
                        name,
                        pathData,
                        fill,
                        fillAlpha,
                        stroke,
                        strokeAlpha,
                        strokeLineWidth,
                        strokeLineCap,
                        strokeLineJoin,
                        strokeLineMiter
                )
        )
        return this
    }

    /**
     * Construct a VectorAsset. This concludes the creation process of a VectorAsset graphic
     * This builder cannot be re-used to create additional VectorAsset instances
     * @return Thew newly created VectorAsset instance
     */
    fun build(): VectorAsset {
        ensureNotConsumed()
        val vectorImage = VectorAsset(
            name,
            defaultWidth,
            defaultHeight,
            viewportWidth,
            viewportHeight,
            root
        )

        // reset state in case this builder is used again
        nodes.clear()
        root = VectorGroup()
        nodes.add(root)

        isConsumed = true

        return vectorImage
    }

    /**
     * Throws IllegalStateException if the VectorAssetBuilder is already been consumed
     */
    fun ensureNotConsumed() {
        if (isConsumed) {
            throw IllegalStateException("VectorAssetBuilder is single use, create " +
                    "a new instance to create a new VectorAsset")
        }
    }
}

sealed class VectorNode

/**
 * Vector graphics object that is generated as a result of [VectorAssetBuilder]]
 * It can be composed and rendered by passing it as an argument to [DrawVector]
 */
class VectorAsset internal constructor(
    val name: String,
    val defaultWidth: Float,
    val defaultHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val root: VectorGroup
)

/**
 * Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * The transformations are applied in the order of scale, rotate then translate.
 */
class VectorGroup(
    val name: String = DefaultGroupName,
    val rotate: Float = DefaultRotate,
    val pivotX: Float = DefaultPivotX,
    val pivotY: Float = DefaultPivotY,
    val scaleX: Float = DefaultScaleX,
    val scaleY: Float = DefaultScaleY,
    val translateX: Float = DefaultTranslateX,
    val translateY: Float = DefaultTranslateY,
    val clipPathData: PathData = EmptyPath
) : VectorNode(), Iterable<VectorNode> {

    private var children = ArrayList<VectorNode>()

    internal fun addNode(node: VectorNode) {
        children.add(node)
    }

    override fun iterator(): Iterator<VectorNode> =
            VectorGroupIterator(children.iterator())

    /**
     * Wrapper iterator class to not expose mutability of underlying ArrayList iterator
     */
    class VectorGroupIterator(
        val childIterator: MutableIterator<VectorNode>
    ) : Iterator<VectorNode> {

        override fun hasNext(): Boolean = childIterator.hasNext()

        override fun next(): VectorNode = childIterator.next()
    }
}

/**
 * Leaf node of a Vector graphics tree. This specifies a path shape and parameters
 * to color and style the the shape itself
 */
class VectorPath(
    val name: String = DefaultPathName,
    val pathData: PathData,
    val fill: BrushType = EmptyBrush,
    val fillAlpha: Float = DefaultAlpha,
    val stroke: BrushType = EmptyBrush,
    val strokeAlpha: Float = DefaultAlpha,
    val strokeLineWidth: Float = DefaultStrokeLineWidth,
    val strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    val strokeLineMiter: Float = DefaultStrokeLineMiter
) : VectorNode()

/**
 * Composes a vector graphic into the composition tree based on the specification
 * provided by given [VectorAsset]
 */
@Composable
fun DrawVector(vectorImage: VectorAsset) {
    Vector(
        name = vectorImage.name,
        viewportWidth = vectorImage.viewportWidth,
        viewportHeight = vectorImage.viewportHeight,
        defaultWidth = vectorImage.defaultWidth,
        defaultHeight = vectorImage.defaultHeight) {
        RenderVectorGroup(group = vectorImage.root)
    }
}

@Composable
private fun RenderVectorGroup(group: VectorGroup) {
    for (vectorNode in group) {
        if (vectorNode is VectorPath) {
            Path(
                pathData = vectorNode.pathData,
                name = vectorNode.name,
                fill = vectorNode.fill,
                fillAlpha = vectorNode.fillAlpha,
                stroke = vectorNode.stroke,
                strokeAlpha = vectorNode.strokeAlpha,
                strokeLineWidth = vectorNode.strokeLineWidth,
                strokeLineCap = vectorNode.strokeLineCap,
                strokeLineJoin = vectorNode.strokeLineJoin,
                strokeLineMiter = vectorNode.strokeLineMiter
            )
        } else if (vectorNode is VectorGroup) {
            Group(
                name = vectorNode.name,
                rotate = vectorNode.rotate,
                scaleX = vectorNode.scaleX,
                scaleY = vectorNode.scaleY,
                translateX = vectorNode.translateX,
                translateY = vectorNode.translateY,
                pivotX = vectorNode.pivotX,
                pivotY = vectorNode.pivotY,
                clipPathData = vectorNode.clipPathData) {
                RenderVectorGroup(group = vectorNode)
            }
        }
    }
}