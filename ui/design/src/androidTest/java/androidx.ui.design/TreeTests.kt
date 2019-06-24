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

package androidx.ui.design

import androidx.compose.composer
import androidx.compose.SlotTable
import androidx.test.filters.SmallTest
import androidx.ui.baseui.ColoredRect
import org.junit.Test

import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TreeTests : DesignTest() {
    @Test
    fun simpleDesign() {
        show {
            Design {
                Column {
                    ColoredRect(color = Color(0xFF), width = 100.dp, height = 100.dp)
                }
            }
        }

        // Should be able to find the group for this test
        val group = tables.findGroupForFile("TreeTests")
        assertNotNull(group)

        // The group should have a non-empty bounding box
        assertEquals(0.px, group!!.box.top)
        assertEquals(0.px, group.box.left)
        assertNotEquals(0.px, group.box.right)
        assertNotEquals(0.px, group.box.bottom)
    }

    @Test
    fun inDesignMode() {
        var displayed = false
        show {
            Design {
                Column {
                    InDesignModeOnly {
                        ColoredRect(color = Color(0xFF), width = 100.dp, height = 100.dp)
                        displayed = true
                    }
                }
            }
        }

        assertTrue(displayed)
    }

    @Test
    fun notInDesignMode() {
        var displayed = false
        show {
            Column {
                InDesignModeOnly {
                    ColoredRect(color = Color(0xFF), width = 100.dp, height = 100.dp)
                    displayed = true
                }
            }
        }

        assertFalse(displayed)
    }
}

fun Iterable<SlotTable>.findGroupForFile(fileName: String) =
    map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()
fun SlotTable.findGroupForFile(fileName: String) = asTree().findGroupForFile(fileName)
fun Group.findGroupForFile(fileName: String): Group? {
    val position = position
    if (position != null && position.contains(fileName)) return this
    return children.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()
}
