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

package androidx.ui.core.pointerinput

import androidx.ui.core.ComponentNode
import androidx.ui.core.ConsumedData
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.PxPosition
import androidx.ui.core.Timestamp
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.toOffset
import androidx.ui.engine.geometry.Rect

/**
 * The core element that receives [PointerInputEvent]s and process them in Compose UI.
 */
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker()
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     */
    fun process(pointerEvent: PointerInputEvent) {

        // Gets a new PointerInputChangeEvent with the PointerInputEvent.
        val pointerInputChangeEvent = pointerInputChangeEventProducer.produce(pointerEvent)

        // Add new hit paths to the tracker due to down events.
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputNode> = mutableListOf()
            root.hitTest(
                it.current.position!!,
                Rect.largest,
                hitResult
            )
            hitPathTracker.addHitPath(it.id, hitResult.reversed())
        }

        // Remove PointerInputNodes that are no longer valid and refresh the offset information for
        // those that are.
        hitPathTracker.refreshPathInformation()

        // Dispatch the PointerInputChanges to the hit PointerInputNodes.
        var changes = pointerInputChangeEvent.changes
        hitPathTracker.apply {
            changes = dispatchChanges(changes, PointerEventPass.InitialDown, PointerEventPass.PreUp)
            changes = dispatchChanges(changes, PointerEventPass.PreDown, PointerEventPass.PostUp)
            dispatchChanges(changes, PointerEventPass.PostDown)
        }

        // Remove hit paths from the tracker due to up events.
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            hitPathTracker.removePointerId(it.id)
        }
    }

    // TODO(b/131780534): This method should return true if a hit was made to prevent
    // overlapping PointerInputNodes that are descendants of children of the ComponentNode from
    // both being successfully hit.
    /**
     * Searches for [PointerInputNode]s among the [ComponentNode]'s descendants, determines if the
     * [point] is within their virtual bounds, and adds them to [hitPointerInputNodes] if they are.
     *
     * This method actually just recursively searches for PointerInputNodes among the descendants
     * of the ComponentNode in a DFS in a reverse child order (so children that will be drawn on top
     * of their siblings will be checked first) and calls [hitTest] on them when found. If that
     * method returns true, it stops searching so that other PointerInputNodes that are drawn under
     * the hit PointerInputNode can't also be hit.
     */
    private fun ComponentNode.hitTest(
        point: PxPosition,
        maxBoundingBox: Rect,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ): HitTestBoundingBoxResult {

        val newMaxBoundingBox =
            if (this is LayoutNode) {
                val layoutNodeRect = Rect(
                    0f,
                    0f,
                    this.width.value.toFloat(),
                    this.height.value.toFloat()
                )
                val intersectingBoundingBox = maxBoundingBox.intersect(layoutNodeRect)
                // If the point is not inside the new max bounding box, it won't hit any of our
                // children so there is no point in looking any further.  Return early with
                // the same intersecting bounding box (which is our size intersected with the max
                // bounding box we were given).
                if (!intersectingBoundingBox.contains(point.toOffset())) {
                    return HitTestBoundingBoxResult(layoutNodeRect, false)
                } else {
                    intersectingBoundingBox
                }
            } else {
                maxBoundingBox
            }

        // In the below code, as soon as hitLeafPointerInputNode is true, we have determined that
        // we hit a leaf PointerInputNode, at which point we must be hitting every parent
        // PointerInputNode so we can quickly back track and end up with a complete
        // hitPointerInputNodes list.
        var hitDescendantPointerInputNode = false

        var overarchingBoundingBox: Rect? = null

        // TODO(shepshapard): This visitChildren use is ugly since once we successfully hit a child
        // we have to continue to loop through the rest of the children event though we don't
        // actually need to.  Figure out a better call here.
        visitChildrenReverse { child ->
            if (!hitDescendantPointerInputNode) {

                val result =
                    if (child is LayoutNode) {
                        // If the child is a LayoutNode, offset the point and bounding box to be
                        // relative to the LayoutNode's (0,0), and then when we get a result, offset
                        // back to our (0,0).
                        val resultRelativeToChild = child.hitTest(
                            PxPosition(point.x - child.x, point.y - child.y),
                            newMaxBoundingBox.translate(
                                -child.x.value.toFloat(),
                                -child.y.value.toFloat()
                            ),
                            hitPointerInputNodes
                        )
                        HitTestBoundingBoxResult(
                            resultRelativeToChild.boundingBox?.translate(
                                child.x.value.toFloat(),
                                child.y.value.toFloat()
                            ),
                            resultRelativeToChild.hit
                        )
                    } else {
                        child.hitTest(point, newMaxBoundingBox, hitPointerInputNodes)
                    }

                hitDescendantPointerInputNode = result.hit

                // If this is not a LayoutNode and we haven't hit a leaf PointerInputNode, then we
                // should build up the layout node bounding box that may be used for an ancestor
                // PointerInputNodes to test for hit testing.
                //
                // If we are a layout node, we will just return our size (intersected with the max
                // bounding box provided to us so we don't care about some other bounding box).
                //
                // If we have hit a leaf PointerInputNode, we will quickly back track and also hit
                // all ancestor PointerInputNodes.
                if (this !is LayoutNode && !hitDescendantPointerInputNode) {
                    overarchingBoundingBox =
                        if (overarchingBoundingBox != null && result.boundingBox != null) {
                            overarchingBoundingBox!!.expandToInclude(result.boundingBox)
                        } else {
                            if (overarchingBoundingBox != null) {
                                overarchingBoundingBox
                            } else {
                                result.boundingBox
                            }
                        }
                }
            }
        }

        if (this is PointerInputNode &&
            (hitDescendantPointerInputNode ||
                    overarchingBoundingBox?.contains(point.toOffset()) ?: false)
        ) {
            // If this is a PointerInputNode and we know we hit (either because we know we hit a
            // descendant PointerInputNode, or we just determined that we were hit, add us and
            // continue or start the fast backtrack.
            hitPointerInputNodes.add(this)
            return HitTestBoundingBoxResult(null, true)
        }

        // If we hit a descendant PointerInputNode, continue the fast back track.  Otherwise return
        // a bounding box.  If we are a LayoutNode, return the newMaxBoundingBox (which is our box
        // intersected with the passed in bounding box), or return the overarchingBoundingBox, which
        // would be the box around any returned LayoutNode bounding boxes.
        return if (hitDescendantPointerInputNode) {
            HitTestBoundingBoxResult(null, true)
        } else {
            HitTestBoundingBoxResult(
                if (this is LayoutNode) newMaxBoundingBox else overarchingBoundingBox,
                false
            )
        }
    }
}

