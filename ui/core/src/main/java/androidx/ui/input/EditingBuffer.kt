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

package androidx.ui.input

import androidx.ui.core.TextRange
import java.lang.IllegalArgumentException

/**
 * The editing buffer
 *
 * This class manages the all editing relate states, editing buffers, selection, styles, etc.
 */
internal class EditingBuffer(
    /**
     * The initial text of this editing buffer
     *
     * TODO(nona): Change the argument to AnnotatedString once it is introduced
     */
    initText: String,

    /**
     * The initial selection range of this buffer.
     * If you provide collapsed selection, it is treated as the cursor position. The cursor and
     * selection cannot exists at the same time.
     * The selection must points the valid index of the initText, otherwise
     * IndexOutOfBoundsException will be thrown.
     */
    initSelection: TextRange // The initial selection range
) {
    internal companion object {
        const val NOWHERE = -1
    }

    private val gapBuffer = PartialGapBuffer(initText)

    // TODO(nona): Add style data.

    /**
     * The inclusive selection start offset
     */
    var selectionStart = initSelection.start
        private set

    /**
     * The exclusive selection end offset
     */
    var selectionEnd = initSelection.end
        private set

    /**
     * The inclusive composition start offset
     *
     * If there is no composing text, returns -1
     */
    var compositionStart = NOWHERE
        private set

    /**
     * The exclusive composition end offset
     *
     * If there is no composing text, returns -1
     */
    var compositionEnd = NOWHERE
        private set

    init {
        val start = initSelection.start
        val end = initSelection.end
        if (start < 0 || start > initText.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${initText.length}")
        }

        if (end < 0 || end > initText.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${initText.length}")
        }

        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }
    }

    /**
     * Replace the text and move the cursor to the end of inserted text.
     *
     * This function cancels selection if there.
     *
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer
     * @throws IllegalArgumentException if start is larger than end. (reversed range)
     */
    fun replace(start: Int, end: Int, text: String) {

        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}")
        }

        if (end < 0 || end > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}")
        }

        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }

        gapBuffer.replace(start, end, text)

        // On Android, all text modification APIs also provides explicit cursor location. On the
        // hand, desktop application usually doesn't. So, here tentatively move the cursor to the
        // end offset of the editing area for desktop like application. In case of Android,
        // implementation will call setSelection immediately after replace function to update this
        // tentative cursor location.
        selectionStart = start + text.length
        selectionEnd = start + text.length

        // Similarly, if text modification happens, cancel ongoing composition. If caller want to
        // change the composition text, it is caller responsibility to call setComposition again
        // to set composition range after replace function.
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    /**
     * Mark the specified area of the text as selected text.
     *
     * You can set cursor by specifying the same value to `start` and `end`.
     * The reversed range is not allowed.
     * @param start the inclusive start offset of the selection
     * @param end the exclusive end offset of the selection
     *
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer.
     * @throws IllegalArgumentException if start is larger than end. (reversed range)
     */
    fun setSelection(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}")
        }
        if (end < 0 || end> gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}")
        }
        if (start > end) {
            throw IllegalArgumentException("Do not set reversed range: $start > $end")
        }

        selectionStart = start
        selectionEnd = end
    }

    /**
     * Mark the specified area of the text as composition text.
     *
     * The empty range or reversed range is not allowed.
     * Use clearComposition in case of clearing composition.
     *
     * @param start the inclusive start offset of the composition
     * @param end the exclusive end offset of the composition
     *
     * @throws IndexOutOfBoundsException if start or end offset is ouside of current buffer
     * @throws IllegalArgumentException if start is larger than or equal to end. (reversed or
     *                                  collapsed range)
     */
    fun setComposition(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}")
        }
        if (end < 0 || end> gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}")
        }
        if (start >= end) {
            throw IllegalArgumentException("Do not set reversed or empty range: $start > $end")
        }

        compositionStart = start
        compositionEnd = end
    }

    /**
     * Clears ongoing composition range if there
     */
    fun clearComposition() {
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    override fun toString(): String = gapBuffer.toString()
}