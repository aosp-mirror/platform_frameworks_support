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

import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.engine.geometry.Rect

private val HANDLE_WIDTH = 20.px
private val HANDLE_HEIGHT = 100.px

/**
 * The enum class allows user to decide the selection mode.
 */
enum class SelectionMode {
    /**
     * When selection handles are dragged across widgets, selection extends by row, for example,
     * when the end selection handle is dragged down, upper rows will be selected first, and the
     * lower rows.
     */
    Vertical {
        override fun isSelected(
            box: Rect,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            // When the end of the selection is above the top of the widget, the widget is outside
            // of the selection range.
            if (end.y < box.top.px) return false

            // When the end of the selection is on the left of the widget, and not below the bottom
            // of widget, the widget is outside of the selection range.
            if (end.x < box.left.px && end.y <= box.bottom.px) return false

            // When the start of the selection is below the bottom of the widget, the widget is
            // outside of the selection range.
            if (start.y > box.bottom.px) return false

            // When the start of the selection is on the right of the widget, and not above the top
            // of the widget, the widget is outside of the selection range.
            if (start.x > box.right.px && start.y >= box.top.px) return false

            return true
        }
    },

    /**
     * When selection handles are dragged across widgets, selection extends by column, for example,
     * when the end selection handle is dragged to the right, left columns will be selected first,
     * and the right rows.
     */
    Horizontal {
        override fun isSelected(
            box: Rect,
            start: PxPosition,
            end: PxPosition
        ): Boolean {
            // When the end of the selection is on the left of the widget, the widget is outside of
            // the selection range.
            if (end.x < box.left.px) return false

            // When the end of the selection is on the top of the widget, and the not on the right
            // of the widget, the widget is outside of the selection range.
            if (end.y < box.top.px && end.x <= box.right.px) return false

            // When the start of the selection is on the right of the widget, the widget is outside
            // of the selection range.
            if (start.x > box.right.px) return false

            // When the start of the selection is below the widget, and not on the left of the
            // widget, the widget is outside of the selection range.
            if (start.y > box.bottom.px && start.x >= box.left.px) return false

            return true
        }
    };

    internal abstract fun isSelected(
        box: Rect,
        start: PxPosition,
        end: PxPosition
    ): Boolean
}

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * The coordinates of the start offset of the selection. For text, it's the left bottom corner
     * of the character at the start offset.
     */
    val startOffset: Offset,
    /**
     * The coordinates of the end offset of the selection. For text, it's the left bottom corner
     * of the character at the end offset.
     */
    val endOffset: Offset,
    /**
     * The layout coordinates of the child which contains the start of the selection. If the child
     * does not contain the start of the selection, this should be null.
     */
    val startLayoutCoordinates: LayoutCoordinates?,
    /**
     * The layout coordinates of the child which contains the end of the selection. If the child
     * does not contain the end of the selection, this should be null.
     */
    val endLayoutCoordinates: LayoutCoordinates?
) {
    fun merge(other: Selection): Selection {
        // TODO: combine two selections' contents with styles together.
        var currentSelection = this.copy()
        if (other.startLayoutCoordinates != null) {
            currentSelection = currentSelection.copy(
                startOffset = other.startOffset,
                startLayoutCoordinates = other.startLayoutCoordinates
            )
        }
        if (other.endLayoutCoordinates != null) {
            currentSelection = currentSelection.copy(
                endOffset = other.endOffset,
                endLayoutCoordinates = other.endLayoutCoordinates
            )
        }
        return currentSelection
    }
}
operator fun Selection?.plus(rhs: Selection?): Selection? {
    if (this == null) return rhs
    if (rhs == null) return this
    return merge(rhs)
}

/**
 * An interface handling selection. Get selection from a widget by passing in the start and end of
 * selection in a selection container as a pair, and the layout coordinates of the selection
 * container.
 */
interface TextSelectionHandler {
    fun getSelection(
        selectionCoordinates: Pair<PxPosition, PxPosition>,
        containerLayoutCoordinates: LayoutCoordinates,
        mode: SelectionMode
    ): Selection?
}

/**
 *  An interface allowing a Text composable to "register" and "unregister" itself with the class
 *  implementing the interface.
 */
interface SelectionRegistrar {
    // TODO(qqd): Replace Any with a type in future.
    fun subscribe(handler: TextSelectionHandler): Any

    fun unsubscribe(key: Any)
}

