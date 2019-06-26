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

package androidx.ui.core.vectorgraphics

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.memo
import androidx.compose.onDispose
import androidx.compose.unaryPlus

import androidx.ui.core.Draw
import androidx.ui.graphics.vectorgraphics.Brush
import androidx.ui.graphics.vectorgraphics.BrushType
import androidx.ui.graphics.vectorgraphics.DefaultAlpha
import androidx.ui.graphics.vectorgraphics.DefaultGroupName
import androidx.ui.graphics.vectorgraphics.DefaultPathName
import androidx.ui.graphics.vectorgraphics.DefaultPivotX
import androidx.ui.graphics.vectorgraphics.DefaultPivotY
import androidx.ui.graphics.vectorgraphics.DefaultRotate
import androidx.ui.graphics.vectorgraphics.DefaultScaleX
import androidx.ui.graphics.vectorgraphics.DefaultScaleY
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineCap
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineJoin
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineMiter
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineWidth
import androidx.ui.graphics.vectorgraphics.DefaultTranslateX
import androidx.ui.graphics.vectorgraphics.DefaultTranslateY
import androidx.ui.graphics.vectorgraphics.EmptyBrush
import androidx.ui.graphics.vectorgraphics.EmptyPath
import androidx.ui.graphics.vectorgraphics.Group
import androidx.ui.graphics.vectorgraphics.Path
import androidx.ui.graphics.vectorgraphics.PathData
import androidx.ui.graphics.vectorgraphics.Vector
import androidx.ui.graphics.vectorgraphics.createPath
import androidx.ui.graphics.vectorgraphics.obtainBrush
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.vector.VectorScope
import androidx.ui.vector.composeVector
import androidx.ui.vector.disposeVector


@Composable
fun DrawVector(
    name: String = "",
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = viewportWidth,
    defaultHeight: Float = viewportHeight,
    @Children children: @Composable() VectorScope.() -> Unit
) {
    val vector =
        Vector(
            name,
            viewportWidth,
            viewportHeight,
            defaultWidth,
            defaultHeight
        )

    val ref = +compositionReference()
    composeVector(vector, ref, children)
    +onDispose { disposeVector(vector) }

    Draw { canvas, _ ->
        vector.draw(canvas)
    }
}

@Composable
fun VectorScope.group(
    name: String = DefaultGroupName,
    rotate: Float = DefaultRotate,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translateX: Float = DefaultTranslateX,
    translateY: Float = DefaultTranslateY,
    clipPathData: PathData = EmptyPath,
    @Children childNodes: @Composable() VectorScope.() -> Unit
) {

    val clipPathNodes = createPath(clipPathData)
    <Group
        name = name
        rotate = rotate
        pivotX = pivotX
        pivotY = pivotY
        scaleX = scaleX
        scaleY = scaleY
        translateX = translateX
        translateY = translateY
        clipPathNodes = clipPathNodes
    >
        childNodes()
    </Group>
//    Group(name = name,
//        rotate = rotate,
//        pivotX = pivotX,
//        pivotY = pivotY,
//        scaleX = scaleX,
//        scaleY = scaleY,
//        translateX = translateX,
//        translateY = translateY,
//        clipPathNodes = clipPathNodes
//        ) {
//        childNodes()
//    }
}

@Composable
fun VectorScope.path(
    pathData: PathData,
    name: String = DefaultPathName,
    fill: BrushType = EmptyBrush,
    fillAlpha: Float = DefaultAlpha,
    stroke: BrushType = EmptyBrush,
    strokeAlpha: Float = DefaultAlpha,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter
) {
    val pathNodes = createPath(pathData)
    val fillBrush: Brush =
        obtainBrush(fill)
    val strokeBrush: Brush =
        obtainBrush(stroke)

    Path(name = name,
        pathNodes = pathNodes,
        fill = fillBrush,
        fillAlpha = fillAlpha,
        stroke = strokeBrush,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = strokeLineWidth,
        strokeLineJoin = strokeLineJoin,
        strokeLineCap = strokeLineCap,
        strokeLineMiter = strokeLineMiter)
}