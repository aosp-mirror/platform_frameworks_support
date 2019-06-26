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

package androidx.ui.graphics.vectorgraphics

import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.TileMode
import androidx.ui.vectormath64.Matrix4

val EmptyBrush = object : Brush {
    override fun applyBrush(p: Paint) {
        // NO-OP
    }
}

interface Brush {
    fun applyBrush(p: Paint)
}

data class SolidColor(private val value: Color) : Brush {
    override fun applyBrush(p: Paint) {
        p.color = value
    }
}

typealias ColorStop = Pair<Color, Float>

fun obtainBrush(brush: Any?): Brush {
    return when (brush) {
        is Int -> SolidColor(Color(brush))
        is Color -> SolidColor(brush)
        is Brush -> brush
        null -> EmptyBrush
        else -> throw IllegalArgumentException(brush.javaClass.simpleName +
                "Brush must be either a Color long, LinearGradient or RadialGradient")
    }
}

fun LinearGradient(
    vararg colorStops: ColorStop,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    tileMode: TileMode = TileMode.clamp
): LinearGradient {
    val colors = List(colorStops.size) { i -> colorStops[i].first }
    val stops = List(colorStops.size) { i -> colorStops[i].second }
    return LinearGradient(colors, stops, startX, startY, endX, endY, tileMode)
}

// TODO (njawad) replace with inline color class
data class LinearGradient internal constructor(
    val colors: List<Color>,
    val stops: List<Float>,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val tileMode: TileMode = TileMode.clamp
) : Brush {

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.linear(
            Offset(startX, startY),
            Offset(endX, endY),
            colors,
            stops,
            tileMode)
    }
}

fun RadialGradient(
    vararg colorStops: ColorStop,
    centerX: Float,
    centerY: Float,
    radius: Float,
    tileMode: TileMode = TileMode.clamp
): RadialGradient {
    val colors = List(colorStops.size) { i -> colorStops[i].first }
    val stops = List(colorStops.size) { i -> colorStops[i].second }
    return RadialGradient(colors, stops, centerX, centerY, radius, tileMode)
}

data class RadialGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.clamp
) : Brush {

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.radial(
            Offset(centerX, centerY),
            radius, colors, stops, tileMode, Matrix4(),
            null, 0.0f)
    }
}