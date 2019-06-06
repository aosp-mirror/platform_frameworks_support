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

import android.content.Context
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextPosition
import androidx.ui.graphics.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.services.text_editing.TextSelection
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.effectOf
import androidx.compose.onCommit
import androidx.compose.state
import androidx.compose.memo
import androidx.compose.onDispose
import androidx.compose.unaryPlus
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextAffinity
import kotlin.math.max
import kotlin.math.min

private val DefaultTextAlign: TextAlign = TextAlign.Start
private val DefaultTextDirection: TextDirection = TextDirection.Ltr
private val DefaultSoftWrap: Boolean = true
private val DefaultOverflow: TextOverflow = TextOverflow.Clip
private val DefaultMaxLines: Int? = null

/** The default selection color if none is specified. */
private val DefaultSelectionColor = Color(0x6633B5E5)

@Composable
fun Text(
    /** How the text should be aligned horizontally. */
    textAlign: TextAlign = DefaultTextAlign,
    /** The directionality of the text. */
    textDirection: TextDirection = DefaultTextDirection,
    /**
     *  Whether the text should break at soft line breaks.
     *  If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     *  space.
     *  If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
     */
    softWrap: Boolean = DefaultSoftWrap,
    /** How visual overflow should be handled. */
    overflow: TextOverflow = DefaultOverflow,
    /** The number of font pixels for each logical pixel. */
    textScaleFactor: Float = 1.0f,
    /**
     *  An optional maximum number of lines for the text to span, wrapping if necessary.
     *  If the text exceeds the given number of lines, it will be truncated according to [overflow]
     *  and [softWrap].
     *  The value may be null. If it is not null, then it must be greater than zero.
     */
    maxLines: Int? = DefaultMaxLines,
    /**
     *  The color used to draw selected region.
     */
    selectionColor: Color = DefaultSelectionColor,
    /**
     * Composable TextSpan attached after [text].
     */
    @Children child: @Composable TextSpanScope.() -> Unit
) {
    val rootTextSpan = +memo { TextSpan() }
    val ref = +compositionReference()
    compose(rootTextSpan, ref, child)
    +onDispose { disposeComposition(rootTextSpan, ref) }

    // TODO This is a temporary workaround due to lack of textStyle parameter of Text.
    val textSpan = if (rootTextSpan.children.size == 1) {
        rootTextSpan.children[0]
    } else {
        rootTextSpan
    }
    Text(
        textAlign = textAlign,
        textDirection = textDirection,
        softWrap = softWrap,
        overflow = overflow,
        textScaleFactor = textScaleFactor,
        maxLines = maxLines,
        selectionColor = selectionColor,
        text = textSpan
    )
}

/**
 * Text Widget Crane version.
 *
 * The Text widget displays text that uses multiple different styles. The text to display is
 * described using a tree of [TextSpan] objects, each of which has an associated style that is used
 * for that subtree. The text might break across multiple lines or might all be displayed on the
 * same line depending on the layout constraints.
 */
