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

package androidx.ui.framework.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.surface.Surface
import androidx.ui.material.themeColor
import androidx.ui.material.themeTextStyle

/**
 * Playground app created to investigate group semantics.
 * This sample creates a group of items, and showcases how the accessibility system can navigate
 * through the group using group actions. */
class GroupSemanticsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {

                // Level 2 API.
                // GroupLevel2Api()

                // Level 3 API.
                GroupLevel3Api()
            }
        }
    }

    /**
     * The level 2 API provides a [GroupSemantics] component that accepts actions corresponding to
     * group actions. It is to be used when the user needs more control than what the level 3 API
     * provides.
     */
    @Suppress("FunctionName", "Unused")
    @Composable
    fun GroupLevel2Api() {

        val groupItems = listOf("Apple", "Banana", "Cashew", "Durian")
        val max = groupItems.count()

        // Used to keep track of the item that is currently focused.
        val focusedIndex = +state { 0 }

        GroupSemantics(
            onNext = SemanticAction<Unit>(
                defaultParam = Unit,
                action = { focusedIndex.value = rollingIncrement(focusedIndex.value, max) }
            ),
            onPrevious = SemanticAction<Unit>(
                defaultParam = Unit,
                action = { focusedIndex.value = rollingDecrement(focusedIndex.value, max) }
            ),
            onDown = SemanticAction<Unit>(
                defaultParam = Unit,
                action = { focusedIndex.value = rollingIncrement(focusedIndex.value, max) }
            ),
            onUp = SemanticAction<Unit>(
                defaultParam = Unit,
                action = { focusedIndex.value = rollingDecrement(focusedIndex.value, max) }
            ),
            onGoTo = SemanticAction<Int>(
                defaultParam = focusedIndex.value,
                action = { focusedIndex.value = it.value }
            )) { groupActions ->
            MaterialTheme {
                Column {
                    for (index in 0 until groupItems.size) {
                        // This is a good example why we do not want the gesture detectors to use
                        // actions. If the gesture detectors just used a lambda, we could easily
                        // invoke the action here. However, we could argue that keeping this API
                        // allows the user to use a custom phrase for the action.
                        PressGestureDetectorWithActions(
                            onRelease = SemanticAction(
                                phrase = "Select this item",
                                defaultParam = Unit,
                                action = {
                                    groupActions.goTo.invoke(ActionCaller.PointerInput, index)
                                })
                        ) {
                            Surface(
                                shape = RoundedRectangleBorder(),
                                color = +themeColor {
                                    if (focusedIndex.value == index) secondary else background
                                }) {
                                Text(
                                    text = groupItems[index],
                                    style = +themeTextStyle { h4.copy() })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun rollingIncrement(current: Int, max: Int) = (current + 1) % max

    private fun rollingDecrement(current: Int, max: Int) = (current + max - 1) % max

    /**
     * The Level 3 API provides a [Group] component, and we can add [GroupRow]s for each item we
     * want to navigate to. This example handles only rows, but we could add columns too. This
     * [Group] component automatically emits group semantic actions.
     */
    @Suppress("FunctionName", "Unused")
    @Composable
    fun GroupLevel3Api() {
        Group {
            GroupRow {
                Text(text = "Apple", style = +themeTextStyle { h4.copy() })
            }
            GroupRow {
                Text(text = "Banana", style = +themeTextStyle { h4.copy() })
            }
            GroupRow {
                Text(text = "Cashew", style = +themeTextStyle { h4.copy() })
            }
            GroupRow {
                Text(text = "Durian", style = +themeTextStyle { h4.copy() })
            }
        }
    }
}