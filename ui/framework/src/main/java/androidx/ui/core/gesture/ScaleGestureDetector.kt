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

package androidx.ui.core.gesture

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.positionChange
import kotlin.math.abs
import kotlin.math.hypot
import androidx.ui.core.PointerInputWrapper
import androidx.compose.composer
import androidx.ui.testutils.consume

interface ScaleObserver {

    /**
     * Override to be notified when a drag has started.
     *
     * This will be called when at least 2 pointers have been added. Always called just before
     * [onScale] and isn't called again until after [onStop].
     *
     * @see onScale
     * @see onStop
     */
    fun onStart() {}

    /**
     * Override to be notified when a distance has been dragged.
     *
     * When overridden, return the amount of the [percentageChanged] that has been consumed.
     *
     * Always called just after [onStart] (and for every subsequent drag).
     *
     * @param percentageChanged The percentage increase or decrease from 1f percent.  For example,
     * if a thing is 50 dp long, a percentageChanged of -.5 will suggest the thing should become 25
     * dp long.
     */
    fun onScale(percentageChanged: Float) = 0f

    /**
     * Override to be notified when scaling has stopped.
     *
     * This is called once less than 2 pointers remain.
     *
     * Only called after [onStart], followed by one or more calls to [onScale].
     */
    fun onStop() {}
}

// TODO(shepshapard): Convert to functional component with effects once effects are ready.
// TODO(shepshapard): Update docs
/**
 * This gesture detector detects scaling.
 */
@Composable
fun ScaleGestureDetector(
    scaleObserver: ScaleObserver? = null,
    @Children children: @Composable() () -> Unit
) {
    val recognizer = +memo { ScaleGestureRecognizer() }
    // TODO(b/129784010): Consider also allowing onStart, onDrag, and onEnd to be set individually.
    recognizer.scaleObserver = scaleObserver

    PointerInputWrapper(pointerInputHandler = recognizer.pointerInputHandler) {
        children()
    }
}

internal class ScaleGestureRecognizer {
    private var scalingActive = false
    var scaleObserver: ScaleObserver? = null

    val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass ->

            var changesToReturn = changes

            if (pass == PointerEventPass.InitialDown && scalingActive) {
                // If we are currently scaling, we want to prevent any children from reacting to any
                // down change.
                changesToReturn = changesToReturn.map {
                    if (it.changedToDown()) {
                        it.consumeDownChange()
                    } else {
                        it
                    }
                }
            }

            if (pass == PointerEventPass.PostUp || pass == PointerEventPass.PostDown) {

                var (currentlyDownChanges, otherChanges) = changesToReturn.partition {
                    it.current.down && !it.changedToDownIgnoreConsumed()
                }

                if (currentlyDownChanges.size >= 2) {
                    if (!scalingActive) {
                        scalingActive = true
                        scaleObserver?.onStart()
                    }
                } else {
                    if (scalingActive) {
                        scalingActive = false
                        scaleObserver?.onStop()
                    }
                }

                if (scalingActive && scaleObserver != null) {
                    val dimensionInformation =
                        currentlyDownChanges.calculateAllDimensionInformation()
                    val scalePercentage = dimensionInformation.calculateScaleChange()

                    if (scalePercentage != 1f) {

                        val scalePercentageUsed = scaleObserver!!.onScale(scalePercentage)

                        var percentageOfChangeUsed =
                            if (scalePercentage < 1) {
                                (scalePercentage - 1) / (scalePercentageUsed - 1)
                            } else if (scalePercentage > 1) {
                                (scalePercentageUsed - 1) / (scalePercentage - 1)
                            } else {
                                0f
                            }

                        if (percentageOfChangeUsed > 1) percentageOfChangeUsed = 1f
                        if (percentageOfChangeUsed < 0) percentageOfChangeUsed = 0f

                        if (percentageOfChangeUsed > 0f) {
                            val newCurrentlyDownChanges = mutableListOf<PointerInputChange>()
                            for (i in 0 until currentlyDownChanges.size) {

                                val xVectorToAverageChange =
                                    getVectorToAverageChange(
                                        dimensionInformation.previousX,
                                        dimensionInformation.currentX,
                                        i
                                    ) * percentageOfChangeUsed

                                val yVectorToAverageChange =
                                    getVectorToAverageChange(
                                        dimensionInformation.previousY,
                                        dimensionInformation.currentY,
                                        i
                                    ) * percentageOfChangeUsed

                                newCurrentlyDownChanges
                                    .add(
                                        currentlyDownChanges[i].consume(
                                            xVectorToAverageChange,
                                            yVectorToAverageChange
                                        )
                                    )
                            }
                            currentlyDownChanges = newCurrentlyDownChanges
                        }
                    }
                }

                changesToReturn = currentlyDownChanges + otherChanges
            }

            changesToReturn
        }
}

private fun getVectorToAverageChange(
    previous: DimensionInformation,
    current: DimensionInformation,
    i: Int
): Float {
    val currentVectorToAverage = current.vectorsToAverage[i]
    val previousVectorToAverage = previous.vectorsToAverage[i]
    val absDistanceChangedX = abs(abs(currentVectorToAverage) - abs(previousVectorToAverage))
    return if (currentVectorToAverage - previousVectorToAverage < 0) {
        -absDistanceChangedX
    } else {
        absDistanceChangedX
    }
}

private fun List<Float>.calculateDimensionInformation(): DimensionInformation {
    val average = average().toFloat()
    val vectorsToAverage = map {
        it - average
    }
    return DimensionInformation(average, vectorsToAverage)
}

private fun List<PointerInputChange>.calculateAllDimensionInformation() =
    AllDimensionInformation(
        map {
            it.previous.position!!.x.value
        }.calculateDimensionInformation(),
        map {
            it.previous.position!!.y.value
        }.calculateDimensionInformation(),
        map {
            it.previous.position!!.x.value + it.positionChange().x.value
        }.calculateDimensionInformation(),
        map {
            it.previous.position!!.y.value + it.positionChange().y.value
        }.calculateDimensionInformation()
    )

private fun AllDimensionInformation.calculateScaleChange() =
    averageDistanceToCenter(currentX, currentY) / averageDistanceToCenter(previousX, previousY)

private fun averageDistanceToCenter(x: DimensionInformation, y: DimensionInformation): Float {
    var totalDistanceToCenter = 0f
    val count = x.vectorsToAverage.size
    for (i in 0 until count) {
        totalDistanceToCenter += hypot(x.vectorsToAverage[i], y.vectorsToAverage[i])
    }
    return totalDistanceToCenter / count
}

private data class DimensionInformation(
    val average: Float,
    val vectorsToAverage: List<Float>
)

private data class AllDimensionInformation(
    val previousX: DimensionInformation,
    val previousY: DimensionInformation,
    val currentX: DimensionInformation,
    val currentY: DimensionInformation
)
