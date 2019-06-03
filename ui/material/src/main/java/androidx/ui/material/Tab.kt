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

import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.ColoredRect
import androidx.ui.baseui.SimpleImage
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.ipx
import androidx.ui.core.looseMin
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.Padding
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Image

/**
 * High level component for creating a tabbed layout that allows navigation between related groups
 * of content. A TabLayout contains a TabRow, with an area below that contains the content that each
 * tab displays.
 *
 * This TabLayout displays a custom TabRow, such as a [TabRowScope.TabRow],
 * with a optional content area below, in case you don't want to show the corresponding content
 * directly beneath this component.
 *
 * @param tabRow the row of tabs to be displayed above the content
 * @param content the optional content to be displayed depending on the currently selected tab
 */
@Composable
fun TabLayout(
    tabRow: @Composable TabRowScope.() -> Unit,
    @Children content: @Composable() () -> Unit? = {}
) {
    val scope = +memo { TabRowScope() }
    FlexColumn {
        inflexible {
            scope.tabRow()
        }
        flexible(flex = 1f) {
            content()
        }
    }
}

/**
 * High level component for creating a tabbed layout that allows navigation between related groups
 * of content. A TabLayout contains a TabRow, with an area below that contains the content that each
 * tab displays.
 *
 * This TabLayout displays a list of tabs with titles. See the [TabLayout] overload that accepts a
 * custom TabRow if you want to customize the displayed tabs.
 *
 * @param tabs a list of [String]s used as the titles for the tabs
 * @param selectedIndex the index of the currently selected tab
 * @param onSelected the callback to be invoked when a tab is selected
 * @param content the content to be displayed depending on the currently selected tab
 */
@JvmName("TextTabLayout")
@Composable
fun TabLayout(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    @Children content: @Composable() (selected: Int) -> Unit
) {
    TabLayout(
        tabRow = { TabRow(tabs, selectedIndex, onSelected) }) {
        content(selectedIndex)
    }
}

/**
 * High level component for creating a tabbed layout that allows navigation between related groups
 * of content. A TabLayout contains a TabRow, with an area below that contains the content that each
 * tab displays.
 *
 * This TabLayout displays a list of tabs with icons. See the [TabLayout] overload that accepts a
 * custom TabRow if you want to customize the displayed tabs.
 *
 * @param tabs a list of [Image]s used as the icons for the tabs
 * @param selectedIndex the index of the currently selected tab
 * @param onSelected the callback to be invoked when a tab is selected
 * @param content the content to be displayed depending on the currently selected tab
 */
@JvmName("ImageTabLayout")
@Composable
fun TabLayout(
    tabs: List<Image>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    @Children content: @Composable() (selected: Int) -> Unit
) {
    TabLayout(
        tabRow = { TabRow(tabs, selectedIndex, onSelected) }) {
        content(selectedIndex)
    }
}

/**
 * High level component for creating a tabbed layout that allows navigation between related groups
 * of content. A TabLayout contains a TabRow, with an area below that contains the content that each
 * tab displays.
 *
 * This TabLayout displays a list of tabs with titles and icons. See the [TabLayout] overload that
 * accepts a custom TabRow if you want to customize the displayed tabs.
 *
 * @param tabs a list of [Pair]s of [String] and [Image] used as the title and icon for the tabs
 * @param selectedIndex the index of the currently selected tab
 * @param onSelected the callback to be invoked when a tab is selected
 * @param content the content to be displayed depending on the currently selected tab
 */
@Composable
fun TabLayout(
    tabs: List<Pair<String, Image>>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    @Children content: @Composable() (selected: Int) -> Unit
) {
    TabLayout(
        tabRow = { TabRow(tabs, selectedIndex, onSelected) }) {
        content(selectedIndex)
    }
}

/**
 * A scope containing TabRows, which can be used from within [TabLayout].
 */
class TabRowScope internal constructor() {