data class HitTestBoundingBoxResult(val boundingBox: Rect?, val hit: Boolean)

/**
 * Produces [PointerInputChangeEvent]s by tracking changes between [PointerInputEvent]s
 */
private class PointerInputChangeEventProducer {
    private val previousPointerInputData: MutableMap<Int, PointerInputData> = mutableMapOf()

    internal fun produce(pointerEvent: PointerInputEvent):
            PointerInputChangeEvent {
        val changes: MutableList<PointerInputChange> = mutableListOf()
        pointerEvent.pointers.forEach {
            changes.add(
                PointerInputChange(
                    it.id,
                    it.pointerInputData,
                    previousPointerInputData[it.id] ?: PointerInputData(),
                    ConsumedData()
                )
            )
            if (it.pointerInputData.down) {
                previousPointerInputData[it.id] = it.pointerInputData
            } else {
                previousPointerInputData.remove(it.id)
            }
        }
        return PointerInputChangeEvent(pointerEvent.timestamp, changes)
    }
}

// TODO(shepshapard): The timestamp property probably doesn't need to exist (and therefore, nor does
// this class, but going to wait to refactor it out till after things like API review to avoid
// thrashing.
private data class PointerInputChangeEvent(
    val timestamp: Timestamp,
    val changes: List<PointerInputChange>
)