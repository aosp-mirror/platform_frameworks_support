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

import androidx.compose.Children
import androidx.compose.composer
import androidx.compose.Composable
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.times

@Composable
fun Clip(
    clipWidth: Float? = null,
    clipHeight: Float? = null,
    @Children children: @Composable() () -> Unit
) {
    Layout(layoutBlock = { measurables, incomingConstraints ->
        val measurable = measurables.firstOrNull()

        val initialMeasure = measurable?.measure(incomingConstraints)
        val initialWidth = initialMeasure?.width ?: incomingConstraints.minWidth
        val initialHeight = initialMeasure?.height ?: incomingConstraints.minHeight

        val placeable = measurable?.measure(incomingConstraints.copy(
            minWidth = clipWidth?.times(initialWidth) ?: incomingConstraints.minWidth,
            maxWidth = clipWidth?.times(initialWidth) ?: incomingConstraints.maxWidth,
            minHeight = clipHeight?.times(initialHeight) ?: incomingConstraints.minHeight,
            maxHeight = clipHeight?.times(initialHeight) ?: incomingConstraints.maxHeight

        ))

        val layoutWidth = placeable?.width ?: incomingConstraints.minWidth
        val layoutHeight = placeable?.height ?: incomingConstraints.minHeight
        layout(layoutWidth, layoutHeight) {
            placeable?.place(IntPx.Zero, IntPx.Zero)
        }
    }, children = children)
}