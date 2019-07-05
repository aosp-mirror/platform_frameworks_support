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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.Wrap
import androidx.ui.material.Button
import androidx.ui.painting.Paint

/**
 * Draws a rectangle of a specified dimension, or to its max incoming constraints if dimensions are
 * not specified.
 */
@Composable
fun SizedRectangle(color: Color, width: Dp? = null, height: Dp? = null) {
    Layout(
        children = { DrawRectangle(color = color) },
        layoutBlock = { _, constraints ->
            val widthPx = width?.toIntPx() ?: constraints.maxWidth
            val heightPx = height?.toIntPx() ?: constraints.maxHeight
            layout(widthPx, heightPx) {}
        })
}

@Composable
fun DrawRectangle(color: Color) {
    val paint = Paint()
    paint.color = color
    Draw { canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    }
}

@Sampled
@Composable
fun SimpleStack() {
    Stack {
        aligned(Alignment.Center) {
            SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 300.dp, height = 300.dp)
        }
        aligned(Alignment.TopLeft) {
            SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 150.dp, height = 150.dp)
        }
        aligned(Alignment.BottomRight) {
            SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 150.dp, height = 150.dp)
        }
        positioned(null, 20.dp, null, 20.dp) {
            SizedRectangle(color = Color(0xFFFFA500.toInt()), width = 80.dp)
            SizedRectangle(color = Color(0xFFA52A2A.toInt()), width = 20.dp)
        }
    }
}

@Sampled
@Composable
fun SimpleFlexRow() {
    FlexRow {
         expanded(flex = 2f) {
             Center {
                 SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
             }
         }
         inflexible {
             SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp)
         }
         expanded(flex = 1f) {
             SizedRectangle(color = Color(0xFF00FF00.toInt()))
         }
     }
}

@Sampled
@Composable
fun SimpleFlexColumn() {
    FlexColumn {
        expanded(flex = 2f) {
            Center {
                SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
            }
        }
        inflexible {
            SizedRectangle(color = Color(0xFFFF0000.toInt()), height = 40.dp)
        }
        expanded(flex = 1f) {
            SizedRectangle(color = Color(0xFF00FF00.toInt()))
        }
    }
}

@Sampled
@Composable
fun SimpleRow() {
    Row {
        SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
        SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp, height = 80.dp)
        SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 80.dp, height = 70.dp)
    }
}

@Sampled
@Composable
fun SimpleColumn() {
    Column {
        SizedRectangle(color = Color(0xFF0000FF.toInt()), width = 40.dp, height = 40.dp)
        SizedRectangle(color = Color(0xFFFF0000.toInt()), width = 40.dp, height = 80.dp)
        SizedRectangle(color = Color(0xFF00FF00.toInt()), width = 80.dp, height = 70.dp)
    }
}

/**
 * Builds a layout containing three buttons having the same width as the widest one.
 *
 * Here [MaxIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose maximum intrinsic width will correspond to the width of the largest [Button]. Then
 * [MaxIntrinsicWidth] will measure the [Column] with tight width, the same as the premeasured
 * maximum intrinsic width, which due to [CrossAxisAlignment.Stretch] will force the [Button]s
 * to use the same width.
 */
@Sampled
@Composable
fun SameWidthButtons() {
    Wrap {
        MaxIntrinsicWidth {
            Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                Button("Text button")
                Button("Extremely long text button")
                Button("Longer text button")
            }
        }
    }
}

/*
 * Builds a layout containing two pieces of text separated by a divider, where the divider
 * is sized according to the height of the longest text.
 *
 * Here [MaxIntrinsicHeight] is adding a speculative height measurement pass for the [FlexRow],
 * whose maximum intrinsic height will correspond to the height of the largest [Text]. Then
 * [MaxIntrinsicHeight] will measure the [FlexRow] with tight height, the same as the premeasured
 * maximum intrinsic height, which due to [CrossAxisAlignment.Stretch] will force the [Text]s and
 * the divider to use the same height.
 */
@Sampled
@Composable
fun MatchParentDivider() {
    Wrap {
        MaxIntrinsicHeight {
            FlexRow(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                expanded(flex = 1f) {
                    Text("This is a really short text")
                }
                inflexible {
                    Container(width = 1.dp) { DrawRectangle(Color.Black) }
                }
                expanded(flex = 1f) {
                    Text("This is a much much much much much much much much much much much " +
                            "much much much much longer text")
                }
            }
        }
    }
}