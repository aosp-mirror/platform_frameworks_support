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

package androidx.ui.material.demos

import android.util.Log
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.Border
import androidx.ui.baseui.shape.corner.CircleShape
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.OutlinedArea
import androidx.ui.core.RepaintBoundary
import androidx.ui.core.Text
import androidx.ui.core.WithDensity
import androidx.ui.core.dp
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.layout.Wrap
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TransparentButton
import androidx.ui.material.themeColor
import androidx.ui.material.themeShape
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.BlendMode
import androidx.ui.painting.Paint

@Composable
fun ButtonDemo2() {
    CraneWrapper {
//        Recompose {_ ->
//            PressReleasedGestureDetector(onRelease = { recompose() }) {
                Container(expanded = true) {
                    Wrap {
                        Container(width = 100.dp, height = 100.dp) {
                            WithDensity {
                                with(CircleShape()) {
                                    OutlinedArea(outlineProvider = { size -> createOutline(size) },
                                        elevation = 8.dp) {
                                        Recompose {
                                            PressReleasedGestureDetector(onRelease = { it() }) {
                                                Container(expanded = true) {
                                                    Draw { canvas, parentSize ->
                                                        canvas.drawRect(parentSize.toRect(), Paint().apply {
                                                            color = Color.Red
                                                        })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
//            }
//        }
    }
}
