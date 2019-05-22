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
import androidx.ui.core.Draw
import androidx.ui.core.vectorgraphics.Brush
import androidx.ui.core.vectorgraphics.SolidColor
import androidx.ui.engine.geometry.drawOutline
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint

// TODO(Andrey: Documentation)
@Composable
fun DrawShape(
    shape: Shape,
    brush: Brush
) = with(shape) {
    val paint = +memo { Paint() }
    Draw { canvas, parentSize ->
        // draw background
        val outline = createOutline(parentSize)
        brush.applyBrush(paint)
        canvas.drawOutline(outline, paint)
    }
}

@Composable
fun DrawShape(
    shape: Shape,
    color: Color
) {
    DrawShape(shape = shape, brush = +memo(color) { SolidColor(color) })
}