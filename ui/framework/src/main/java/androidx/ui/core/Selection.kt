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

import android.util.Log
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver

private const val HANDLE_WIDTH = 100f
private const val HANDLE_HEIGHT = 100f

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * The coordinate of the start offset of the selection. For text, it's the bounding box
     * of the character at the start offset.
     */
    val startOffset: Rect,
    /**
     * The coordinate of the end offset of the selection. For text, it's the bounding box
     * of the character at the end offset.
     */
    val endOffset: Rect
) {
    var startCurrent = PxPosition.Origin
    var endCurrent = PxPosition.Origin
    var startOrigin = PxPosition.Origin
    var endOrigin = PxPosition.Origin

    init {
        startCurrent = PxPosition(
            startOffset.left.px + ((startOffset.right - startOffset.left) / 4).px,
            startOffset.top.px + ((startOffset.bottom - startOffset.top) / 4).px
        )
        endCurrent = PxPosition(
            endOffset.left.px + ((endOffset.right - endOffset.left) / 4).px,
            endOffset.top.px + ((endOffset.bottom - endOffset.top) / 4).px
        )
        startOrigin = PxPosition(
            startOffset.left.px,
            startOffset.top.px + ((startOffset.bottom - startOffset.top) / 2).px
        )
        endOrigin = PxPosition(
            endOffset.left.px,
            endOffset.top.px + ((endOffset.bottom - endOffset.top) / 2).px
        )
    }
}

/**
 * An interface handling selection. Get selection from a widget by passing in a coordinate.
 */
