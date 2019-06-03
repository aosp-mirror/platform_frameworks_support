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

package androidx.ui.material

import androidx.animation.ColorPropKey
import androidx.animation.DpPropKey
import androidx.animation.FastOutSlowInEasing
import androidx.animation.LinearEasing
import androidx.animation.TransitionDefinition
import androidx.animation.transitionDefinition
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.Transition
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.ColoredRect
import androidx.ui.baseui.SimpleImage
import androidx.ui.core.Dp
import androidx.ui.core.WithConstraints
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.layout.Stack
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Image

/**
 * A TabLayout is a high level component responsible for creating a tabbed layout that allows
 * navigation between related groups of content.
 *
 * A TabLayout contains a row of [Tab]s, with an area below that contains the content that is linked
 * to each tab.
 *
 * @param T the type of the item provided that will map to a [Tab]
 * @param items the list containing the items used to build the tab row
 * @param selectedIndex the index of the currently selected tab
 * @param tab the [Tab] to be emitted for the given index and element of type [T] in [items]
 */
@Composable
fun <T> TabLayout(
    items: List<T>,
    selectedIndex: Int,
    tab: @Composable() (Int, T) -> Unit,
    content: @Composable() (Int) -> Unit
) {
    FlexColumn {
        inflexible {
            TabRow(items, selectedIndex, tab)
        }
        flexible(flex = 1f) {
            // TODO: paging, synchronizing scroll state of the pager component with the TabRow
            content(selectedIndex)
        }
    }
}

/**
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab.
 *
 * @param T the type of the item provided that will map to a [Tab]
 * @param items the list containing the items used to build this TabRow
 * @param selectedIndex the index of the currently selected tab
 * @param tab the [Tab] to be emitted for the given index and element of type [T] in [items]
 */
@Composable
private fun <T> TabRow(
    items: List<T>,
    selectedIndex: Int,
    tab: @Composable() (Int, T) -> Unit
) {
    val count = items.size
    Surface(shape = +memo { RoundedRectangleBorder() }, color = +themeColor { primary }) {
        WithConstraints { constraints ->
            val totalWidth = +withDensity {
                constraints.maxWidth.value.px.toDp()
            }
            val indicatorWidth = totalWidth / count
            Stack {
                aligned(Alignment.Center) {
                    FlexRow {
                        items.forEachIndexed { index, item ->
                            expanded(1f) {
                                tab(index, item)
                            }
                        }
                    }
                }
                aligned(Alignment.BottomCenter) {
                    TabRowDivider()
                }
                val transitionDefinition = +memo(indicatorWidth) {
                    TabIndicatorTransitionDefinition(count, indicatorWidth)
                }
                Transition(transitionDefinition, toState = selectedIndex) { state ->
                    positioned(leftInset = state[IndicatorPosition], bottomInset = 0.dp) {
                        TabIndicator(indicatorWidth)
                    }
                }
            }
        }
    }
}

private val IndicatorPosition = DpPropKey()

/**
 * [TransitionDefinition] defining how the tab indicator animates between tabs, when a new tab is
 * selected.
 */
private fun TabIndicatorTransitionDefinition(tabCount: Int, indicatorWidth: Dp) =
    transitionDefinition {
        repeat(tabCount) { index ->
            state(index) {
                this[IndicatorPosition] = indicatorWidth * index
            }
        }

        transition {
            IndicatorPosition using tween {
                duration = 250
                easing = FastOutSlowInEasing
            }
        }
    }

@Composable
private fun TabIndicator(width: Dp) {
    ColoredRect(
        color = +themeColor { onPrimary },
        height = IndicatorHeight,
        width = width
    )
}

@Composable
private fun TabRowDivider() {
    val onPrimary = +themeColor { onPrimary }
    ColoredRect(color = (onPrimary.copy(alpha = DividerOpacity)), height = DividerHeight)
}

/**
 * A Tab represents a single page of content using a title and/or image. It represents its selected
 * state by tinting the title and/or image with [MaterialColors.onPrimary].
 *
 * @param text the title displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 */
@Composable
fun Tab(text: String? = null, icon: Image? = null, selected: Boolean, onSelected: () -> Unit) {
    val tint = +themeColor { onPrimary }
    when {
        text != null && icon != null -> CombinedTab(text, icon, selected, onSelected, tint)
        text != null -> TextTab(text, selected, onSelected, tint)
        icon != null -> IconTab(icon, selected, onSelected, tint)
        // Nothing provided here (?!), so let's just draw an empty tab that handles clicks
        else -> BaseTab(onSelected, {})
    }
}

