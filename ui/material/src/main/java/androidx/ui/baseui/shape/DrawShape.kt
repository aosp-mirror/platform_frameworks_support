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

package androidx.ui.baseui.shape

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation

// TODO(Andrey: Documentation)
@Composable
fun DrawShape(shape: Shape, color: Color, @Children children: @Composable() () -> Unit) {
    val cache = +memo { DrawShapeCache() }
    Draw(children = children) { canvas, parentSize ->
        with(shape) {
            // draw background
            val outerPath = createOutline(parentSize).toPath()
            if (color.alpha > 0) {
                cache.paint.color = color
                canvas.drawPath(outerPath, cache.paint)
            }

            // draw clipped children
            canvas.save()
            canvas.clipPath(outerPath)
            drawChildren()
            canvas.restore()

            // draw border
            val border = border
            if (border != null) {
                // to have an inner path we provide a smaller parent size and shift the result
                val borderSize = if (border.size == Dp.Hairline) 1.px else border.size.toPx()
                val doubleBorderSize = borderSize * 2
                val sizeMinusBorder = parentSize.copy(
                    width = parentSize.width - doubleBorderSize,
                    height = parentSize.height - doubleBorderSize
                )
                val innerPath = createOutline(sizeMinusBorder).toPath()
                innerPath.shift(Offset(borderSize.value, borderSize.value))
                // now we calculate the diff between the inner and outer paths
                cache.diffPath.reset()
                cache.diffPath.op(outerPath, innerPath, PathOperation.difference)
                cache.paint.color = border.color
                canvas.drawPath(cache.diffPath, cache.paint)
            }
        }
    }
}

private class DrawShapeCache {
    val diffPath = Path()
    val paint = Paint().apply {
        style = PaintingStyle.fill
        isAntiAlias = true
    }
}
