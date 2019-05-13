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
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.engine.geometry.Rect

private val HANDLE_WIDTH = 100.px
private val HANDLE_HEIGHT = 100.px

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * The bounding box of the character at the start offset.
     */
    val startOffset: Rect,
    /**
     * The bounding box of the character at the end offset.
     */
    val endOffset: Rect,
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
        containerLayoutCoordinates: LayoutCoordinates
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
            result += handler.getSelection(Pair(position, position), containerLayoutCoordinates!!)
        }
        onSelectionChange(result)
    }

    var dragBeginPosition = PxPosition.Origin
    var dragTotalDistance = PxPosition.Origin

    // Get the coordinates of a character. Currently, it's the middle point of the left edge of the
    // bounding box of the character. It's based on Android Text's calculation.
    fun getCoordinatesForCharacter(box: Rect): PxPosition {
        return PxPosition(box.left.px, box.top.px + (box.bottom.px - box.top.px) / 2)
    }

    fun handleDragObserver(dragStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart() {
                val beginLayoutCoordinates =
                    if (dragStartHandle) {
                        selection?.startLayoutCoordinates!!
                    } else {
                        selection?.endLayoutCoordinates!!
                    }
                val beginCoordinates =
                    getCoordinatesForCharacter(
                        if (dragStartHandle) {
                            selection!!.startOffset
                        } else {
                            selection!!.endOffset
                        }
                    )

                dragBeginPosition = containerLayoutCoordinates!!.childToLocal(
                    beginLayoutCoordinates,
                    beginCoordinates
                )
                dragTotalDistance = PxPosition.Origin
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                var result = selection
                dragTotalDistance += dragDistance

                val currentStart = if (dragStartHandle) {
                    dragBeginPosition + dragTotalDistance
                } else {
                    containerLayoutCoordinates!!.childToLocal(
                        selection?.startLayoutCoordinates!!,
                        getCoordinatesForCharacter(selection!!.startOffset)
                    )
                }

                val currentEnd = if (dragStartHandle) {
                    containerLayoutCoordinates!!.childToLocal(
                        selection?.endLayoutCoordinates!!,
                        getCoordinatesForCharacter(selection!!.endOffset)
                    )
                } else {
                    dragBeginPosition + dragTotalDistance
                }

                for (handler in handlers) {
                    result += handler.getSelection(
                        Pair(currentStart, currentEnd),
                        containerLayoutCoordinates!!
                    )
                }
                onSelectionChange(result)
                return dragDistance
            }
        }
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
    @Children children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    // TODO (qqd): After selection widget is fully implemented, evaluate if the following 2 items
    // are expensive. If so, use
    // +memo(selection) { manager.selection = selection }
    // +memo(onSelectionChange) { manager.onSelectionChange = onSelectionChange }
    manager.selection = selection
    manager.onSelectionChange = onSelectionChange

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
            DragGestureDetector(
                canDrag = { true },
                dragObserver = manager.handleDragObserver(dragStartHandle = true)
            ) {
                Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                })
            }
        }
        val endHandle = @Composable {
            DragGestureDetector(
                canDrag = { true },
                dragObserver = manager.handleDragObserver(dragStartHandle = false)
            ) {
                Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                })
            }
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
                            PxPosition(selection.startOffset.left.px,
                                selection.startOffset.bottom.px)
                        )
                        val endOffset = manager.containerLayoutCoordinates!!.childToLocal(
                            selection.endLayoutCoordinates,
                            PxPosition(selection.endOffset.right.px,
                                selection.endOffset.bottom.px)
                        )
                        start.place(startOffset.x - HANDLE_WIDTH, startOffset.y - HANDLE_HEIGHT)
                        end.place(endOffset.x, endOffset.y - HANDLE_HEIGHT)
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
