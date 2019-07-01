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

package androidx.ui.text

import androidx.annotation.RestrictTo
import androidx.ui.core.Density
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.text.font.Font
import java.lang.IllegalStateException
import kotlin.math.max

/**
 * The text layout that uses multiple paragraphs to render text.
 *
 * It's main responsibility is to determine the paragraph ranges and do the conversion between
 * global index/offset and the local one. It acts similar to a [Paragraph] except that it supports
 * multiple [ParagraphStyle]s.
 */
class AggregatedTextLayout(
    val annotatedString: AnnotatedString,
    textStyle: TextStyle?,
    paragraphStyle: ParagraphStyle?,
    val maxLines: Int? = null,
    val ellipsis: Boolean? = null,
    val density: Density,
    val resourceLoader: Font.ResourceLoader
) {
    private var paragraphInfoList: List<ParagraphInfo>? = null

    val minIntrinsicWidth: Float
        get() = paragraphInfoList!!.foldRight(0f) { paragraphInfo, minWidth ->
            max(paragraphInfo.paragraph.minIntrinsicWidth, minWidth)
        }

    val maxIntrinsicWidth: Float
        get() = paragraphInfoList!!.foldRight(0f) { paragraphInfo, maxWidth ->
            max(paragraphInfo.paragraph.maxIntrinsicWidth, maxWidth)
        }

    var didExceedMaxLines: Boolean = false
        private set

    var width: Float = 0f
        private set

    var height: Float = 0f
        private set

    val textStyle: TextStyle = textStyle ?: TextStyle()

    val paragraphStyle: ParagraphStyle = paragraphStyle ?: ParagraphStyle()

    fun layout(width: Float) {
        this.width = width
        this.didExceedMaxLines = false

        val paragraphStyles = completeParagraphRanges(
            annotatedString.text.length,
            annotatedString.paragraphStyles,
            paragraphStyle
        )

        var currentLineNum = 0
        var currentTopOffset = 0f

        this.paragraphInfoList = paragraphStyles.mapIndexedNotNull { index, paragraphStyle ->
            if (this.didExceedMaxLines) {
                return@mapIndexedNotNull null
            }
            val start = paragraphStyle.start
            val end = paragraphStyle.end

            val textInParagraph = annotatedString.text.substring(start, end)
            val textStylesInParagraph = annotatedString.getLocalStyles(start, end)

            // TODO: support IncludePadding correctly
            val paragraph = Paragraph(
                textInParagraph,
                textStyle,
//                createParagraphStyle(
//                    this.paragraphStyle.merge(paragraphStyle.style),
//                    ellipsis = ellipsis,
//                    maxLines = maxLines?.minus(currentLineNum)
//                ),
                this.paragraphStyle.merge(paragraphStyle.style),
                textStylesInParagraph,
                maxLines,
                ellipsis,
                density,
                resourceLoader
            )
            paragraph.layout(ParagraphConstraints(width = width))

            val topOffset = currentTopOffset
            val bottomOffset = currentTopOffset + paragraph.height

            currentLineNum += paragraph.lineCount
            currentTopOffset = bottomOffset

            // TODO: solve the corner case where the ellipsis won't be applied when
            //  currentLineNum == maxLines and paragraph.didExceedMaxLines is false.
            if (paragraph.didExceedMaxLines || currentLineNum == maxLines) {
                this.didExceedMaxLines =
                    paragraph.didExceedMaxLines || index != paragraphStyles.lastIndex
            }

            ParagraphInfo(
                paragraph = paragraph,
                startIndex = start,
                endIndex = end,
                topOffset = topOffset,
                bottomOffset = bottomOffset
            )
        }
        this.height = currentTopOffset
    }

    fun paint(canvas: Canvas, offset: Offset) {
        paragraphInfoList!!.forEach {
            val (x, y) = offset + it.originOffset
            it.paragraph.paint(canvas, x, y)
        }
    }

    /**
     * Draws text background of the given range.
     *
     * If the given range is empty, do nothing.
     *
     * @param start inclusive start offset of the drawing range.
     * @param end exclusive end offset of the drawing range.
     * @param color a color to be used for drawing background.
     * @param canvas the target canvas.
     * @param offset the drawing offset.
     */
    fun paintBackground(start: Int, end: Int, color: Color, canvas: Canvas, offset: Offset) {
        paragraphInfoList?.let {
            var paragraphIndex = findParagraphByIndex(it, start)
            val path = Path()
            while (paragraphIndex < it.size && it[paragraphIndex].startIndex < end) {
                val paragraphInfo = it[paragraphIndex]
                with(paragraphInfo) {
                    path.addPath(
                        path = paragraph.getPathForRange(
                            start = start.toLocal(),
                            end = end.toLocal()
                        ),
                        offset = originOffset
                    )
                }
                ++paragraphIndex
            }
            val paint = Paint()
            paint.color = color
            path.shift(offset)
            canvas.drawPath(path, paint)
        }
    }

    /** Returns the position within the text for the given pixel offset. */
    fun getPositionForOffset(offset: Offset): Int {
        return paragraphInfoList?.let {
            val paragraphIndex = findParagraphByDy(it, offset.dy.coerceIn(0f, height))
            val paragraphInfo = it[paragraphIndex]
            with(paragraphInfo) {
                paragraph.getPositionForOffset(offset.toLocal()).toGlobal()
            }
        } ?: throw IllegalStateException("")
    }

    /**
     * Returns the bounding box as Rect of the character for given TextPosition. Rect includes the
     * top, bottom, left and right of a character.
     *
     * Valid only after [layout] has been called.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getBoundingBox(index: Int): Rect {
        return paragraphInfoList?.let {
            val paragraphIndex = findParagraphByIndex(it, index)
            with(it[paragraphIndex]) {
                paragraph.getBoundingBox(index.toLocal()).toGlobal()
            }
        } ?: throw IllegalStateException("")
    }

    /**
     * Returns the text range of the word at the given offset. Characters not part of a word, such
     * as spaces, symbols, and punctuation, have word breaks on both sides. In such cases, this
     * method will return a text range that contains the given text position.
     *
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * <http://www.unicode.org/reports/tr29/#Word_Boundaries>.
     */
    fun getWordBoundary(index: Int): TextRange {
        return paragraphInfoList?.let {
            val paragraphIndex = findParagraphByIndex(it, index)

            with(it[paragraphIndex]) {
                paragraph.getWordBoundary(index.toLocal()).toGlobal()
            }
        } ?: throw IllegalStateException("")
    }

    /** Returns rectangle of the cursor area. */
    fun getCursorRect(offset: Int): Rect {
        return paragraphInfoList?.let {
            val paragraphIndex = findParagraphByIndex(it, offset)

            with(it[paragraphIndex]) {
                paragraph.getCursorRect(
                    offset.toLocal()
                ).toGlobal()
            }
        } ?: throw IllegalStateException("")
    }
}

