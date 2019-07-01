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

package androidx.ui.core

import androidx.ui.graphics.Color
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorState
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.painting.Canvas
import androidx.ui.painting.TextPainter
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputFieldDelegateTest {

    private lateinit var delegate: InputFieldDelegate
    private lateinit var canvas: Canvas
    private lateinit var painter: TextPainter
    private lateinit var processor: EditProcessor
    private lateinit var onValueChange: (EditorState) -> Unit

    @Before
    fun setup() {
        painter = mock()
        canvas = mock()
        processor = mock()
        onValueChange = mock()

        delegate = InputFieldDelegate(painter, processor, onValueChange)
    }

    @Test
    fun draw_selection_test() {
        val selection = TextRange(0, 1)
        val selectionColor = Color.Blue

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = selection),
            editorStyle = EditorStyle(selectionColor = selectionColor))

        verify(painter, times(1)).paintBackground(
            eq(selection.start), eq(selection.end), eq(selectionColor), eq(canvas), any())
        verify(painter, times(1)).paint(eq(canvas), any())

        verify(painter, never()).paintCursor(any(), any())
    }

    @Test
    fun draw_cursor_test() {
        val cursor = TextRange(1, 1)

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = cursor),
            editorStyle = EditorStyle())

        verify(painter, times(1)).paintCursor(eq(cursor.start), eq(canvas))
        verify(painter, times(1)).paint(eq(canvas), any())
        verify(painter, never()).paintBackground(any(), any(), any(), any(), any())
    }

    @Test
    fun draw_composition_test() {
        val composition = TextRange(0, 1)
        val compositionColor = Color.Red

        val cursor = TextRange(1, 1)

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = cursor,
                composition = composition),
            editorStyle = EditorStyle(compositionColor = compositionColor))

        verify(painter, times(1)).paintBackground(
            eq(composition.start), eq(composition.end), eq(compositionColor), eq(canvas), any())
        verify(painter, times(1)).paint(eq(canvas), any())
        verify(painter, times(1)).paintCursor(eq(cursor.start), any())
    }

    @Test
    fun test_on_edit_command() {
        val ops = listOf(CommitTextEditOp("Hello, World", 1))
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))

        whenever(processor.onEditCommands(ops)).thenReturn(dummyEditorState)

        delegate.onEditCommand(ops)

        verify(onValueChange, times(1)).invoke(eq(dummyEditorState))
    }

    @Test
    fun test_on_release() {
        val position = PxPosition(100.px, 200.px)
        val offset = 10
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))

        whenever(painter.getPositionForOffset(position.toOffset())).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(dummyEditorState)

        delegate.onRelease(position)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is SetSelectionEditOp)
        verify(onValueChange, times(1)).invoke(eq(dummyEditorState))
    }

    @Test
    fun test_draw_order() {
        val canvas: Canvas = mock()

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = TextRange(1, 1),
                composition = TextRange(1, 3)),
            editorStyle = EditorStyle(compositionColor = Color.Red))

        inOrder(painter) {
            verify(painter).paintBackground(eq(1), eq(3), eq(Color.Red), eq(canvas), any())
            verify(painter).paintCursor(eq(1), eq(canvas))
        }
    }
}