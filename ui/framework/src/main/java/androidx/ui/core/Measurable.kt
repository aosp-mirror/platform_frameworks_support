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

/**
 * A part of the composition that can be measured. This represents a [Layout] somewhere
 * down the hierarchy.
 *
 * @return a [Placeable] that can be used within a [layoutResult] block
 */
abstract class Measurable {
    /**
     * Data provided by the [ParentData].
     */
    abstract val parentData: Any?
    protected abstract fun MeasureReceiver.measure(constraints: Constraints): Placeable
    protected abstract fun IntrinsicMeasurementsReceiver.minIntrinsicWidth(h: IntPx): IntPx
    protected abstract fun IntrinsicMeasurementsReceiver.maxIntrinsicWidth(h: IntPx): IntPx
    protected abstract fun IntrinsicMeasurementsReceiver.minIntrinsicHeight(w: IntPx): IntPx
    protected abstract fun IntrinsicMeasurementsReceiver.maxIntrinsicHeight(w: IntPx): IntPx

    private val measureReceiver = MeasureReceiver()
    internal fun measureInternal(constraints: Constraints): Placeable {
        return measureReceiver.measure(constraints)
    }
    private val intrinsicsReceiver = IntrinsicMeasurementsReceiver()
    internal fun minIntrinsicWidthInternal(h: IntPx) = intrinsicsReceiver.minIntrinsicWidth(h)
    internal fun maxIntrinsicWidthInternal(h: IntPx) = intrinsicsReceiver.maxIntrinsicWidth(h)
    internal fun minIntrinsicHeightInternal(w: IntPx) = intrinsicsReceiver.minIntrinsicHeight(w)
    internal fun maxIntrinsicHeightInternal(w: IntPx) = intrinsicsReceiver.maxIntrinsicHeight(w)

    class MeasureReceiver internal constructor() {
        fun Measurable.measure(constraints: Constraints) = measureInternal(constraints)
        fun Measurable.minIntrinsicWidth(h: IntPx) = minIntrinsicWidthInternal(h)
        fun Measurable.maxIntrinsicWidth(h: IntPx) = maxIntrinsicWidthInternal(h)
        fun Measurable.minIntrinsicHeight(w: IntPx) = minIntrinsicHeightInternal(w)
        fun Measurable.maxIntrinsicHeight(w: IntPx) = maxIntrinsicHeightInternal(w)
    }

    class IntrinsicMeasurementsReceiver internal constructor() {
        fun Measurable.minIntrinsicWidth(h: IntPx) = minIntrinsicWidthInternal(h)
        fun Measurable.maxIntrinsicWidth(h: IntPx) = maxIntrinsicWidthInternal(h)
        fun Measurable.minIntrinsicHeight(w: IntPx) = minIntrinsicHeightInternal(w)
        fun Measurable.maxIntrinsicHeight(w: IntPx) = maxIntrinsicHeightInternal(w)
    }
}
