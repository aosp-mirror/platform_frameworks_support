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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

fun <T> MutableList<T>.transform() : Transformation.StageOneBuilder<T> {
    return Transformation.StageOneBuilder<T>();
}

fun <BaseType, CurrentItemType : BaseType, SeparatorType : BaseType>
        Transformation.StageOneBuilder<CurrentItemType>.inject(gen: SeparatorGenerator<CurrentItemType, SeparatorType>): Transformation.StageTwoBuilder<BaseType> {
    return Transformation.inject<BaseType, CurrentItemType, SeparatorType>(this, gen)
}

@RunWith(JUnit4::class)
class TransformedPageTest {
    open class A(val foo: String = "")
    data class B(val bar: String = "") : A(bar)
    data class C(val baz: String = "") : A(baz)

    fun simple() {
        val f : PagedList<A> = mutableListOf("aa", "bb", "cc")
                .transform()
                .map { B(it) }
                .filter { it.bar != "" }
                .inject<A, B, C>(SeparatorGenerator { previous, next ->
                    if (previous != null
                            && next != null
                            && previous.bar[0] != next.bar[0]) {
                        C(next.bar[0].toString())
                    }
                    null
                })
                .build() as PagedList<A>
    }

    @Test
    fun simpleMap() {
        val page = TransformedPage<String>(
                listOf(Transform.Mapper { it: String -> it + "a" }),
                0,
                listOf("a", "b"))
        assertEquals(listOf("aa", "ba"), page.mItems)
        assertEquals(listOf(0, 1), page.mItemOriginalIndices)
    }

    @Test
    fun simpleFilter() {
        val page = TransformedPage<String>(
                listOf(Transform.Filter { it: String -> !it.endsWith("1") }),
                0,
                listOf("a1", "a2", "b1", "b2", "c"))
        assertEquals(listOf("a2", "b2", "c"), page.mItems)
        assertEquals(listOf(1, 3, 4), page.mItemOriginalIndices)
    }

    @Test
    fun filterMap() {
        val page = TransformedPage<String>(
                listOf(
                        Transform.Filter { it: String -> !it.endsWith("1") },
                        Transform.Mapper { it: String -> it + "a" }),
                0,
                listOf("a1", "a2", "b1", "b2", "c"))
        assertEquals(listOf("a2a", "b2a", "ca"), page.mItems)
        assertEquals(listOf(1, 3, 4), page.mItemOriginalIndices)
    }

    @Test
    fun filterMapFilterMap() {
        val page = TransformedPage<String>(
                listOf(
                        Transform.Filter { it: String -> !it.endsWith("1") },
                        Transform.Mapper { it: String -> it + "a" },
                        Transform.Filter { it: String -> it.contains("2a") },
                        Transform.Mapper { it: String -> it + "b" }),
                0,
                listOf("a1", "a2", "b1", "b2", "c"))
        assertEquals(listOf("a2ab", "b2ab"), page.mItems)
        assertEquals(listOf(1, 3), page.mItemOriginalIndices)
    }

    @Test
    fun inject() {
        val page = TransformedPage<String, String>(
                listOf(),
                0,
                listOf("a1", "a2", "b1", "b2", "c1"),
                SeparatorGenerator<String, String> { previous, next ->
                    // can assume params are nonnull *within* TransformedPage
                    if (previous!![0] != next!![0]) next[0].toUpperCase().toString() else null
                })

        assertEquals(listOf("a1", "a2", "B", "b1", "b2", "C", "c1"), page.mItems)
        assertEquals(listOf(0, 1, 1, 2, 3, 3, 4), page.mItemOriginalIndices)
    }

    @Test
    fun injectHierarchy() {
        @Suppress("EqualsOrHashCode")
        open class Base(val name: String, val isHeader: Boolean) {
            override fun equals(other: Any?): Boolean {
                return other != null
                        && other is Base
                        && name == other.name
                        && isHeader == other.isHeader;
            }
        }
        class Header(name: String) : Base(name, true)
        class Item(name: String) : Base(name, false)

        val page = TransformedPage<Base, Item>(
                listOf(),
                0,
                listOf("a1", "a2", "b1", "b2", "c1").map { Item(it) },
                SeparatorGenerator<Item, Base> { previous, next ->
                    // can assume params are nonnull *within* TransformedPage
                    if (previous!!.name[0] != next!!.name[0]) Header(next.name[0].toUpperCase().toString()) else null
                })

        assertEquals(listOf(Item("a1"), Item("a2"),
                Header("B"), Item("b1"), Item("b2"),
                Header("C"), Item("c1")), page.mItems)
        assertEquals(listOf(0, 1, 1, 2, 3, 3, 4), page.mItemOriginalIndices)
    }

    @Test
    fun offset() {
        val page = TransformedPage<String>(
                listOf(Transform.Mapper { it: String -> it + "a" }),
                0,
                listOf("a", "b"))
        assertEquals(listOf("aa", "ba"), page.mItems)

        assertEquals(0, page.getOriginalIndex(0))
        assertEquals(1, page.getOriginalIndex(1))
        page.offset(100)
        assertEquals(100, page.getOriginalIndex(0))
        assertEquals(101, page.getOriginalIndex(1))
    }
}
