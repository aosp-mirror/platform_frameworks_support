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

package androidx.ui.material.studies.rally.components

import androidx.compose.composer
import androidx.compose.Children
import androidx.compose.Composable
import androidx.ui.core.Draw
import androidx.ui.core.dp
import androidx.ui.core.min
import androidx.ui.engine.geometry.Rect
import androidx.ui.layout.Column
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.vectormath64.radians
import kotlin.math.max

data class RallyCircleGraphItem(
    val color: Color,
    val value: Float
)

@Composable
fun RallyCircleGraph(
    graphItems: List<RallyCircleGraphItem>,
    @Children children: @Composable() () -> Unit
) {
    ConstrainedBox(constraints = DpConstraints(minHeight = 320.dp)) {
        Draw { canvas, parentSize ->
            val centerX = parentSize.width.value / 2.0f
            val centerY = parentSize.height.value / 2.0f
            val radius = min(parentSize.width, parentSize.height).value - 64.dp.toPx().value

            canvas.drawArc(
                rect = Rect(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius
                ),
                startAngle = radians(0.0f),
                sweepAngle = radians(360.0f),
                useCenter = false,
                paint = Paint().apply {
                    this@apply.color = Color(0xFF22222D.toInt())
                    style = PaintingStyle.stroke
                    strokeWidth = 4.dp.toPx().value
                }
            )

            var currentDegrees = -90.0f
            graphItems.forEach {
                val degrees = 360 * it.value
                canvas.drawArc(
                    rect = Rect(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius
                    ),
                    startAngle = radians(currentDegrees),
                    sweepAngle = radians(max(0.0f, degrees - 1)),
                    useCenter = false,
                    paint = Paint().apply {
                        this@apply.color = it.color
                        style = PaintingStyle.stroke
                        strokeWidth = 4.dp.toPx().value
                    }
                )
                currentDegrees += degrees
            }
        }

        Column(mainAxisAlignment = MainAxisAlignment.Center) {
            children()
        }
    }
}