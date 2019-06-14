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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextRangeTest {
    @Test
    fun equality() {
        assertEquals(TextRange(0, 0), TextRange(0, 0))
        assertEquals(TextRange(0, 1), TextRange(0, 1))
        assertNotEquals(TextRange(0, 1), TextRange(0, 0))
    }

    @Test
    fun rangeCollapsedTest() {
        assertTrue(TextRange(0, 0).collapsed)
        assertFalse(TextRange(0, 1).collapsed)
        assertTrue(TextRange(1, 1).collapsed)
        assertFalse(TextRange(1, 2).collapsed)
    }

    @Test
    fun intersectTest() {
        assertTrue(TextRange(0, 2).intersect(TextRange(1, 2)))
        assertTrue(TextRange(0, 1).intersect(TextRange(0, 1)))
        assertTrue(TextRange(0, 2).intersect(TextRange(0, 1)))
        assertTrue(TextRange(0, 1).intersect(TextRange(0, 2)))
        assertFalse(TextRange(0, 1).intersect(TextRange(1, 2)))
        assertFalse(TextRange(0, 1).intersect(TextRange(2, 3)))
        assertFalse(TextRange(1, 2).intersect(TextRange(0, 1)))
        assertFalse(TextRange(2, 3).intersect(TextRange(0, 1)))
    }

    @Test
    fun containsRangeTest() {
        assertTrue(TextRange(0, 2).contains(TextRange(0, 1)))
        assertTrue(TextRange(0, 2).contains(TextRange(0, 2)))
        assertFalse(TextRange(0, 2).contains(TextRange(0, 3)))
        assertFalse(TextRange(0, 2).contains(TextRange(1, 3)))
    }

    @Test
    fun containsOffsetTest() {
        assertTrue(TextRange(0, 2).contains(0))
        assertTrue(TextRange(0, 2).contains(1))
        assertFalse(TextRange(0, 2).contains(2))
        assertFalse(TextRange(0, 2).contains(3))
    }
}