/**
 * Given an index in [AggregatedTextLayout], find the corresponding [ParagraphInfo] which
 * covers the provided index.
 *
 * @param paragraphInfoList The list of [ParagraphInfo] containing the information of each paragraph
 *  in the [AggregatedTextLayout].
 * @param index The target index in the [AggregatedTextLayout].
 * @return The index of the [ParagraphInfo] in [paragraphInfoList].
 */
private fun findParagraphByIndex(paragraphInfoList: List<ParagraphInfo>, index: Int): Int {
    return paragraphInfoList.binarySearch { paragraphInfo ->
        when {
            paragraphInfo.startIndex > index -> 1
            paragraphInfo.endIndex <= index -> -1
            else -> 0
        }
    }
}

/**
 * Given the y offset relative to this [AggregatedTextLayout], find the index of the corresponding
 * [ParagraphInfo] which occupies the provided position.
 *
 * @param paragraphInfoList The list of [ParagraphInfo] containing the information of each paragraph
 *  in the [AggregatedTextLayout].
 * @param dy The y coordinate position relative to the [AggregatedTextLayout].
 * @return The index of the [ParagraphInfo] in [paragraphInfoList].
 */
private fun findParagraphByDy(paragraphInfoList: List<ParagraphInfo>, dy: Float): Int {
    return paragraphInfoList.binarySearch { paragraphInfo ->
        when {
            paragraphInfo.topOffset > dy -> 1
            paragraphInfo.bottomOffset <= dy -> -1
            else -> 0
        }
    }
}

/**
 * A helper function used to determine the paragraph boundaries in [AggregatedTextLayout].
 * It takes a list of [ParagraphStyle]s which specifies some paragraphs, where the uncovered
 * parts should be default paragraphs. And this function will complete the [paragraphStyles] by
 * adding those default paragraphs into the [ParagraphStyle] list.
 *
 * @param textLength The length of the text in [AggregatedTextLayout].
 * @param paragraphStyles The input [ParagraphStyle] list to be completed.
 * @param defaultParagraphStyle The default [ParagraphStyle]. It's used for both unspecified default
 *  paragraphs and specified paragraph. When a specified paragraph's [ParagraphStyle] has a null
 *  attribute, the default one will be used instead.
 */
