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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.LayoutReceiver
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureReceiver
import androidx.ui.core.Placeable
import androidx.ui.core.PlaceReceiver
import androidx.ui.core.ipx
import androidx.ui.core.max

internal class SingleChildMeasurable(private val measurables: List<Measurable>) : Measurable() {
    override val parentData: Any? = null
    override fun MeasureReceiver.measure(constraints: Constraints): Placeable {
        val placeables = measurables.map { it.measure(constraints) }
        return SingleChildPlaceable(placeables)
    }
}

internal class SingleChildPlaceable(private val placeables: List<Placeable>) : Placeable() {
    override val width: IntPx get() = placeables.fold(0.ipx) { width, placeable ->
        max(width, placeable.width)
    }
    override val height: IntPx get() = placeables.fold(0.ipx) { height, placeable ->
        max(height, placeable.height)
    }
    override fun PlaceReceiver.place(x: IntPx, y: IntPx) {
        placeables.forEach { it.place(x, y) }
    }
}

@Composable
fun SingleChildLayout(
    children: @Composable() () -> Unit,
    @Children(composable = false) layoutBlock: LayoutReceiver.(Measurable, Constraints) -> Unit
) {
    Layout(children) { measurables, constraints ->
        val measurable = SingleChildMeasurable(measurables)
        layoutBlock(measurable, constraints)
    }
}