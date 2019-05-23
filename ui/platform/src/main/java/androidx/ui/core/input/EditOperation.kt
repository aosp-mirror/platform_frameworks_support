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

package androidx.ui.core.input

import androidx.ui.core.TextRange

/**
 * An enmu class for the type of edit operations.
 */
enum class OpType { REPLACE, SET_SELECTION, SET_COMPOSITION }

/**
 * A base class of all EditOperations
 */
open class EditOperation(val type: OpType)

/**
 * A class represents edit operation of replacing certain region text with given one.
 *
 * This also may change selection range and composition range. Usually, commitText
 * or setCompositionText explicitly specify or cancel selection and composition, so any selection
 * composition modification will be consistent state. In other words, you can expect subsequent
 * SetSelectionEdit/SetCompositionEdit or preceding SetCompositionEdit() with null for composition
 * cancellation events.
 *
 * - If target range is equals to selection/composition range, ReplaceEdit operation selects the
 *   replacement text.
 *
 *   Example 1-1: Replace selected text
 *     "Hello, [World.]" => ReplaceEdit(TextRange(7, 11), "Android.") => "Hello, [Android.]"
 *
 *   Example 1-2: Replace cursor position with selected text
 *     "Hello, []" => ReplaceEdit(TextRange(7, 7), "Android.") => "Hello, [Android.]"
 *
 *   Example 1-3: Delete selected region
 *     "Hello, [World.]" => ReplaceEdit(TextRange(7, 11), "") => "Hello, []"
 *
 * - If target range is covered by selections, replace with keeping the selection
 *
 *   Example 2-1: Replace inside selection
 *     "Hello, [World.]" => ReplaceEdit(TextRange(8, 10), "ORLD") => "Hello, [WORLD.]"
 *
 *   Example 2-2: Delete part of selected region
 *     "Hello, [World.]" => ReplaceEdit(TextRange(8, 11), "") => "Hello, [W]"
 *
 * - If target range covers the selections, replace and place the selection at the end of inserted
 *   text
 *
 *   Example 3-1: Replace text including selection range
 *     "Hello, W[orld]." => ReplaceEdit(TextRange(8, 11), "WORLD") => "Hello, WORLD.[]"
 *
 *   Example 3-2: Delete text including selection range
 *     "Hello, W[orld]." => ReplaceEdit(TextRange(8, 11), "") => "Hello, []"
 *
 * - If target range has part of selection, exclude replaced range from the selection.
 *
 *   Example 4-1: Replace text which is partially selected by the text. (adjusting end offset)
 *     "Hel[lo, Wor]ld." => ReplaceText(TextRange(7, 11), "Android.") => "Hel[lo, ]Android."
 *
 *   Example 4-2: Replace text which is partially selected by the text. (adjusting start offset)
 *     "Hel[lo, Wor]ld." => ReplaceText(TextRange(0, 6), "Greeting") => "Greeting[, Wor]ld."
 *
 *  Note: [] represents a selection range. The collapsed selection range means the cursor position.
 */
data class ReplaceEdit(
    /**
     * The text range to be replaced.
     */
    val range: TextRange,
    /**
     * The text to be replaced.
     */
    val text: String
) : EditOperation(OpType.REPLACE)

/**
 * A class represents edit operation of changing selection region in the text.
 *
 * This operation does not change any text buffer nor composition range.
 * Any previous selection state will be discarded and only new selection range will be effective.
 * The collapsed range represents a cursor position.
 * The range cannot be null. In other words, cursor or selection must be exists in the editor.
 */
data class SetSelectionEdit(
    /**
     * The text range to be selected.
     */
    val range: TextRange
) : EditOperation(OpType.SET_SELECTION)

/**
 * A class represents edit operation of changing composition region in the text.
 *
 * This operation does not change any text buffer nor selection range.
 * Any previous selection state will be discarded and new composition range will be effective.
 * If the range is null, no composition range is available in the text.
 */
data class SetCompositionEdit(
    /**
     * The text to be a composition text.
     * If null is set, that means cancelling composition.
     */
    val range: TextRange?
) : EditOperation(OpType.SET_COMPOSITION)
