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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle

/**
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but aren’t the primary action
 * in an app.
 *
 * Outlined buttons are also a lower emphasis alternative to contained buttons, or a higher emphasis alternative to text
 * buttons.
 *
 * @sample androidx.ui.material.samples.OutlinedButtonSample
 *
 * To modify the properties of the text within the button or to specify arbitrary content within your Button, use the
 * version of this component that takes content:
 *
 * @sample androidx.ui.material.samples.SlottedOutlinedButtonSample
 *
 * @see ContainedButton
 * @see TextButton
 *
 */
@Composable
fun OutlinedButton(
    text: String,
    onClick: (() -> Unit)? = null) {
    OutlinedButton(
        text = {
            Text(text = text)
        },
        onClick = onClick
    )
}

/**
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but aren’t the primary action
 * in an app.
 *
 * Outlined buttons are also a lower emphasis alternative to contained buttons, or a higher emphasis alternative to text
 * buttons.
 *
 * To specify spec compliant content within your Outlined Button and have it follow Material guidelines, use the other
 * overload for [OutlinedButton].
 *
 * @see ContainedButton
 * @see TextButton
 *
 * @sample androidx.ui.material.samples.SlottedOutlinedButtonSample
 */
@Composable
fun OutlinedButton(
    text: @Composable() () -> Unit,
    onClick: (() -> Unit)? = null) {
    Button(
        children = {
            val constraints = DpConstraints
                .tightConstraintsForHeight(ButtonHeight)
                .copy(minWidth = ButtonMinWidth)
            Container(
                padding = EdgeInsets(left = ButtonHorPadding, right = ButtonHorPadding),
                constraints = constraints) {
                text()
            }
        },
        onClick = onClick,
        elevation = 0.dp,
        // TODO: figure out the default border color
        border = Border(Color(0xFF888888.toInt()), 1.dp),
        color = Color.Transparent)
}


/**
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They contain actions that are
 * primary to your app.
 *
 * To specify arbitrary content within your Contained Button, use the other overload for [ContainedButton].
 *
 * @see OutlinedButton
 * @see TextButton
 *
 * @sample androidx.ui.material.samples.ContainedButtonSample
 */
@Composable
fun ContainedButton(
    text: String,
    color: Color = +themeColor { primary },
    onClick: (() -> Unit)? = null) {
    ContainedButton(
        text = {
            Text(text = text)
        },
        color = color,
        onClick = onClick
    )
}

/**
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They contain actions that are
 * primary to your app.
 *
 * To specify spec compliant content within your Contained Button and have it follow Material guidelines, use the other
 * overload for [ContainedButton].
 *
 * @see OutlinedButton
 * @see TextButton
 *
 * @sample androidx.ui.material.samples.SlottedOutlinedButtonSample
 */
@Composable
fun ContainedButton(
    text: @Composable() () -> Unit,
    color: Color = +themeColor { primary },
    onClick: (() -> Unit)? = null) {
    Button(
        children = {
            val constraints = DpConstraints
                .tightConstraintsForHeight(ButtonHeight)
                .copy(minWidth = ButtonMinWidth)
            Container(
                padding = EdgeInsets(left = ButtonHorPadding, right = ButtonHorPadding),
                constraints = constraints) {
                text()
            }
        },
        onClick = onClick,
        // TODO: might have elevation
        elevation = 0.dp,
        color = color)
}


/**
 * Text buttons are typically used for less-pronounced actions, including those located in cards and dialogs.
 *
 * To specify arbitrary content within your Text Button, use the other overload for [TextButton].
 *
 * @see OutlinedButton
 * @see ContainedButton
 *
 * @sample androidx.ui.material.samples.TextButtonSample
 */
@Composable
fun TextButton(
    text: String,
    onClick: (() -> Unit)? = null) {
    TextButton(
        text = {
            Text(text = text)
        },
        onClick = onClick
    )
}

/**
 * Text buttons are typically used for less-pronounced actions, including those located in cards and dialogs.
 *
 * To specify spec compliant content within your Text Button and have it follow Material guidelines, use the other
 * overload for [TextButton].
 *
 * @see OutlinedButton
 * @see ContainedButton
 *
 * @sample androidx.ui.material.samples.SlottedTextButtonSample
 */