/**
 * A base Tab that displays some content inside of a clickable ripple.
 *
 * @param onSelected the callback to be invoked when this tab is selected
 * @param children the composable content to be displayed inside of this Tab
 */
@Composable
private fun BaseTab(onSelected: () -> Unit, @Children children: @Composable() () -> Unit) {
    Ripple(bounded = true) {
        Clickable(onClick = onSelected) {
            children()
        }
    }
}

/**
 * A Tab that contains a title, and represents its selected state by tinting the title with [tint].
 *
 * @param text the title displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param tint the color that will be used to tint the title
 */
@Composable
private fun TextTab(text: String, selected: Boolean, onSelected: () -> Unit, tint: Color) {
    BaseTab(onSelected = onSelected) {
        Container(height = SmallTabHeight, alignment = Alignment.BottomCenter) {
            Padding(bottom = SingleRowTextBaselinePadding) {
                Transition(TabTransitionDefinition(tint), toState = selected) { state ->
                    // TODO: This should be aligned to the bottom padding by baseline,
                    // not raw layout
                    TabText(text, state[TabTintColor])
                }
            }
        }
    }
}

/**
 * A Tab that contains an icon, and represents its selected state by tinting the icon with [tint].
 *
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param tint the color that will be used to tint the icon
 */
@Composable
private fun IconTab(icon: Image, selected: Boolean, onSelected: () -> Unit, tint: Color) {
    BaseTab(onSelected = { onSelected() }) {
        Container(height = SmallTabHeight) {
            Transition(TabTransitionDefinition(tint), toState = selected) { state ->
                TabIcon(icon, state[TabTintColor])
            }
        }
    }
}

/**
 * A Tab that contains a title and an icon, and represents its selected state by tinting the
 * title and icon with [tint].
 *
 * @param text the title displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param tint the color that will be used to tint the title and icon
 */
@Composable
private fun CombinedTab(
    text: String,
    icon: Image,
    selected: Boolean,
    onSelected: () -> Unit,
    tint: Color
) {
    BaseTab(onSelected = { onSelected() }) {
        Container(height = LargeTabHeight) {
            Padding(
                top = SingleRowTextImagePadding,
                bottom = SingleRowTextBaselinePadding
            ) {
                Transition(TabTransitionDefinition(tint), toState = selected) { state ->
                    val tabTintColor = state[TabTintColor]
                    Column(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
                        TabIcon(icon, tabTintColor)
                        // TODO: This should be aligned to the bottom padding by baseline,
                        // not raw layout
                        TabText(text, tabTintColor)
                    }
                }
            }
        }
    }
}

private val TabTintColor = ColorPropKey()

/**
 * [TransitionDefinition] defining how the tint color opacity for a tab animates, when a new tab
 * is selected.
 */
private fun TabTransitionDefinition(color: Color) = +memo(color) {
    transitionDefinition {
        state(false) {
            this[TabTintColor] = color.copy(alpha = 0.74f)
        }

        state(true) {
            this[TabTintColor] = color
        }

        transition(toState = false, fromState = true) {
            TabTintColor using tween {
                duration = 150
                delay = 100
                easing = LinearEasing
            }
        }

        transition(fromState = true, toState = false) {
            TabTintColor using tween {
                duration = 100
                easing = LinearEasing
            }
        }
    }
}

@Composable
private fun TabText(text: String, color: Color) {
    val buttonTextStyle = +themeTextStyle { button }
    Padding(left = HorizontalTextPadding, right = HorizontalTextPadding) {
        Text(text = text, style = buttonTextStyle.copy(color = color), maxLines = 2)
    }
}

@Composable
private fun TabIcon(icon: Image, tint: Color) {
    Container(width = IconDiameter, height = IconDiameter) {
        SimpleImage(icon, tint)
    }
}

// TabRow specifications
private val DividerHeight = 1.dp
private val IndicatorHeight = 2.dp
private const val DividerOpacity = 0.12f

// Tab specifications

private val SmallTabHeight = 48.dp
private val LargeTabHeight = 72.dp

// The horizontal padding on the left and right of text
private val HorizontalTextPadding = 16.dp

private val IconDiameter = 24.dp

// TODO: this should be 18.dp + IndicatorHeight, but as we are not currently aligning by
// baseline, this can be 13.dp + IndicatorHeight to make it look more correct
private val SingleRowTextBaselinePadding = 13.dp + IndicatorHeight
private val SingleRowTextImagePadding = 12.dp
// TODO: need to figure out how many lines of text are drawn in the tab, so we can adjust
// the baseline padding
private val DoubleRowTextBaselinePadding = 8.dp + IndicatorHeight
private val DoubleRowTextImagePadding = 6.dp
