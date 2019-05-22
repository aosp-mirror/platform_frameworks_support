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

package androidx.ui.baseui.shape.corner

import androidx.ui.core.DensityReceiver
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.px

/**
 * Contains sizes of all four cornerSizes for a shape.
 *
 * @param topLeft a size for the top left corner
 * @param topRight a size for the top right corner
 * @param bottomRight a size for the bottom left corner
 * @param bottomLeft a size for the bottom right corner
 */
data class CornerSizes(
    val topLeft: CornerSize,
    val topRight: CornerSize,
    val bottomRight: CornerSize,
    val bottomLeft: CornerSize
)

fun CornerSizes(allCorners: CornerSize) = CornerSizes(
    allCorners,
    allCorners,
    allCorners,
    allCorners
)

data class PxCornerSizes(
    val topLeft: Px,
    val topRight: Px,
    val bottomRight: Px,
    val bottomLeft: Px
) {
    init {
        if (topLeft < 0.px || topRight < 0.px || bottomRight < 0.px || bottomLeft < 0.px) {
            throw IllegalArgumentException("CornerSize size in Px can't be negative!")
        }
    }
}

/*inline*/ fun PxCornerSizes.isEmpty() =
    topLeft + topRight + bottomLeft + bottomRight == 0.px

/*inline*/ fun DensityReceiver.PxCornerSizes(
    corners: CornerSizes,
    shapeSize: PxSize
): PxCornerSizes = with(corners) {
    PxCornerSizes(
        topLeft = topLeft(shapeSize),
        topRight = topRight(shapeSize),
        bottomRight = bottomRight(shapeSize),
        bottomLeft = bottomLeft(shapeSize)
    )
}
