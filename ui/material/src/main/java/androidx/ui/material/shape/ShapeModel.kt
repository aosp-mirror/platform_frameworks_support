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
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.painting.Path

// TODO(Andrey: Documentation)
data class ShapeModel(
    val family: ShapeFamily,
    val topLeftCorner: Corner,
    val topRightCorner: Corner,
    val bottomRightCorner: Corner,
    val bottomLeftCorner: Corner,
    override val border: Border? = null
) : Shape {

    override fun DensityReceiver.applyToPath(path: Path, parentSize: PxSize) {
        val pxCorners = PxCorners(this@ShapeModel, parentSize)
        if (pxCorners.hasOnlyZeros()) {
            path.addRect(parentSize.toRect())
        } else {
            when (family) {
                ShapeFamily.Rounded -> {
                    path.addRoundedCorners(parentSize, pxCorners)
                }
                ShapeFamily.Cut -> {
                    path.addCutCorners(parentSize, pxCorners)
                }
            }
        }
    }
}

fun ShapeModel(family: ShapeFamily, corner: Corner = SharpCorner, border: Border? = null) =
    ShapeModel(family, corner, corner, corner, corner, border)

fun RoundedShapeModel(corner: Corner, border: Border? = null) =
    ShapeModel(ShapeFamily.Rounded, corner, border)

fun CutShapeModel(corner: Corner, border: Border? = null) =
    ShapeModel(ShapeFamily.Cut, corner, border)

val CircleShapeModel =  RoundedShapeModel(Corner(50))

val RectShapeModel = RoundedShapeModel(Corner(0.dp))

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

private /*inline*/ fun Path.addRoundedCorners(parentSize: PxSize, corners: PxCorners) {
    fun Px.toRadius() = Radius.circular(this.value)
    addRRect(
        RRect(
            rect = parentSize.toRect(),
            topLeft = corners.topLeft.toRadius(),
            topRight = corners.topRight.toRadius(),
            bottomRight = corners.bottomRight.toRadius(),
            bottomLeft = corners.bottomLeft.toRadius()
        )
    )
}

private /*inline*/ fun Path.addCutCorners(parentSize: PxSize, corners: PxCorners) {
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
}
