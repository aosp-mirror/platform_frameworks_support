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

package androidx.ui.material.shape

import androidx.ui.baseui.shape.Border
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.painting.Path

/**
 * This is the Material design specific implementation of [Shape].
 *
 * You can read the specification here:
 * https://material.io/design/shape/applying-shape-to-ui.html
 *
 * ShapesModel helps you to declare a component shape by specifying:
 * 1) A shape family
 * 2) All four corner sizes
 * 3) An optional border
 *
 * @param family the shape family to apply for the shape.
 * @param topLeftCorner the top left corner size.
 * @param topRightCorner the top right corner size.
 * @param bottomRightCorner the bottom right corner size.
 * @param bottomLeftCorner the bottom left corner size.
 * @param border an optional border to draw on top of the shape.
 */
data class ShapeModel(
    val family: ShapeFamily,
    val topLeftCorner: Corner,
    val topRightCorner: Corner,
    val bottomRightCorner: Corner,
    val bottomLeftCorner: Corner,
    override val border: Border? = null
) : Shape {

    override fun DensityReceiver.createOutline(size: PxSize): Outline {
        val pxCorners = PxCorners(this@ShapeModel, size)
        if (pxCorners.hasOnlyZeros()) {
            return Outline.Sharp(size.toRect())
        } else {
            return when (family) {
                ShapeFamily.Rounded -> roundedCorners(size, pxCorners)
                ShapeFamily.Cut -> cutCorners(size, pxCorners)
            }
        }
    }
}

/**
 * [ShapeModel] with an uniform [corner]s size.
 */
fun ShapeModel(family: ShapeFamily, corner: Corner, border: Border? = null) =
    ShapeModel(family, corner, corner, corner, corner, border)

/**
 * Rounded [ShapeModel] with an uniform [corner]s size.
 */
fun RoundedShapeModel(corner: Corner, border: Border? = null) =
    ShapeModel(ShapeFamily.Rounded, corner, border)

/**
 * Cut [ShapeModel] with an uniform [corner]s size.
 */
fun CutShapeModel(corner: Corner, border: Border? = null) =
    ShapeModel(ShapeFamily.Cut, corner, border)

/**
 * Circular [ShapeModel] with all the [Corner]s sized as the 50 percent of the shape size.
 */
val CircleShapeModel = RoundedShapeModel(Corner(50))

/**
 * Rectangular [ShapeModel] with the uniform [SharpCorner]s.
 */
val RectShapeModel = RoundedShapeModel(SharpCorner)

private data class PxCorners(
    val topLeft: Px,
    val topRight: Px,
    val bottomRight: Px,
    val bottomLeft: Px
) {
    init {
        if (topLeft < 0.px || topRight < 0.px || bottomRight < 0.px || bottomLeft < 0.px) {
            throw IllegalArgumentException("Corner size in Px can't be negative!")
        }
    }
}

private /*inline*/ fun PxCorners.hasOnlyZeros() =
    topLeft + topRight + bottomLeft + bottomRight == 0.px

private /*inline*/ fun DensityReceiver.PxCorners(shape: ShapeModel, parentSize: PxSize) =
    with(shape) {
        PxCorners(
            topLeft = topLeftCorner(parentSize),
            topRight = topRightCorner(parentSize),
            bottomRight = bottomRightCorner(parentSize),
            bottomLeft = bottomLeftCorner(parentSize)
        )
    }

private /*inline*/ fun roundedCorners(parentSize: PxSize, corners: PxCorners): Outline.Rounded {
    fun Px.toRadius() = Radius.circular(this.value)
    return Outline.Rounded(
        RRect(
            rect = parentSize.toRect(),
            topLeft = corners.topLeft.toRadius(),
            topRight = corners.topRight.toRadius(),
            bottomRight = corners.bottomRight.toRadius(),
            bottomLeft = corners.bottomLeft.toRadius()
        )
    )
}

private /*inline*/ fun cutCorners(parentSize: PxSize, corners: PxCorners) =
    Outline.Convex(Path().apply {
        var cornerSize = corners.topLeft.value
        moveTo(0f, cornerSize)
        lineTo(cornerSize, 0f)
        cornerSize = corners.topRight.value
        lineTo(parentSize.width.value - cornerSize, 0f)
        lineTo(parentSize.width.value, cornerSize)
        cornerSize = corners.bottomRight.value
        lineTo(parentSize.width.value, parentSize.height.value - cornerSize)
        lineTo(parentSize.width.value - cornerSize, parentSize.height.value)
        cornerSize = corners.bottomLeft.value
        lineTo(cornerSize, parentSize.height.value)
        lineTo(0f, parentSize.height.value - cornerSize)
        close()
    })
