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

package androidx.ui.layout

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.ParentData
import androidx.ui.core.Placeable

/**
 * Collects information about the children of a [Table] when
 * its body is executed with a [TableChildren] as argument.
 */
class TableChildren internal constructor() {

    internal val tableChildren = mutableListOf<@Composable() () -> Unit>()
    private var rowIndex = 0

    fun row(children: @Composable() () -> Unit) {
        tableChildren += {
            ParentData(data = TableChildData(rowIndex++), children = children)
        }
    }
}

/**
 * Parent data associated with children to assign a row index.
 */
private data class TableChildData(val rowIndex: Int)

private val Measurable.rowIndex get() = (parentData as TableChildData).rowIndex

/**
 * Layout model that arranges its children into rows and columns.
 */
@Composable
fun Table(
    childAlignment: Alignment = Alignment.Center,
    @Children(composable = false) block: TableChildren.() -> Unit
) {
    val children: @Composable() () -> Unit = with(TableChildren()) {
        apply(block)
        val composable = @Composable {
            tableChildren.forEach { it() }
        }
        composable
    }

    Layout(children = children, layoutBlock = { measurables, constraints ->
        val measurablesByRow = measurables.groupBy { it.rowIndex }

        val rows = measurablesByRow.size
        val columns = measurablesByRow.map { it.value.size }.max() ?: 0

        val placeables = Array<Array<Placeable?>>(rows) { arrayOfNulls(columns) }
        if (rows > 0 && columns > 0) {
            val childConstraints = Constraints(
                maxWidth = constraints.maxWidth / columns,
                maxHeight = constraints.maxHeight / rows
            )
            for (i in 0 until rows) {
                for (j in 0 until columns) {
                    placeables[i][j] =
                        measurablesByRow[i]?.getOrNull(j)?.measure(childConstraints)
                }
            }
        }

        val tableWidth = constraints.maxWidth
        val tableHeight = constraints.maxHeight

        layout(tableWidth, tableHeight) {
            for (i in 0 until rows) {
                for (j in 0 until columns) {
                    placeables[i][j]?.let {
                        val position = childAlignment.align(
                            IntPxSize(
                                width = tableWidth / columns - it.width,
                                height = tableHeight / rows - it.height
                            )
                        )
                        it.place(
                            x = (tableWidth / columns) * j + position.x,
                            y = (tableHeight / rows) * i + position.y
                        )
                    }
                }
            }
        }
    })
}