// TODO(migration/qqd): Add tests when text widget system is mature and testable.
@Composable
internal fun Text(
    /** How the text should be aligned horizontally. */
    textAlign: TextAlign = DefaultTextAlign,
    /** The directionality of the text. */
    textDirection: TextDirection = DefaultTextDirection,
    /**
     *  Whether the text should break at soft line breaks.
     *  If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     *  space.
     *  If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
     */
    softWrap: Boolean = DefaultSoftWrap,
    /** How visual overflow should be handled. */
    overflow: TextOverflow = DefaultOverflow,
    /** The number of font pixels for each logical pixel. */
    textScaleFactor: Float = 1.0f,
    /**
     *  An optional maximum number of lines for the text to span, wrapping if necessary.
     *  If the text exceeds the given number of lines, it will be truncated according to [overflow]
     *  and [softWrap].
     *  The value may be null. If it is not null, then it must be greater than zero.
     */
    maxLines: Int? = DefaultMaxLines,
    /**
     *  The color used to draw selected region.
     */
    selectionColor: Color = DefaultSelectionColor,
    /**
     * Composable TextSpan attached after [text].
     */
    text: TextSpan
) {
    val context = composer.composer.context
    val internalSelection = +state<TextSelection?> { null }
    val registrar = +ambient(SelectionRegistrarAmbient)
    val layoutCoordinates = +state<LayoutCoordinates?> { null }
    val lastTextPosition = text.toPlainText().length - 1

    fun attachContextToFont(
        text: TextSpan,
        context: Context
    ) {
        text.visitTextSpan() {
            it.style?.fontFamily?.let {
                it.context = context
            }
            true
        }
    }

    val style = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = style.merge(text.style)
    // Make a wrapper to avoid modifying the style on the original element
    val styledText = TextSpan(style = mergedStyle, children = mutableListOf(text))

    Semantics(
        label = styledText.toPlainText()
    ) {
        val renderParagraph = RenderParagraph(
            text = styledText,
            textAlign = textAlign,
            textDirection = textDirection,
            softWrap = softWrap,
            overflow = overflow,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines,
            selectionColor = selectionColor
        )
        // TODO(Migration/siyamed): This is temporary and should be removed when resource
        // system is resolved.
        attachContextToFont(styledText, context)

        val children = @Composable {
            // Get the layout coordinates of the text widget. This is for hit test of cross-widget
            // selection.
            OnPositioned(onPositioned = { layoutCoordinates.value = it })
            Draw { canvas, _ ->
                internalSelection.value?.let { renderParagraph.paintSelection(canvas, it) }
                renderParagraph.paint(canvas, Offset(0.0f, 0.0f))
            }
        }
        Layout(children = children, layoutBlock = { _, constraints ->
            renderParagraph.performLayout(constraints)
            layout(renderParagraph.width.px.round(), renderParagraph.height.px.round()) {}
        })

        +onCommit(renderParagraph) {
            val id = registrar.subscribe(object : TextSelectionHandler {
                // Get selection for the start and end coordinates pair.
                override fun getSelection(
                    selectionCoordinates: Pair<PxPosition, PxPosition>,
                    containerLayoutCoordinates: LayoutCoordinates,
                    mode: SelectionMode
                ): Selection? {
                    val relativePosition = containerLayoutCoordinates.childToLocal(
                        layoutCoordinates.value!!, PxPosition.Origin
                    )
                    val startPx = selectionCoordinates.first - relativePosition
                    val endPx = selectionCoordinates.second - relativePosition

                    val textBoundingBox = Rect(
                        top = 0f,
                        bottom = renderParagraph.height,
                        left = 0f,
                        right = renderParagraph.width)

                    if (!mode.isSelected(textBoundingBox, start = startPx, end = endPx)) {
                        internalSelection.value = null
                        return null
                    }

                    var textSelectionStart = getSelectionStart(startPx).first
                    var startLayoutCoordinates = getSelectionStart(startPx).second

                    var textSelectionEnd = getSelectionEnd(endPx).first
                    var endLayoutCoordinates = getSelectionEnd(endPx).second

                    if (textSelectionStart.offset == textSelectionEnd.offset) {
                        val wordBoundary = renderParagraph.getWordBoundary(textSelectionStart)
                        textSelectionStart =
                            TextPosition(wordBoundary.start, textSelectionStart.affinity)
                        textSelectionEnd = TextPosition(wordBoundary.end, textSelectionEnd.affinity)
                    }

                    internalSelection.value =
                        TextSelection(textSelectionStart.offset, textSelectionEnd.offset)

                    // TODO(qqd): Determine a set of coordinates around a character that we need.
                    // Clean up the lower layer's getCaretForTextPosition methods.
                    // Currently the left bottom corner of a character is returned.
                    return Selection(
                        startOffset =
                        renderParagraph.getCaretForTextPosition(textSelectionStart).second,
                        endOffset =
                        renderParagraph.getCaretForTextPosition(textSelectionEnd).second,
                        startLayoutCoordinates = startLayoutCoordinates,
                        endLayoutCoordinates = endLayoutCoordinates
                    )
                }

                fun getSelectionStart(start: PxPosition): Pair<TextPosition, LayoutCoordinates?> {
                    val top = 0.px
                    val left = 0.px

                    var selectionStart = TextPosition(0, TextAffinity.upstream)
                    var startLayoutCoordinates: LayoutCoordinates? = null

                    if (start.y >= top && start.x >= left) {
                        val startOffset = Offset(start.x.value, start.y.value)
                        selectionStart = TextPosition(
                            offset =
                            min(
                                max(
                                    renderParagraph.getPositionForOffset(startOffset).offset,
                                    0
                                ),
                                lastTextPosition
                            ), affinity = TextAffinity.upstream
                        )
                        startLayoutCoordinates = layoutCoordinates.value!!
                    }
                    return Pair(selectionStart, startLayoutCoordinates)
                }

                fun getSelectionEnd(end: PxPosition): Pair<TextPosition, LayoutCoordinates?> {
                    val bottom = renderParagraph.height.px
                    val right = renderParagraph.width.px

                    var selectionEnd = TextPosition(lastTextPosition, TextAffinity.upstream)
                    var endLayoutCoordinates: LayoutCoordinates? = null

                    if (end.y <= bottom && end.x <= right) {
                        val endOffset = Offset(end.x.value, end.y.value)
                        selectionEnd = TextPosition(
                            offset =
                            min(
                                max(
                                    renderParagraph.getPositionForOffset(endOffset).offset,
                                    0
                                ),
                                lastTextPosition
                            ), affinity = TextAffinity.upstream
                        )
                        endLayoutCoordinates = layoutCoordinates.value!!
                    }
                    return Pair(selectionEnd, endLayoutCoordinates)
                }
            })
            onDispose {
                registrar.unsubscribe(id)
            }
        }
    }
}

/**
 * Simplified version of [Text] component with minimal set of customizations.
 *
 * @param text The text to display.
 * @param style The text style for the text.
 */
@Composable
fun Text(
    text: String,
    style: TextStyle? = null,
    textAlign: TextAlign = DefaultTextAlign,
    textDirection: TextDirection = DefaultTextDirection,
    softWrap: Boolean = DefaultSoftWrap,
    overflow: TextOverflow = DefaultOverflow,
    maxLines: Int? = DefaultMaxLines
) {
    Text(
        textAlign = textAlign,
        textDirection = textDirection,
        softWrap = softWrap,
        overflow = overflow,
        textScaleFactor = 1.0f,
        maxLines = maxLines,
        selectionColor = DefaultSelectionColor,
        text = TextSpan(text = text, style = style)
    )
}

internal val CurrentTextStyleAmbient = Ambient.of<TextStyle>("current text style") {
    TextStyle()
}

/**
 * This component is used to set the current value of the Text style ambient. The given style will
 * be merged with the current style values for any missing attributes. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Composable
fun CurrentTextStyleProvider(value: TextStyle, @Children children: @Composable() () -> Unit) {
    val style = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = style.merge(value)
    CurrentTextStyleAmbient.Provider(value = mergedStyle) {
        children()
    }
}

/**
 * This effect is used to read the current value of the Text style ambient. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
fun currentTextStyle() =
    effectOf<TextStyle> { +ambient(CurrentTextStyleAmbient) }