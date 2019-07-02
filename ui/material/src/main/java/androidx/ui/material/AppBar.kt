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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.SimpleImage
import androidx.ui.core.CurrentTextStyleProvider
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
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Stack
import androidx.ui.material.FloatingActionButtonPosition.CENTER
import androidx.ui.material.FloatingActionButtonPosition.END
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Image
import androidx.ui.painting.TextStyle

@Composable
fun TopAppBar(
    title: @Composable() () -> Unit = {},
    color: Color = +themeColor { primary },
    navigationIcon: @Composable() () -> Unit = {}
) {
    TopAppBar(
        title = title,
        color = color,
        navigationIcon = navigationIcon,
        actionItems = emptyList<Any>()
    )
}

@Composable
fun <T> TopAppBar(
    title: @Composable() () -> Unit = {},
    color: Color = +themeColor { primary },
    navigationIcon: @Composable() () -> Unit = {},
    actionItems: List<T>,
    action: @Composable() (T) -> Unit = {}
) {
    BaseTopAppBar(
        color = color,
        startContent = navigationIcon,
        title = title,
        endContent = { AppBarActions(MaxIconsInTopAppBar, actionItems, action) }
    )
}

/**
 * A Top App Bar displays information and actions relating to the current screen and is placed at
 * the top of the screen.
 *
 * Example usage:
 *     TopAppBar(
 *         color = +themeColor{ secondary },
 *         startContent = { MyNavIcon() },
 *         titleTextLabel = { Text(text = "Title") },
 *         endContent = { AppBarActions(icons) }
 *     )
 *
 * @param color An optional color for the App Bar. By default [MaterialColors.primary] will be used.
 * @param startContent A composable lambda to be inserted in the Leading Icon space. This is usually
 * a navigation icon. A standard implementation is provided by [AppBarstartContent].
 * @param titleTextLabel A composable lambda to be inserted in the title space. This is usually a
 * [Text] element. A standard implementation is provided by [TopAppBarTitleTextLabel]. Default text
 * styling [MaterialTypography.h6] will be used.
 * @param endContent A composable lambda to be inserted at the end of the bar, usually containing
 * a collection of menu icons. A standard implementation is provided by [AppBarActions].
 */
@Composable
private fun BaseTopAppBar(
    color: Color = +themeColor { primary },
    startContent: @Composable() () -> Unit,
    title: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                // TODO: what should the spacing be when there is no icon provided here?
                startContent()
                WidthSpacer(width = 32.dp)
            }
            expanded(1f) {
                CurrentTextStyleProvider(value = +themeTextStyle { h6 }) {
                    title()
                }
            }
            inflexible {
                endContent()
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
private fun BaseAppBar(color: Color, children: @Composable() () -> Unit) {
    Semantics(
        container = true
    ) {
        Surface(color = color) {
            Container(height = AppBarHeight, expanded = true, padding = EdgeInsets(AppBarPadding)) {
                children()
            }
        }
    }
}

enum class FloatingActionButtonPosition {
    CENTER,
    END,
    CENTER_CUT,
}

// TODO: type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
fun BottomAppBar(
    color: Color = +themeColor { primary },
    navigationIcon: @Composable() () -> Unit = {},
    floatingActionButton: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    floatingActionButtonPosition: FloatingActionButtonPosition = CENTER
) {
    BottomAppBar(
        color = color,
        navigationIcon = navigationIcon,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        actionItems = emptyList<Any>()
    )
}

// TODO: type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
fun <T> BottomAppBar(
    color: Color = +themeColor { primary },
    navigationIcon: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    floatingActionButton: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    floatingActionButtonPosition: FloatingActionButtonPosition = CENTER,
    actionItems: List<T>,
    action: @Composable() (T) -> Unit = {}
) {
    require(navigationIcon == null || floatingActionButtonPosition != END) {
        "Using a navigation icon with an end-aligned FloatingActionButton is not supported"
    }

    val actions = { maxIcons: Int -> @Composable { AppBarActions(maxIcons, actionItems, action) } }
    val navigationIconComposable = @Composable {
        if (navigationIcon != null) {
            navigationIcon()
        }
    }

    if (floatingActionButton == null) {
        BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            endContent = actions(MaxIconsInBottomAppBarNoFab)
        )
        return
    }

    when (floatingActionButtonPosition) {
        END -> BaseBottomAppBar(
            color = color,
            startContent = { actions(MaxIconsInBottomAppBarEndFab) },
            fab = { Align(Alignment.CenterRight) { floatingActionButton() } }
        )
        // TODO: support CENTER_CUT
        else -> BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            fab = { Center { floatingActionButton() } },
            endContent = { actions(MaxIconsInBottomAppBarCenterFab) }
        )
    }
}

// TODO: type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
private fun BaseBottomAppBar(
    color: Color = +themeColor { primary },
    startContent: @Composable() () -> Unit = {},
    fab: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    endContent: @Composable() () -> Unit = {}
) {
    val appBar = @Composable { BaseBottomAppBarWithoutFab(color, startContent, endContent) }
    if (fab == null) {
        appBar()
    } else {
        ConstrainedBox(
            constraints = DpConstraints(
                minHeight = BottomAppBarHeightWithFab,
                maxHeight = BottomAppBarHeightWithFab
            )
        ) {
            Stack {
                aligned(Alignment.BottomCenter) {
                    appBar()
                }
                aligned(Alignment.TopCenter) {
                    Container(
                        height = AppBarHeight,
                        padding = EdgeInsets(left = AppBarPadding, right = AppBarPadding)
                    ) {
                        fab()
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseBottomAppBarWithoutFab(
    color: Color,
    startContent: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                startContent()
                // TODO: if startContent() doesn't have any layout, then the endContent won't be
                // placed at the end, so we need to trick it with a spacer
                WidthSpacer(width = 1.dp)
            }
            inflexible { endContent() }
        }
    }
}

@Composable
private fun <T> AppBarActions(
    actionsToDisplay: Int,
    actionItems: List<T>,
    action: @Composable() (T) -> Unit
) {
    if (actionItems.isEmpty()) {
        return
    }

    // Split the list depending on how many actions we are displaying - if actionsToDisplay is
    // greater than or equal to the number of actions provided, overflowActions will be empty.
    val (shownActions, overflowActions) = actionItems.withIndex().partition {
        it.index < actionsToDisplay
    }

    Row {
        shownActions.forEach { (index, shownAction) ->
            action(shownAction)
            if (index != shownActions.lastIndex) {
                WidthSpacer(width = 24.dp)
            }
        }
        if (overflowActions.isNotEmpty()) {
            WidthSpacer(width = 24.dp)
            // TODO: use overflowActions to build menu here
            Container(width = 12.dp) {
                Text(text = "${overflowActions.size}", style = TextStyle(fontSize = 15.sp))
            }
        }
    }
}

@Composable
fun AppBarIcon(icon: Image, onClick: () -> Unit) {
    Ripple(bounded = false) {
        Clickable(onClick = onClick) {
            Center {
                Container(width = ActionIconDiameter, height = ActionIconDiameter) {
                    SimpleImage(icon)
                }
            }
        }
    }
}

private val ActionIconDiameter = 24.dp

private val AppBarHeight = 56.dp
private val BottomAppBarHeightWithFab = 84.dp
private val AppBarPadding = 16.dp

private const val MaxIconsInTopAppBar = 2
private const val MaxIconsInBottomAppBarCenterFab = 2
private const val MaxIconsInBottomAppBarEndFab = 4
private const val MaxIconsInBottomAppBarNoFab = 4