internal class SelectionManager : SelectionRegistrar {
    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    val handlers = mutableSetOf<TextSelectionHandler>()

    /**
     * Layout Coordinates of the selection container.
     */
    var containerLayoutCoordinates: LayoutCoordinates? = null

    var mode: SelectionMode = SelectionMode.Vertical

    /**
     * Allow a Text composable to "register" itself with the manager
     */
    override fun subscribe(handler: TextSelectionHandler): Any {
        handlers.add(handler)
        return handler
    }

    /**
     * Allow a Text composable to "unregister" itself with the manager
     */
    override fun unsubscribe(key: Any) {
        handlers.remove(key as TextSelectionHandler)
    }

    var selection: Selection? = null

    var onSelectionChange: (Selection?) -> Unit = {}

    fun onPress(position: PxPosition) {
        var result: Selection? = null
        for (handler in handlers) {
            result += handler.getSelection(
                Pair(position, position),
                containerLayoutCoordinates!!,
                mode)
        }
        onSelectionChange(result)
    }
}

/** Ambient of SelectionRegistrar for SelectionManager. */
val SelectionRegistrarAmbient = Ambient.of<SelectionRegistrar> { SelectionManager() }

/**
 * Selection Widget.
 *
 * The selection widget wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@Composable
fun SelectionContainer(
    /** Current Selection status.*/
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    mode: SelectionMode = SelectionMode.Vertical,
    @Children children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    // TODO (qqd): After selection widget is fully implemented, evaluate if the following 2 items
    // are expensive. If so, use
    // +memo(selection) { manager.selection = selection }
    // +memo(onSelectionChange) { manager.onSelectionChange = onSelectionChange }
    manager.selection = selection
    manager.onSelectionChange = onSelectionChange
    manager.mode = mode

    SelectionRegistrarAmbient.Provider(value = manager) {
        val content = @Composable {
            val content = @Composable() {
                // Get the layout coordinates of the selection container. This is for hit test of
                // cross-widget selection.
                OnPositioned(onPositioned = { manager.containerLayoutCoordinates = it })
                PressIndicatorGestureDetector(onStart = { position -> manager.onPress(position) }) {
                    children()
                }
            }
            Layout(children = content, layoutBlock = { measurables, constraints ->
                val placeable = measurables.firstOrNull()?.measure(constraints)
                val width = placeable?.width ?: constraints.minWidth
                val height = placeable?.height ?: constraints.minHeight
                layout(width, height) {
                    placeable?.place(0.ipx, 0.ipx)
                }
            })
        }
        val startHandle = @Composable {
            Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            })
        }
        val endHandle = @Composable {
            Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            })
        }
        @Suppress("USELESS_CAST")
        Layout(
            childrenArray = arrayOf(content, startHandle, endHandle),
            layoutBlock = { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                val width = placeable.width
                val height = placeable.height
                val start =
                    measurables[startHandle as () -> Unit].first().measure(
                        Constraints.tightConstraints(
                            HANDLE_WIDTH.round(),
                            HANDLE_HEIGHT.round()
                        )
                    )
                val end =
                    measurables[endHandle as () -> Unit].first().measure(
                        Constraints.tightConstraints(
                            HANDLE_WIDTH.round(),
                            HANDLE_HEIGHT.round()
                        )
                    )
                layout(width, height) {
                    placeable.place(IntPx.Zero, IntPx.Zero)
                    if (selection != null &&
                        selection.startLayoutCoordinates != null &&
                        selection.endLayoutCoordinates != null) {
                        val startOffset = manager.containerLayoutCoordinates!!.childToLocal(
                            selection.startLayoutCoordinates,
                            PxPosition(selection.startOffset.dx.px, selection.startOffset.dy.px)
                        )
                        val endOffset = manager.containerLayoutCoordinates!!.childToLocal(
                            selection.endLayoutCoordinates,
                            PxPosition(selection.endOffset.dx.px, selection.endOffset.dy.px)
                        )
                        start.place(startOffset.x, startOffset.y - HANDLE_HEIGHT)
                        end.place(endOffset.x - HANDLE_WIDTH, endOffset.y - HANDLE_HEIGHT)
                    }
                }
            })
    }
}

@Composable
internal fun SelectionHandle() {
    val paint = +memo { Paint() }
    paint.color = Color(0xAAD94633.toInt())
    Draw { canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    }
}
