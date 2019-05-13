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

import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.google.common.truth.Truth.assertThat

@SmallTest
@RunWith(JUnit4::class)
class NodesModelHolderTest {

    private val node1 = 1
    private val node2 = 2
    private lateinit var holder: NodesModelHolder<Int>

    @Before
    fun setup() {
        holder = NodesModelHolder()
    }

    @Test
    fun testHolderContainsPreviouslyAddedModel() {
        val model = TestModel("Test")
        holder.addModel(node1, model)

        holder.assertNodes(model, node1)
    }

    @Test
    fun testHolderAssociateBothNodesWithTheModel() {
        val model = TestModel("Test")
        holder.addModel(node1, model)
        holder.addModel(node2, model)

        holder.assertNodes(model, node1, node2)
    }

    @Test
    fun testHolderContainsModelWithChangedHashCode() {
        val model = TestModel("Original")
        holder.addModel(node1, model)
        model.content = "Changed"

        holder.assertNodes(model, node1)
    }

    private data class TestModel(var content: String)

    private fun NodesModelHolder<Int>.assertNodes(model: Any, vararg nodes: Int) {
        val expected = nodes.toList().sorted()
        val actual = mutableListOf<Int>()
        forEachNode(model) { actual.add(it) }
        assertThat(actual.sorted()).isEqualTo(expected)
    }
}