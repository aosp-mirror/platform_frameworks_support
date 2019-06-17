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

class EditProcessor {

    private var mPreviousState: EditorState? = null
    private var mBuffer: EditingBuffer? = null

    fun onNewState(state: EditorState): Boolean {
        if (mPreviousState === state) {
            return false
        }

        mBuffer = EditingBuffer(
            initialText = state.text,
            initialSelection = state.selection)

        // TODO(nona): Tell to IME about the selection/extracted text changes.
        return true
    }

    fun onEditCommands(ops: List<EditOperation>): EditorState {
        var buffer = mBuffer
        if (buffer == null) {
            buffer = EditingBuffer(initialText = "", initialSelection = TextRange(0, 0))
            mBuffer = buffer
        }

        for (op in ops) {
            op.process(buffer)
        }

        val newState = EditorState(
            text = buffer.toString(),
            selection = TextRange(buffer.selectionStart, buffer.selectionEnd),
            composition = if (buffer.hasComposition()) {
                TextRange(buffer.compositionStart, buffer.compositionEnd)
            } else {
                null
            })

        mPreviousState = newState
        return newState
    }
}