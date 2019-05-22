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

package androidx.ui.baseui.shape.border

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.Draw
import androidx.ui.core.px
import androidx.ui.engine.geometry.toPath
import androidx.ui.painting.Paint
import androidx.ui.painting.Path

@Composable
fun DrawBorder(
    shape: Shape,
    border: Border?
) = with(shape) {
    val cache = +memo { DrawBorderCache() }
    if (border != null) {
        Draw { canvas, parentSize ->
            val outline = createOutline(parentSize)
            val outerPath = outline.toPath()

            // to have an inner path we provide a smaller parent size and shift the result
            val borderSize =
                if (border.width == androidx.ui.core.Dp.Hairline) 1.px else border.width.toPx()
            val doubleBorderSize = borderSize * 2
            val sizeMinusBorder = parentSize.copy(
                width = parentSize.width - doubleBorderSize,
                height = parentSize.height - doubleBorderSize
            )
            val innerPath = createOutline(sizeMinusBorder).toPath()
            innerPath.shift(
                androidx.ui.engine.geometry.Offset(
                    borderSize.value,
                    borderSize.value
                )
            )
            // now we calculate the diff between the inner and outer paths
            cache.diffPath.reset()
            cache.diffPath.op(outerPath, innerPath, androidx.ui.painting.PathOperation.difference)
            border.brush.applyBrush(cache.borderPaint)
            canvas.drawPath(cache.diffPath, cache.borderPaint)
        }
    }
}

private class DrawBorderCache {
    val diffPath = Path()
    val borderPaint = Paint().apply { isAntiAlias = true }
}