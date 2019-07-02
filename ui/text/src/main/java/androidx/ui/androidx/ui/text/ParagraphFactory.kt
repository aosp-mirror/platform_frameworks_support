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

package androidx.ui.androidx.ui.text

import androidx.ui.core.Density
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.ui.painting.TextStyle

/**
 * Provides platform dependent text layout capabilities.
 */
interface ParagraphFactory {
    fun createParagraph(
        text: String,
        style: TextStyle,
        paragraphStyle: ParagraphStyle,
        textStyles: List<AnnotatedString.Item<TextStyle>>,
        density: Density
    ): ParagraphInterface
}

/**
 * Provides platform dependent Paragraph implementation.
 */
interface ParagraphInterface {
    val width: Float
        get() = 0f

    val height: Float
        get() = 0f

    val minIntrinsicWidth: Float
        get() = 0f

    val maxIntrinsicWidth: Float
        get() = 0f

    val baseline: Float
        get() = 0f

    val didExceedMaxLines: Boolean
        get() = false

    val lineCount: Int
        get() = 0

    fun layout(width: Float)

    fun getPositionForOffset(offset: Offset): Int

    fun getBoundingBoxForTextPosition(textPosition: Int): Rect

    fun getPathForRange(start: Int, end: Int): Path

    fun getCursorRect(offset: Int): Rect

    fun getWordBoundary(offset: Int): Pair<Int, Int>

    fun getLineLeft(lineIndex: Int): Float

    fun getLineRight(lineIndex: Int): Float

    fun getLineHeight(lineIndex: Int): Float

    fun getLineWidth(lineIndex: Int): Float

    fun isEllipsisApplied(lineIndex: Int): Boolean

    fun paint(canvas: Canvas, x: Float, y: Float)
}