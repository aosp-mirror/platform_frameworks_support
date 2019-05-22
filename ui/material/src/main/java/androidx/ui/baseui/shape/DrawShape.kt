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
import androidx.ui.core.Draw
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path

// TODO(Andrey: Documentation)
@Composable
fun DrawShape(shape: Shape, color: Color, @Children children: @Composable() () -> Unit) =
    with(shape) {
        val paint = +memo {
            Paint().apply {
                style = PaintingStyle.fill
                isAntiAlias = true
            }
        }
        paint.color = color
        val path = +memo { Path() }
        Draw(children = children) { canvas, parentSize ->
            path.reset()
            applyToPath(path, parentSize)
            if (color.alpha > 0) {
                canvas.drawPath(path, paint)
            }
            canvas.save()
            canvas.clipPath(path)
            drawChildren()
            canvas.restore()
        }
    }
