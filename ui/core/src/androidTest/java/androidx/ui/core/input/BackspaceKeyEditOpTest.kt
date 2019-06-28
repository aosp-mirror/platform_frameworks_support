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

import androidx.test.filters.SmallTest
import androidx.ui.core.TextRange
import androidx.ui.input.BackspaceKeyEditOp
import androidx.ui.input.EditingBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class BackspaceKeyEditOpTest {
    private val CH1 = "\uD83D\uDE00" // U+1F600
    private val CH2 = "\uD83D\uDE01" // U+1F601
    private val CH3 = "\uD83D\uDE02" // U+1F602
    private val CH4 = "\uD83D\uDE03" // U+1F603
    private val CH5 = "\uD83D\uDE04" // U+1F604

    // U+1F468 U+200D U+1F469 U+200D U+1F467 U+200D U+1F466
    private val FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"

    @Test
    fun test_delete() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 1))

        BackspaceKeyEditOp().process(eb)

        assertEquals("BCDE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_from_offset0() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 0))

        BackspaceKeyEditOp().process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_with_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(2, 3))

        BackspaceKeyEditOp().process(eb)

        assertEquals("ABDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_with_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 1))
        eb.setComposition(2, 3)

        BackspaceKeyEditOp().process(eb)

        assertEquals("ABDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2, 2))

        BackspaceKeyEditOp().process(eb)

        assertEquals("$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_with_selection_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4, 6))

        BackspaceKeyEditOp().process(eb)

        assertEquals("$CH1$CH2$CH4$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_with_composition_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2, 2))
        eb.setComposition(4, 6)

        BackspaceKeyEditOp().process(eb)

        assertEquals("$CH1$CH2$CH4$CH5", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_with_composition_zwj_emoji() {
        val eb = EditingBuffer("$FAMILY$FAMILY", TextRange(FAMILY.length, FAMILY.length))

        BackspaceKeyEditOp().process(eb)

        assertEquals(FAMILY, eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }
}