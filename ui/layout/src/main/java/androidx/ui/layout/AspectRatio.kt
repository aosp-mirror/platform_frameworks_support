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

package androidx.ui.layout

import android.util.Log
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.constrain
import androidx.ui.core.ipx
import androidx.ui.core.isFinite
import androidx.ui.core.satisfiedBy

/**
 * Layout widget that takes a child composable and applies whitespace padding around it.
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 *     Row {
 *         Padding(padding=EdgeInsets(right=20.dp)) {
 *             SizedRectangle(color=Color(0xFFFF0000.toInt()), width=20.dp, height= 20.dp)
 *         }
 *     }
 */
@Composable
fun AspectRatio(
    aspectRatio: Float,
    @Children children: @Composable() () -> Unit
) {
    Layout(children) { measurables, constraints ->
        val size = listOf(
            IntPxSize(constraints.maxWidth, constraints.maxWidth / aspectRatio),
            IntPxSize(constraints.maxHeight * aspectRatio, constraints.maxHeight),
            IntPxSize(constraints.minWidth, constraints.minWidth / aspectRatio),
            IntPxSize(constraints.minHeight * aspectRatio, constraints.minHeight)
        ).find { it.width.isFinite() && it.height.isFinite() && constraints.satisfiedBy(it) }

        val measurable = measurables.firstOrNull()
        val childConstraints = if (size != null) {
            Constraints.tightConstraints(size.width, size.height)
        } else {
            constraints
        }
        val placeable = measurable?.measure(childConstraints)

        layout(size?.width ?: constraints.minWidth, size?.height ?: constraints.minHeight) {
            placeable?.place(0.ipx, 0.ipx)
        }
    }
}
