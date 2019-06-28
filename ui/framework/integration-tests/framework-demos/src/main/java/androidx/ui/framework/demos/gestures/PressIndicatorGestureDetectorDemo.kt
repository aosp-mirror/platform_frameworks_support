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

package androidx.ui.framework.demos.gestures

import android.app.Activity
import android.os.Bundle
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.px
import androidx.ui.graphics.Color
import androidx.compose.composer
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.core.gesture.PressIndicatorGestureDetector

/**
 * Simple demo that shows off DragGestureDetector.
 */
class PressIndicatorGestureDetectorDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                PressableContainer(
                    paddingLeft = 48.dp,
                    paddingRight = 48.dp,
                    paddingTop = 96.dp,
                    paddingBottom = 96.dp
                ) {
                    PressableContainer(
                        paddingLeft = 48.dp,
                        paddingRight = 48.dp,
                        paddingTop = 96.dp,
                        paddingBottom = 96.dp
                    ) {
                        PressableContainer {}
                    }
                }
            }
        }
    }
}

@Composable
fun PressableContainer(
    paddingLeft: Dp? = null,
    paddingTop: Dp? = null,
    paddingRight: Dp? = null,
    paddingBottom: Dp? = null,
    @Children children: (@Composable() () -> Unit)
) {
    val notPressedColor = Color(0xFF2196f3.toInt())
    val pressedColor = Color(0xFFf44336.toInt())

    val color = +state { notPressedColor }

    val onStart: (Any) -> Unit = {
        color.value = pressedColor
    }

    val onStop: () -> Unit = {
        color.value = notPressedColor
    }

    PressIndicatorGestureDetector(
        onStart = onStart,
        onStop = onStop,
        onCancel = onStop
    ) {
        Border(Color(0xFF000000.toInt()), 2.dp) {
            DrawBox(
                0.px,
                0.px,
                -1.px,
                -1.px,
                color.value
            )
            Padding(paddingLeft, paddingTop, paddingRight, paddingBottom) {
                children()
            }
        }
    }
}