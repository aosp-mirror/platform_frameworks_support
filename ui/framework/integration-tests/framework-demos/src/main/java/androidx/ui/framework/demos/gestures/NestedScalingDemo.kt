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
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.graphics.Color
import androidx.compose.composer
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dp
import androidx.ui.core.gesture.ScaleGestureDetector
import androidx.ui.core.gesture.ScaleObserver

/**
 * Demo app created to study some complex interactions of multiple DragGestureDetectors.
 */
class NestedScalingDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                Scalable(325.dp, 400.dp, Color(0xFFffeb3b.toInt())) {
                    Scalable(200.dp, 275.dp, Color(0xFF4caf50.toInt())) {}
                }
            }
        }
    }
}

@Composable
fun Scalable(
    dimensionMin: Dp,
    dimensionMax: Dp,
    color: Color,
    @Children children: @Composable() () -> Unit
) {

    val outerDim = +state { dimensionMin }

    val outerScaleObserver = object : ScaleObserver {
        override fun onScale(percentageChanged: Float): Float {
            val oldSize = outerDim.value
            outerDim.value = oldSize * percentageChanged
            if (outerDim.value < dimensionMin) {
                outerDim.value = dimensionMin
            } else if (outerDim.value > dimensionMax) {
                outerDim.value = dimensionMax
            }
            return outerDim.value / oldSize
        }
    }

    Center {
        ScaleGestureDetector(outerScaleObserver) {
            SimpleContainer(
                width = outerDim.value,
                height = outerDim.value,
                padding = 0.dp
            ) {
                DrawBox(0.px, 0.px, -1.px, -1.px, color)
                children()
            }
        }
    }
}