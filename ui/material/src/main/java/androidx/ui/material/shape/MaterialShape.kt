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
import androidx.ui.baseui.shape.corner.RoundedCornerShape
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.corner.CornerSize
import androidx.ui.baseui.shape.corner.CornerSizes

/**
 * Components can apply one of this styles to the shapes of their cornerSizes.
 *
 * The rectangular shape can be defined as a shape with [SharpCorner] of any family.
 */
enum class ShapeFamily {
    /**
     * Shape with the rounded cornerSizes. [Corner] size means a corner radius.
     */
    Rounded,
    /**
     * Shape with the cut cornerSizes. [Corner] size means a cut length.
     */
    Cut
}

/**
 * This is the Material design specific implementation of [Shape].
 *
 * You can find the specification here:
 * https://material.io/design/shape/applying-shape-to-ui.html
 *
 * @param family the shape family to apply for all the corners
 * @param cornerSizes the sizes of all four corners
 * @param border an optional border to draw on top of the shape.
 */
data class MaterialShape(
    val family: ShapeFamily,
    val cornerSizes: CornerSizes,
    val border: Border? = null
)

/**
 * A [MaterialShape] with the same corner size applied for all the corners.
 */
fun MaterialShape(
    family: ShapeFamily,
    cornerSize: CornerSize,
    border: Border? = null
) = MaterialShape(family, CornerSizes(cornerSize), border)

/**
 * Converts [MaterialShape] to [Shape]
 */
fun MaterialShape.toShape(): Shape {
    return when (family) {
        ShapeFamily.Rounded -> RoundedCornerShape(cornerSizes, border)
        ShapeFamily.Cut -> CutCornerShape(cornerSizes, border)
    }
}
