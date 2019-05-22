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
import androidx.ui.core.vectorgraphics.Brush
import androidx.ui.core.vectorgraphics.SolidColor
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.drawOutline
import androidx.ui.engine.geometry.toPath
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation

// TODO(Andrey: Documentation)
@Composable
fun DrawShape(
    shape: Shape,
    brush: Brush? = null,
    @Children children: @Composable() () -> Unit = NoChildren
) =
    with(shape) {
        val cache = +memo { DrawShapeCache() }
        Draw(children = children) { canvas, parentSize ->
            with(cache) {
                // draw background
                val outline = createOutline(parentSize)
                if (brush != null) {
                    brush.applyBrush(fillPaint)
                    canvas.drawOutline(outline, fillPaint)
                }
                val outerPath = outline.toPath()

                if (children !== NoChildren) {
                    // draw clipped children
                    canvas.save()
                    canvas.clipPath(outerPath)
                    drawChildren()
                    canvas.restore()
                } else {
                    // TODO: remove else block when drawChildren will not be required
                    drawChildren()
                }

                // draw border
                val border = border
                if (border != null) {
                    // to have an inner path we provide a smaller parent size and shift the result
                    val borderSize = if (border.width == Dp.Hairline) 1.px else border.width.toPx()
                    val doubleBorderSize = borderSize * 2
                    val sizeMinusBorder = parentSize.copy(
                        width = parentSize.width - doubleBorderSize,
                        height = parentSize.height - doubleBorderSize
                    )
                    val innerPath = createOutline(sizeMinusBorder).toPath()
                    innerPath.shift(Offset(borderSize.value, borderSize.value))
                    // now we calculate the diff between the inner and outer paths
                    diffPath.reset()
                    diffPath.op(outerPath, innerPath, PathOperation.difference)
                    border.brush.applyBrush(borderPaint)
                    canvas.drawPath(diffPath, borderPaint)
                }
            }
        }
    }

// TODO(Andrey: Documentation)
@Composable
fun DrawShape(
    shape: Shape,
    color: Color,
    @Children children: @Composable() () -> Unit = NoChildren
) {
    DrawShape(shape, SolidColor(color), children)
}

private val NoChildren: (@Composable() () -> Unit) = {}

private class DrawShapeCache {
    val diffPath = Path()
    val fillPaint = Paint().apply { isAntiAlias = true }
    val borderPaint = Paint().apply { isAntiAlias = true }
}
