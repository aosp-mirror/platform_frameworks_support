/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import androidx.ui.painting.Canvas
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus

/**
 * Draw with children is used when the Canvas should be modified children.
 *
 *     Draw(children) { canvas, parentSize, drawChildren ->
 *         canvas.save()
 *         canvas.clip(parentSize.toRect())
 *         drawChildren(canvas)
 *         canvas.restore()
 *     }
 * If `drawChildren()` is not called, none of the children will be painted.
 */
@Composable
fun Draw(
    children: @Composable() () -> Unit,
    @Children(composable = false)
    onPaint: DensityReceiver.(canvas: Canvas,
                              parentSize: PxSize,
                              drawChildren: (Canvas) -> Unit) -> Unit
) {
    // Hide the internals of DrawNode
    <DrawNode onPaint={ densityReceiver, canvas, parentSize, drawChildren ->
        densityReceiver.onPaint(canvas, parentSize, drawChildren)
    }>
        children()
    </DrawNode>
}

/**
 * Use Draw to get a [Canvas] to paint into the parent.
 *
 *     val paint = +memo { Paint() }
 *     Draw { canvas, parentSize ->
 *         paint.value.color = Color.Blue
 *         canvas.drawRect(Rect(0.0f, 0.0f, parentSize.width, parentSize.height, paint.value)
 *     }
 */
@Composable
fun Draw(@Children(composable = false)
         onPaint: DensityReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit) {
    <DrawNode onPaint={ densityReceiver, canvas, parentSize, _ ->
        densityReceiver.onPaint(canvas, parentSize)
    }/>
}
