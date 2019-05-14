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

package androidx.ui.material.studies.rally.components

import androidx.animation.FloatPropKey
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.annotation.DrawableRes
import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.unaryPlus
import androidx.ui.animation.Transition
import androidx.ui.baseui.Clickable
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.studies.Icon
import androidx.ui.material.themeTextStyle

data class Tab(
    val title: String,
    @DrawableRes val icon: Int,
    val children: @Composable() () -> Unit
)

@Composable
fun TabLayout(
    selectedItem: Int = 0,
    items: List<Tab>,
    onSelected: ((Int) -> Unit)? = null
) {
    FlexColumn {
        inflexible {
            TabLayoutToolbar(
                selectedItem = selectedItem,
                items = items,
                onSelected = onSelected
            )
        }

        flexible(flex = 1.0f) {
            items[selectedItem].children()
        }
    }
}

@Composable
private fun TabLayoutToolbar(
    selectedItem: Int = 0,
    items: List<Tab>,
    onSelected: ((Int) -> Unit)? = null
) {
    Container(height = 56.dp, padding = EdgeInsets(16.dp)) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            items.forEachIndexed { index, it ->
                inflexible {
                    TabToolbarItem(
                        item = it,
                        isSelected = index == selectedItem,
                        onClick = { onSelected?.invoke(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabToolbarItem(
    item: Tab,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null
) {
    Transition(
        definition = tabSelectionTransition,
        toState = isSelected
    ) @Composable { state ->
        val select = state[selectionAmount]

        Clickable(onClick = onClick) {
            Row {
                Container(width = 24.dp, height = 24.dp) {
                    Icon(item.icon, alpha = 0.65f + (select * 0.35f))
                }
                WidthSpacer(8.dp)
                Clip(select) {
                    Text(text = item.title, maxLines = 1, style = +themeTextStyle { button })
                }
            }
        }
    }
}

private val selectionAmount = FloatPropKey()
private val tabSelectionTransition = transitionDefinition {
    state(true) {
        this[selectionAmount] = 1.0f
    }

    state(false) {
        this[selectionAmount] = 0.0f
    }

    transition(null to null) {
        selectionAmount using tween {
            easing = LinearEasing
            duration = 175
        }
    }
}