@Composable
fun TextButton(
    text: @Composable() () -> Unit,
    onClick: (() -> Unit)? = null) {
    Button(
        children = {
            val constraints = DpConstraints
                .tightConstraintsForHeight(ButtonHeight)
                .copy(minWidth = ButtonMinWidth)
            Container(
                padding = EdgeInsets(left = ButtonHorPadding, right = ButtonHorPadding),
                constraints = constraints) {
                text()
            }
        },
        onClick = onClick,
        // TODO: might have elevation
        elevation = 0.dp,
        color = Color.Transparent)
}

/**
 * [Button] with flexible user interface. You can provide any content you want as a
 * [children] composable.
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 * You can specify a [shape] of the surface, it's background [color] and an [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 * To modify these default style values use [CurrentTextStyleProvider].
 *
 * Example:
 *     Button(onClick = { ... }) {
 *         Padding(padding = EdgeInsets(16.dp)) {
 *             Text(text=TextSpan(text="CUSTOM BUTTON"))
 *         }
 *     }
 *
 * @see Button overload for the default Material Design implementation of [Button] with text.
 *
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] value from the theme.
 * @param color The background color. [MaterialColors.primary] is used when null
 *  is provided. Provide [Color.Transparent] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
// TODO: rename to BaseButton
@Composable
fun Button(
    onClick: (() -> Unit)? = null,
    shape: Shape = +themeShape { button },
    color: Color = +themeColor { primary },
    border: Border? = null,
    elevation: Dp = 0.dp,
    @Children children: @Composable() () -> Unit
) {
    val textStyle = +themeTextStyle { button }
    Surface(shape = shape, color = color, border = border, elevation = elevation) {
        CurrentTextStyleProvider(value = textStyle) {
            val clickableChildren = @Composable {
                Clickable(onClick = onClick) {
                    children()
                }
            }
            if (onClick != null) {
                Ripple(bounded = true) {
                    clickableChildren()
                }
            } else {
                clickableChildren()
            }
        }
    }
}

/**
 * Material Design implementation of [Button] with [text].
 *
 * To make a [Button] clickable, you must provide an [onClick]. Not providing it will
 * also make this [Button] to be displayed as a disabled one.
 * You can specify a [shape] of the surface, it's background [color] and [elevation].
 *
 * The text style for internal [Text] components will be changed to [MaterialTypography.button],
 * text color will try to match the correlated color for the background [color]. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used for text.
 *
 * Example:
 *     Button(
 *         onClick = { ... },
 *         text="TEXT"))
 *
 * @see Button for the flexible implementation with a customizable content.
 * @see TransparentButton for the version with no background.
 *
 * @param text The text to display.
 * @param textStyle The optional text style to apply for the text.
 * @param onClick Will be called when user clicked on the button. The button will be disabled
 *  when it is null.
 * @param shape Defines the Button's shape as well its shadow. When null is provided it uses
 *  the [Shapes.button] value from the theme.
 * @param color The background color. [MaterialColors.primary] is used when null
 *  is provided. Use [TransparentButton] to have no color.
 * @param border Optional border to draw on top of the shape.
 * @param elevation The z-coordinate at which to place this button. This controls the size
 *  of the shadow below the button.
 */
// TODO: remove
@Composable
fun Button(
    text: String,
    textStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = +themeShape { button },
    color: Color = +themeColor { primary },
    border: Border? = null,
    elevation: Dp = 0.dp
) {
    val hasBackground = color.alpha > 0 || border != null
    val horPaddings = if (hasBackground) ButtonHorPadding else ButtonHorPaddingNoBg
    Button(
        onClick = onClick,
        elevation = elevation,
        color = color,
        border = border,
        shape = shape
    ) {
        val constraints = DpConstraints
            .tightConstraintsForHeight(ButtonHeight)
            .copy(minWidth = ButtonMinWidth)
        Container(
            padding = EdgeInsets(left = horPaddings, right = horPaddings),
            constraints = constraints) {
            Text(text = text, style = textStyle)
        }
    }
}

// Specification for Material Button:
private val ButtonHeight = 36.dp
private val ButtonMinWidth = 64.dp
private val ButtonHorPadding = 16.dp
private val ButtonHorPaddingNoBg = 8.dp