    /**
     * A TabRow contains a list of Tabs, and displays an indicator underneath the currently
     * selected tab.
     *
     * This TabRow displays a list of tabs, such as a [TabScope.Tab].
     *
     * @param selectedIndex the index of the currently selected tab
     * @param tabs a list of tab components to be displayed inside this TabRow
     */
    @Composable
    fun TabRow(
        selectedIndex: Int,
        tabs: TabScope.() -> List<@Composable() () -> Unit>
    ) {
        val scope = +memo { TabScope() }
        val children = scope.tabs()
        Layout(layoutBlock = { measurables, constraints ->
            val row = measurables[0].measure(constraints.looseMin())
            val indicatorWidth = row.width / children.size
            val tabIndicator = measurables[1].measure(
                Constraints.tightConstraints(
                    width = indicatorWidth,
                    height = IndicatorHeight.toIntPx()
                )
            )
            // TODO: specs for this
            // The indicator should (?) overlap the bottom edge, drawing one dp below and above
            // the tab
            // TODO: specs?
            val totalHeight = row.height + IndicatorOffset.toIntPx()
            layout(width = constraints.maxWidth, height = totalHeight) {
                row.place(0.ipx, 0.ipx)
                tabIndicator.place(
                    (indicatorWidth * selectedIndex),
                    totalHeight - IndicatorHeight.toIntPx()
                )
            }
        }, children = {
            // TODO: Need to measure the width, if an individual tab is wider than the amount of the
            //  screen it has been allocated (e.g 3 tabs have 1/3rd each) then they should all have
            //  the same width as the widest tab and scroll
            Surface(shape = +memo { RoundedRectangleBorder() }, color = Color.Transparent) {
                FlexRow {
                    children.forEach { child ->
                        expanded(flex = 1f) {
                            child()
                        }
                    }
                }
            }
            ColoredRect(color = +themeColor { primary })
        })
    }

    /**
     * A TabRow contains a list of Tabs, and displays an indicator underneath the currently
     * selected tab.
     *
     * This TabRow displays a list of tabs with titles. See the [TabRow] overload that
     * accepts a list of Tabs if you want to customize the displayed tabs.
     *
     * @param tabs a list of [String]s used as the titles for the tabs
     * @param selectedIndex the index of the currently selected tab
     * @param onSelected the callback to be invoked when a tab is selected
     */
    @JvmName("TextTabRow")
    @Composable
    fun TabRow(
        tabs: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        TabRow(selectedIndex) {
            @Suppress("RemoveExplicitTypeArguments")
            tabs.mapIndexed<String, @Composable() () -> Unit> { index, text ->
                {
                    Tab(
                        selected = (index == selectedIndex),
                        onSelected = { onSelected(index) },
                        text = text
                    )
                }
            }
        }
    }

    /**
     * A TabRow contains a list of Tabs, and displays an indicator underneath the currently
     * selected tab.
     *
     * This TabRow displays a list of tabs with icons. See the [TabRow] overload that
     * accepts a list of Tabs if you want to customize the displayed tabs.
     *
     * @param tabs a list of [Image]s used as the icons for the tabs
     * @param selectedIndex the index of the currently selected tab
     * @param onSelected the callback to be invoked when a tab is selected
     */
    @JvmName("ImageTabRow")
    @Composable
    fun TabRow(
        tabs: List<Image>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        TabRow(selectedIndex) {
            @Suppress("RemoveExplicitTypeArguments")
            tabs.mapIndexed<Image, @Composable() () -> Unit> { index, image ->
                {
                    Tab(
                        selected = (index == selectedIndex),
                        onSelected = { onSelected(index) },
                        image = image
                    )
                }
            }
        }
    }

    /**
     * A TabRow contains a list of Tabs, and displays an indicator underneath the currently
     * selected tab.
     *
     * This TabRow displays a list of tabs with titles and icons. See the [TabRow] overload that
     * accepts a list of Tabs if you want to customize the displayed tabs.
     *
     * @param tabs a list of [Pair]s of [String] and [Image] used as the title and icon for the tabs
     * @param selectedIndex the index of the currently selected tab
     * @param onSelected the callback to be invoked when a tab is selected
     */
    @Composable
    fun TabRow(
        tabs: List<Pair<String, Image>>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        TabRow(selectedIndex) {
            @Suppress("RemoveExplicitTypeArguments")
            tabs.mapIndexed<Pair<String, Image>, @Composable() () -> Unit> { index, (text, image) ->
                {
                    Tab(
                        selected = (index == selectedIndex),
                        onSelected = { onSelected(index) },
                        text = text,
                        image = image
                    )
                }
            }
        }
    }

    private val IndicatorHeight = 2.dp
    private val IndicatorOffset = 1.dp

