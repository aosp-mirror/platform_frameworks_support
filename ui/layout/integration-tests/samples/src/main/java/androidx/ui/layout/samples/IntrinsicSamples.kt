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
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.Wrap

/**
 * Builds a layout containing three text widgets having the same width as the widest one.
 *
 * Here [MaxIntrinsicWidth] is adding a speculative width measurement pass for the [Column],
 * whose maximum intrinsic width will correspond to the width of the largest [Text]. Then
 * [MaxIntrinsicWidth] will measure the [Column] with tight width, the same as the premeasured
 * maximum intrinsic width, which due to [CrossAxisAlignment.Stretch] will force the [Text] [Wrap]s
 * to use the same width.
 */
@Sampled
@Composable
fun SameWidthButtons() {
    @Composable
    fun TextWithBackground(text: String) {
        Wrap(Alignment.Center) {
            DrawRectangle(Color.Purple)
            Text(text)
        }
    }
    Wrap {
        MaxIntrinsicWidth {
            Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                TextWithBackground("Text button")
                TextWithBackground("Extremely long text button")
                TextWithBackground("Longer text button")
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
