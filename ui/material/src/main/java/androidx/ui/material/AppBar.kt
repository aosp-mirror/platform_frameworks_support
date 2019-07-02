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

import android.util.Log
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.ColoredRect
import androidx.ui.baseui.SimpleImage
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Semantics
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.sp
import androidx.ui.layout.Container
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.surface.Surface
import androidx.ui.graphics.Color
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.MainAxisSize
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Image
import androidx.ui.painting.TextStyle

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 * This version of the TopAppBar will produce a default bar with a navigation leading icon, an
 * optional title and a set of menu icons.
 *
 * Example usage:
 *     TopAppBar(
 *         title = "Title",
 *         color = +themeColor{ secondary }
 *     )
 *
 * @param title An optional title to display
 * @param color An optional color for the App Bar. By default [MaterialColors.primary] will be used.
 * @param actions An optional list of actions to display on the App Bar.
 */
@Composable
fun TopAppBar(
    title: String? = null,
    color: Color = +themeColor { primary },
    // TODO: maybe we just expose onClick here, and provide the default navigation icon?
    navigationAction: AppBarAction? = null,
    actions: List<AppBarAction> = emptyList()
) {
    TopAppBar(
        color = color,
        leadingIcon = {
            navigationAction?.let { AppBarIcon(it) }
        },
        titleTextLabel = {
           title?.let { TopAppBarTitleTextLabel(it) }
        },
        trailingIcons = { AppBarActions(MaxIconsInTopAppBar, actions) }
    )
}

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 *
 * Example usage:
 *     TopAppBar(
 *         color = +themeColor{ secondary },
 *         leadingIcon = { MyNavIcon() },
 *         titleTextLabel = { Text(text = "Title") },
 *         trailingIcons = { AppBarActions(icons) }
 *     )
 *
 * @param color An optional color for the App Bar. By default [MaterialColors.primary] will be used.
 * @param leadingIcon A composable lambda to be inserted in the Leading Icon space. This is usually
 * a navigation icon. A standard implementation is provided by [AppBarLeadingIcon].
 * @param titleTextLabel A composable lambda to be inserted in the title space. This is usually a
 * [Text] element. A standard implementation is provided by [TopAppBarTitleTextLabel]. Default text
 * styling [MaterialTypography.h6] will be used.
 * @param trailingIcons A composable lambda to be inserted at the end of the bar, usually containing
 * a collection of menu icons. A standard implementation is provided by [AppBarActions].
 */
@Composable
fun TopAppBar(
    color: Color = +themeColor { primary },
    leadingIcon: @Composable() () -> Unit,
    titleTextLabel: @Composable() () -> Unit,
    trailingIcons: @Composable() () -> Unit
) {
    AppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                // TODO: what should the spacing be when there is no icon provided here?
                leadingIcon()
                WidthSpacer(width = 32.dp)
            }
            expanded(flex = 1f) {
                CurrentTextStyleProvider(value = +themeTextStyle { h6 }) {
                    titleTextLabel()
                }
            }
            inflexible {
                trailingIcons()
            }
        }
    }
}

/**
 * An empty App Bar that expands to the parent's width.
 *
 * For an App Bar that follows Material spec guidelines to be placed on the top of the screen, see
 * [TopAppBar].
 */
@Composable
fun AppBar(color: Color, @Children children: @Composable() () -> Unit) {
    Semantics(
        container = true
    ) {
        Surface(color = color) {
            Container(height = RegularHeight, expanded = true, padding = EdgeInsets(Padding)) {
                children()
            }
        }
    }
}

@Composable
fun BottomAppBar(
    color: Color = +themeColor { primary },
    // TODO: maybe we just expose onClick here, and provide the default navigation icon?
    navigationAction: AppBarAction? = null,
    actions: List<AppBarAction> = emptyList()
) {
    BottomAppBar(
        color = color,
        leadingIcon = {
            navigationAction?.let { AppBarIcon(it) }
        },
        trailingIcons = { AppBarActions(MaxIconsInBottomAppBar, actions) }
    )
}

// TODO: support a FAB and its alignment
@Composable
fun BottomAppBar(
    color: Color = +themeColor { primary },
    leadingIcon: @Composable() () -> Unit,
    trailingIcons: @Composable() () -> Unit
) {
    AppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                leadingIcon()
                WidthSpacer(width = 32.dp)
            }
            inflexible { trailingIcons() }
        }
    }
}

/**
 * A component that displays a title as a [Text] element for placement within a Top App Bar
 * following Material spec guidelines.
 *
 * @see [TopAppBar]
 *
 * @param title A title String to display
 */
@Composable
fun TopAppBarTitleTextLabel(title: String) {
    Text(text = title)
}

/**
 * A component that displays a set of menu icons for placement within a Top App Bar following
 * Material spec guidelines.
 *
 * @see [TopAppBar]
 *
 * @param icons A list of icons to display
 */
@Composable
private fun AppBarActions(actionsToDisplay: Int, actions: List<AppBarAction>) {
    // Split the list depending on how many actions we are displaying - if actionsToDisplay is
    // greater than or equal to the number of actions provided, menuActions will be empty.
    val (iconActions, menuActions) = actions.withIndex().partition {
        it.index < actionsToDisplay
    }

    Row {
        iconActions.forEach { (index, action) ->
            AppBarIcon(action)
            if (index != iconActions.lastIndex) {
                WidthSpacer(width = 24.dp)
            }
        }
        if (menuActions.isNotEmpty()) {
            WidthSpacer(width = 24.dp)
            // TODO: use menuActions to build menu here
            Container(width = 12.dp) {
                Text(text = "${menuActions.size}", style = TextStyle(fontSize = 15.sp))
            }
        }
    }
}

@Composable
private fun AppBarIcon(action: AppBarAction) {
    Ripple(bounded = true) {
        Clickable(onClick = action.onClick) {
            Container(width = ActionIconDiameter, height = ActionIconDiameter) {
                SimpleImage(action.icon)
            }
        }
    }
}

data class AppBarAction(val icon: Image, val text: String, val onClick: () -> Unit)

private val ActionIconDiameter = 24.dp

private val RegularHeight = 56.dp
private val Padding = 16.dp
// TODO: b/123936606 (IR bug) prevents using constants in a closure
private val MaxIconsInTopAppBar = 2
private val MaxIconsInBottomAppBar = 4