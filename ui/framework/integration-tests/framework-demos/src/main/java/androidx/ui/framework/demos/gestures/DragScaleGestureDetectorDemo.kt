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
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.ScaleGestureDetector
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.core.px
import androidx.ui.graphics.Color
import androidx.ui.core.Px
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.compose.composer
import androidx.ui.core.gesture.DragGestureDetector

/**
 * Simple demo that shows off how DragGestureDetector and ScaleGestureDetector automatically
 * interoperate.
 */
class DragAndScaleGestureDetectorDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                val width = +state { 200.px }
                val height = +state { 200.px }
                val xOffset = +state { 0.px }
                val yOffset = +state { 0.px }
                val dragInScale = +state { false }

                val scaleObserver = object : ScaleObserver {
                    override fun onScale(percentageChanged: Float): Float {
                        width.value *= percentageChanged
                        height.value *= percentageChanged
                        return percentageChanged
                    }
                }

                val dragObserver = object : DragObserver {
                    override fun onDrag(dragDistance: PxPosition): PxPosition {
                        xOffset.value += dragDistance.x
                        yOffset.value += dragDistance.y
                        return dragDistance
                    }
                }

                val onRelease = {
                    dragInScale.value = !dragInScale.value
                }

                if (dragInScale.value) {
                    ScaleGestureDetector(scaleObserver) {
                        DragGestureDetector(dragObserver = dragObserver) {
                            BoxLayout(
                                onRelease,
                                width.value,
                                height.value,
                                xOffset.value,
                                yOffset.value,
                                Color(0xFFf44336.toInt())
                            )
                        }
                    }
                } else {
                    DragGestureDetector(dragObserver = dragObserver) {
                        ScaleGestureDetector(scaleObserver) {
                            BoxLayout(
                                onRelease,
                                width.value,
                                height.value,
                                xOffset.value,
                                yOffset.value,
                                Color(0xFF2196f3.toInt())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxLayout(
    onRelease: () -> Unit,
    width: Px,
    height: Px,
    xOffset: Px,
    yOffset: Px,
    color: Color
) {
    PressReleasedGestureDetector(onRelease) {
        MatchParent {
            DrawBox(
                xOffset,
                yOffset,
                width,
                height,
                color
            )
        }
    }
}