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

package androidx.ui.core.selection

import androidx.compose.Ambient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.px
import androidx.ui.engine.geometry.Rect

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
            result = handler.getSelection(Pair(position, position), containerLayoutCoordinates!!)
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
                    result = handler.getSelection(
                        Pair(currentStart, currentEnd),
                        containerLayoutCoordinates!!)
                }
                onSelectionChange(result)
                return dragDistance
            }
        }
    }
}

/** Ambient of SelectionRegistrar for SelectionManager. */
val SelectionRegistrarAmbient = Ambient.of<SelectionRegistrar> { SelectionManager() }
