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

package androidx.ui.baseui.shape

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.ui.core.Draw
import androidx.ui.core.PxSize
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.toPath

// Temporary.
//
// TODO: Replace with an implementation using RenderNode's Outlines instead to have
// clipping with antialiasing.
@Composable
fun ClipShape(shape: Shape, @Children children: @Composable() () -> Unit) = with(shape) {
    Draw(children) { canvas, parentSize ->
        val outline = createOutline(parentSize)
        val outerPath = outline.toPath()
        // draw clipped children
        canvas.save()
        canvas.clipPath(outerPath)
        drawChildren()
        canvas.restore()
    }
}

fun Shape.toOutlineProvider() = effectOf<(PxSize) -> Outline> {
    +withDensity {
        +memo(this@toOutlineProvider) {
            { size: PxSize -> createOutline(size) }
        }
    }
}