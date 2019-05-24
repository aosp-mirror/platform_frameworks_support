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

package androidx.ui.animation.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.AnimatedFloat
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Layout
import androidx.ui.core.gesture.PressGestureDetector

class FancyScrolling : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MovingRect()
            }
        }
    }

    @Composable
    fun MovingRect() {
        val animValue = +state { AnimatedFloat(0f) }
        PressGestureDetector(
            onPress = { animValue.value.toValue(4f) },
            onRelease = { animValue.value.toValue(0f) },
            onCancel = { /* animValue.value.toValue(0f) */}) {
            val children = @Composable {
                Recompose {recompose ->
                    DrawScaledRect(scale = animValue.value.value, color = androidx.ui.graphics.Color.Companion.Blue)
                    animValue.value.onUpdate = recompose
                }
            }
            Layout(children = children, layoutBlock = { _, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {}
            })
        }
    }
}