    /**
     * A scope containing Tabs, which can be used from within [TabRow].
     */
    class TabScope internal constructor() {

        /**
         * A Tab that displays some content inside of a clickable ripple.
         *
         * @param onSelected the callback to be invoked when this tab is selected
         * @param children the composable content to be displayed inside of this Tab
         */
        @Composable
        fun Tab(onSelected: () -> Unit, @Children children: @Composable() () -> Unit) {
            Ripple(bounded = true) {
                Clickable(onClick = onSelected) {
                    children()
                }
            }
        }

        /**
         * A Tab that contains a title, and represents its selected state by tinting the title with
         * [MaterialColors.primary].
         *
         * See the [Tab] overload that accepts any component if you want to display custom content
         * inside a tab.
         *
         * @param text the title displayed in this tab
         * @param selected whether this tab is selected or not
         * @param onSelected the callback to be invoked when this tab is selected
         */
        // TODO: spec for white-on-primary
        @Composable
        fun Tab(text: String, selected: Boolean, onSelected: () -> Unit) {
            val buttonTextStyle = +themeTextStyle { button }
            val textStyle =
                buttonTextStyle.copy(color = if (selected) +themeColor { primary } else Color.Gray)
            Tab(onSelected = onSelected) {
                Container(height = SmallTabHeight) {
                    HorizontalTabPadding {
                        // TODO: check text baseline alignment
                        Text(text = text, style = textStyle)
                    }
                }
            }
        }

        /**
         * A Tab that contains an icon, and represents its selected state by tinting the icon with
         * [MaterialColors.primary].
         *
         * See the [Tab] overload that accepts any component if you want to display custom content
         * inside a tab.
         *
         * @param image the icon displayed in this tab
         * @param selected whether this tab is selected or not
         * @param onSelected the callback to be invoked when this tab is selected
         */
        @Composable
        fun Tab(image: Image, selected: Boolean, onSelected: () -> Unit) {
            val tint = if (selected) +themeColor { primary } else Color.Gray
            Tab(onSelected = { onSelected() }) {
                Container(height = SmallTabHeight) {
                    HorizontalTabPadding {
                        TabImage(image, tint)
                    }
                }
            }
        }

        /**
         * A Tab that contains a title and an icon, and represents its selected state by tinting the
         * title and icon with [MaterialColors.primary].
         *
         * See the [Tab] overload that accepts any component if you want to display custom content
         * inside a tab.
         *
         * @param text the title displayed in this tab
         * @param image the icon displayed in this tab
         * @param selected whether this tab is selected or not
         * @param onSelected the callback to be invoked when this tab is selected
         */
        @Composable
        fun Tab(text: String, image: Image, selected: Boolean, onSelected: () -> Unit) {
            val buttonTextStyle = +themeTextStyle { button }
            val tint = if (selected) +themeColor { primary } else Color.Gray
            val textStyle = buttonTextStyle.copy(color = tint)
            Tab(onSelected = { onSelected() }) {
                Container(height = LargeTabHeight) {
                    HorizontalTabPadding {
                        VerticalTitleAndImageTabPadding {
                            FlexColumn {
                                expanded(1f) {
                                    Center {
                                        TabImage(image, tint)
                                    }
                                }
                                expanded(1f) {
                                    // TODO: This should be text baseline alignment
                                    Align(Alignment.BottomCenter) {
                                        Text(text = text, style = textStyle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun HorizontalTabPadding(children: @Composable() () -> Unit) {
            Padding(left = HorizontalTabPadding, right = HorizontalTabPadding) {
                children()
            }
        }

        @Composable
        private fun VerticalTitleAndImageTabPadding(children: @Composable() () -> Unit) {
            Padding(
                    top = TopTitleAndImageTabPadding,
                    bottom = BottomTitleAndImageTabPadding
            ) {
                children()
            }
        }

        // TODO: Specs for image padding
        @Composable
        private fun TabImage(image: Image, tint: Color) {
            Container(width = 24.dp, height = 24.dp) {
                SimpleImage(image, tint)
            }
        }

        private val HorizontalTabPadding = 16.dp
        private val TopTitleAndImageTabPadding = 12.dp
        private val BottomTitleAndImageTabPadding = 16.dp

        private val SmallTabHeight = 48.dp
        private val LargeTabHeight = 72.dp
    }
}