private fun completeParagraphRanges(
    textLength: Int,
    paragraphStyles: List<AnnotatedString.Item<ParagraphStyle>>,
    defaultParagraphStyle: ParagraphStyle
): List<AnnotatedString.Item<ParagraphStyle>> {
    var lastPos = 0
    val ret = mutableListOf<AnnotatedString.Item<ParagraphStyle>>()
    for (paragraphStyle in paragraphStyles) {
        if (paragraphStyle.start != lastPos) {
            ret.add(AnnotatedString.Item(defaultParagraphStyle, lastPos, paragraphStyle.start))
        } else {
            ret.add(paragraphStyle.copy(style = defaultParagraphStyle.merge(paragraphStyle.style)))
        }
        lastPos = paragraphStyle.end
    }
    if (lastPos != textLength) {
        ret.add(AnnotatedString.Item(defaultParagraphStyle, lastPos, textLength))
    }
    return ret
}

/**
 * Helper function used to find the [TextStyle]s in the given paragraph range and also convert the
 * range of those [TextStyle]s to paragraph local range.
 *
 * @param start The start index of the paragraph range, inclusive.
 * @param end The end index of the paragraph range, exclusive.
 * @return The list of converted [TextStyle]s in the given paragraph range.
 */
private fun AnnotatedString.getLocalStyles(
    start: Int,
    end: Int
): List<AnnotatedString.Item<TextStyle>> {
    // If the given range covers the whole AnnotatedString, return textStyles without conversion.
    if (start == 0 && end >= this.text.length) {
        return textStyles
    }
    val ret = mutableListOf<AnnotatedString.Item<TextStyle>>()
    for (item in this.textStyles) {
        if (item.start < end && item.end > start) {
            ret.add(
                AnnotatedString.Item(
                    item.style,
                    item.start.coerceIn(start, end) - start,
                    item.end.coerceIn(start, end) - start
                )
            )
        }
    }
    return ret
}

/**
 * This is a helper data structure to store the information of a single [Paragraph] in an
 * [AggregatedTextLayout]. It's mainly used to convert a global index, lineNumber and [Offset] to the
 * local ones inside the [paragraph], and vice versa.
 *
 * @param paragraph The [Paragraph] object corresponding to this [ParagraphInfo].
 * @param startIndex The start index of this paragraph in the parent [AggregatedTextLayout], inclusive.
 * @param endIndex The end index of this paragraph in the parent [AggregatedTextLayout], exclusive.
 * @param topOffset The top of the [paragraph] relative to the parent [AggregatedTextLayout].
 * @param bottomOffset The bottom of the [paragraph] relative to the parent [AggregatedTextLayout].
 */
internal data class ParagraphInfo(
    val paragraph: Paragraph,
    val startIndex: Int,
    val endIndex: Int,
    val topOffset: Float,
    val bottomOffset: Float
) {
    /**
     * The [Offset] of the origin point of the [paragraph] relative to the parent [AggregatedTextLayout].
     */
    val originOffset = Offset(dx = 0f, dy = topOffset)

    /**
     * Convert an index in the parent [AggregatedTextLayout] to the local index in the [paragraph].
     *
     * If it the given index is not in the range of this [paragraph], it would be coerce into the
     * [paragraph] range first.
     */
    fun Int.toLocal(): Int {
        return coerceIn(startIndex, endIndex) - startIndex
    }

    /**
     * Convert a local index in the [paragraph] to the global index in the parent [AggregatedTextLayout].
     *
     * It assumes the index is not in
     */
    fun Int.toGlobal(): Int {
        return this + startIndex
    }

    /**
     * Convert a [Offset] relative to the parent [AggregatedTextLayout] to the local [Offset]
     * relative to the [paragraph].
     */
    fun Offset.toLocal(): Offset {
        return copy(dy = dy - topOffset)
    }

    /**
     * Convert a [Rect] relative to the [paragraph] to the [Rect] relative to the parent [AggregatedTextLayout].
     */
    fun Rect.toGlobal(): Rect {
        return shift(originOffset)
    }

    /**
     * Convert a [TextRange] in to the [paragraph] to the [TextRange] in the parent [AggregatedTextLayout].
     */
    fun TextRange.toGlobal(): TextRange {
        return TextRange(start = start.toGlobal(), end = end.toGlobal())
    }
}