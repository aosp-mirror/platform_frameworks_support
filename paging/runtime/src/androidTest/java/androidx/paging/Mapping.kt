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

package androidx.paging

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class MappingTest {

    @Test
    fun filter() {
        val list = listOf("a1", "a2", "a3", "b1", "b2", "c")
        assertEquals(listOf("a2", "b2"), list.filter { it.endsWith("2") })
    }

    @Test
    fun mapper() {
        val list = listOf("a1", "a2", "a3")

        assertEquals(listOf("a10", "a20", "a30"),
                list.map { it + "0" })
    }

    @Test
    fun separator() {
        val list = listOf("a1", "a2", "b3")

        assertEquals(
                listOf("A", "a1", "a2", "B", "b3"),
                list.generateSeparators(SeparatorGenerator { previous, next ->
                    if (next != null && previous?.get(0) != next[0]) {
                        next[0].toUpperCase().toString()
                    }
                    null
                }))

        assertEquals(
                listOf("a1", "a2", "A", "b3", "B"),
                list.generateSeparators(SeparatorGenerator { previous, next ->
                    if (previous != null && previous[0] != next?.get(0)) {
                        previous[0].toUpperCase().toString()
                    }
                    null
                }))

        assertEquals(
                listOf("a1", "a2", "A", "b3", "B"),
                list.generateSeparators(SeparatorGenerator { previous, next ->
                    if (previous != null && previous[0] != next?.get(0)) {
                        previous[0].toUpperCase().toString()
                    }
                    null
                }))


        assertEquals(
                listOf("a1", "a2", "A", "b3", "B"),
                list.generateSeparators(SeparatorGenerator { previous, next ->
                    if (previous != null && previous[0] != next?.get(0)) {
                        previous[0].toUpperCase().toString()
                    }
                    null
                }))

        assertEquals(
                listOf("c", "a1", "c", "a2", "c", "b3", "c"),
                list.generateSeparators(SeparatorGenerator { _, _ -> "c" }))
    }

    @Test
    fun multiType() {
        val list = listOf("a1", "a2", "b3")
        assertEquals(
                listOf('A', "a1", "a2", 'B', "b3"),
                list.generateSeparators(SeparatorGenerator<String, Any> { previous, next ->
                    if (next != null && previous?.get(0) != next[0]) {
                        next[0].toUpperCase()
                    } else {
                        null
                    }
                }))
    }
}

fun <I : O, O> List<I>.generateSeparators(separatorator: SeparatorGenerator<I, O>): List<O> {
    val output = mutableListOf<O>()
    for (i in 0..size) {
        val sep = separatorator.generate(getOrNull(i-1), getOrNull(i))
        if (sep != null) {
            output.add(sep)
        }
        if (i != size) output.add(get(i))
    }
    return output
}
