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
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.addOutline
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation

@Composable
fun DrawBorder(
    shape: Shape,
    border: Border
) = with(shape) {
    with(+memo { DrawBorderCachesHolder() }) {
        Draw { canvas, parentSize ->
            outerPath.reset()
            outerPath.addOutline(createOutline(parentSize))

            // to have an inner path we provide a smaller parent size and shift the result
            val borderSize = if (border.width == Dp.Hairline) 1.px else border.width.toPx()
            val sizeMinusBorder = parentSize.copy(
                width = parentSize.width - borderSize * 2,
                height = parentSize.height - borderSize * 2
            )
            innerPath.reset()
            innerPath.addOutline(createOutline(sizeMinusBorder))
            innerPath.shift(Offset(borderSize.value, borderSize.value))

            // now we calculate the diff between the inner and the outer paths
            diffPath.op(outerPath, innerPath, PathOperation.difference)

            border.brush.applyBrush(paint)
            canvas.drawPath(diffPath, paint)
        }
    }
}

private class DrawBorderCachesHolder {
    val outerPath = Path()
    val innerPath = Path()
    val diffPath = Path()
    val paint = Paint().apply { isAntiAlias = true }
}
