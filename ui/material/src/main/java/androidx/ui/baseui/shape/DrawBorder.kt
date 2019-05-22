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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation

// TODO(Andrey: Documentation)
@Composable
fun DrawBorder(shape: Shape) = with(shape) {
    val border = border
    if (border != null) {
        val paint = +memo(border) {
            Paint().apply {
                isAntiAlias = true
                color = border.color
                style = PaintingStyle.fill
            }
        }
        val outerPath = +memo { Path() }
        val innerPath = +memo { Path() }
        val diffPath = +memo { Path() }
        Draw { canvas, parentSize ->
            outerPath.reset()
            applyToPath(outerPath, parentSize)
            val borderSize = if (border.size == Dp.Hairline) 1.px else border.size.toPx()
            val doubleBorderSize = borderSize * 2
            val sizeMinusBorder = parentSize.copy(
                width = parentSize.width - doubleBorderSize,
                height = parentSize.height - doubleBorderSize
            )
            innerPath.reset()
            applyToPath(innerPath, sizeMinusBorder)
            val innerShifted = innerPath.shift(Offset(borderSize.value, borderSize.value))
            diffPath.reset()
            diffPath.op(outerPath, innerShifted, PathOperation.difference)
            canvas.drawPath(diffPath, paint)
        }
    }
}