interface TextSelectionHandler {
    fun getSelection(coordinates: Pair<PxPosition, PxPosition>): Selection?
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
            result = handler.getSelection(Pair(position, position))
        }
        onSelectionChange(result)
    }

    var oldStartOrigin = PxPosition.Origin
    var oldEndOrigin = PxPosition.Origin
    var dragStartDist = PxPosition.Origin
    var dragEndDist = PxPosition.Origin

    val startHandleDragObserver = object : DragObserver {

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            // Get the positions of start and end handles.
            var result = selection

            var startOrigin = PxPosition.Origin
            var endCurrent = PxPosition.Origin

            selection?.let {
                startOrigin = it.startOrigin
                endCurrent = it.endCurrent
            }

            Log.e(
                "SelectionDrag",
                "\n\ndragDistance = (" + dragDistance.x.value.toString() + " ," +
                        dragDistance.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "startOffset = (top = " + selection!!.startOffset.top.toString() +
                        ", bottom = " + selection!!.startOffset.bottom.toString() + ", left = " +
                        selection!!.startOffset.left.toString() + ", right = " +
                        selection!!.startOffset.right.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "endOffset = (top = " + selection!!.endOffset.top.toString() + ", bottom = " +
                        selection!!.endOffset.bottom.toString() + ", left = " +
                        selection!!.endOffset.left.toString() + ", right = " +
                        selection!!.endOffset.right.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "startCenter = (" + startOrigin.x.value.toString() + ", " +
                        startOrigin.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "endCenter = (" + endCurrent.x.value.toString() + ", " +
                        endCurrent.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "dragStartDist = (" + dragStartDist.x.value.toString() + ", " +
                        dragStartDist.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "oldStartCenter = (" + oldStartOrigin.x.value.toString() + ", " +
                        oldStartOrigin.y.value.toString() + ")"
            )

            if (oldStartOrigin != startOrigin) {
                dragStartDist = PxPosition.Origin
                oldStartOrigin = startOrigin
            }

            Log.e(
                "SelectionDrag",
                "updated dragStartDist = (" + dragStartDist.x.value.toString() + ", " +
                        dragStartDist.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "updated oldStartCenter = (" + oldStartOrigin.x.value.toString() + ", " +
                        oldStartOrigin.y.value.toString() + ")"
            )

            dragStartDist += dragDistance
            Log.e(
                "SelectionDrag",
                "dragged dragStartDist = (" + dragStartDist.x.value.toString() + ", " +
                        dragStartDist.y.value.toString() + ")"
            )

            // Change the selection.
            for (handler in handlers) {
                result = handler.getSelection(Pair(startOrigin + dragStartDist, endCurrent))
            }
            onSelectionChange(result)

            return dragDistance
        }
    }

    val endHandleDragObserver = object : DragObserver {

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            // Get the positions of start and end handles.
            var result = selection
            var startCurrent = PxPosition.Origin
            var endOrigin = PxPosition.Origin

            selection?.let {
                startCurrent = it.startCurrent
                endOrigin = it.endOrigin
            }

            Log.e(
                "SelectionDrag",
                "\n\ndragDistance = (" + dragDistance.x.value.toString() + " ," +
                        dragDistance.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "startOffset = (top = " + selection!!.startOffset.top.toString() +
                        ", bottom = " + selection!!.startOffset.bottom.toString() + ", left = " +
                        selection!!.startOffset.left.toString() + ", right = " +
                        selection!!.startOffset.right.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "endOffset = (top = " + selection!!.endOffset.top.toString() +
                        ", bottom = " + selection!!.endOffset.bottom.toString() + ", left = " +
                        selection!!.endOffset.left.toString() + ", right = " +
                        selection!!.endOffset.right.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "startCenter = (" + startCurrent.x.value.toString() + ", " +
                        startCurrent.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "endCenter = (" + endOrigin.x.value.toString() + ", " +
                        endOrigin.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "dragEndDist = (" + dragEndDist.x.value.toString() + ", " +
                        dragEndDist.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "oldEndCenter = (" + oldEndOrigin.x.value.toString() + ", " +
                        oldEndOrigin.y.value.toString() + ")"
            )

            if (oldEndOrigin != endOrigin) {
                dragEndDist = PxPosition.Origin
                oldEndOrigin = endOrigin
            }

            Log.e(
                "SelectionDrag",
                "updated dragEndDist = (" + dragEndDist.x.value.toString() + ", " +
                        dragEndDist.y.value.toString() + ")"
            )
            Log.e(
                "SelectionDrag",
                "updated oldEndCenter = (" + oldEndOrigin.x.value.toString() + ", " +
                        oldEndOrigin.y.value.toString() + ")"
            )

            dragEndDist += dragDistance
            Log.e(
                "SelectionDrag",
                "dragged dragEndDist = (" + dragEndDist.x.value.toString() + ", " +
                        dragEndDist.y.value.toString() + ")"
            )

            // Change the selection.
            for (handler in handlers) {
                result = handler.getSelection(Pair(startCurrent, endOrigin + dragEndDist))
            }
            onSelectionChange(result)

            return dragDistance
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
@Suppress("FunctionName")
@Composable
fun SelectionContainer(
    /** Current Selection status.*/
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    +memo(selection) { manager.selection = selection }
    +memo(onSelectionChange) { manager.onSelectionChange = onSelectionChange }

    SelectionRegistrarAmbient.Provider(value = manager) {
        val content = @Composable {
            PressIndicatorGestureDetector(onStart = { position -> manager.onPress(position) }) {
                children()
            }
        }
        val startHandle = @Composable {
            DragGestureDetector(
                canDrag = { true },
                dragObserver = manager.startHandleDragObserver
            ) {
                Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                })
            }
        }
        val endHandle = @Composable {
            DragGestureDetector(canDrag = { true }, dragObserver = manager.endHandleDragObserver) {
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
                            HANDLE_WIDTH.toInt().ipx,
                            HANDLE_HEIGHT.toInt().ipx
                        )
                    )
                val end =
                    measurables[endHandle as () -> Unit].first().measure(
                        Constraints.tightConstraints(
                            HANDLE_WIDTH.toInt().ipx,
                            HANDLE_HEIGHT.toInt().ipx
                        )
                    )
                layout(width, height) {
                    placeable.place(IntPx.Zero, IntPx.Zero)
                    selection?.let {
                        start.place(
                            it.startOffset.left.px - HANDLE_WIDTH.px - 20.ipx,
                            it.startOffset.bottom.px - 20.ipx
                        )
                        end.place(
                            it.endOffset.right.px + 20.ipx,
                            it.endOffset.bottom.px - 20.ipx
                        )
                    }
                }
            })
    }
}

@Suppress("FunctionName")
@Composable
internal fun SelectionHandle() {
    val paint = Paint()
    paint.color = Color(0xAAD94633.toInt())
    Draw { canvas, _ ->
        canvas.drawRect(
            Rect(left = 0f, top = 0f, right = HANDLE_WIDTH, bottom = HANDLE_HEIGHT),
            paint
        )
    }
}
