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

package androidx.ui.material.shape

import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.minSide
import androidx.ui.core.px

/**
 * Defines size of a corner. It can be a corner radius or cut length.
 */
typealias Corner = DensityReceiver.(PxSize) -> Px

/**
 * @size the corner size defined in [Dp].
 */
fun Corner(size: Dp): Corner = { size.toPx() }

/**
 * @size the corner size defined in [Px].
 */
fun Corner(size: Px): Corner = { size }

/**
 * @percent the corner size defined in float percents of the parent's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun Corner(percent: Float): Corner {
    if (percent < 0 || percent > 50) {
        throw IllegalArgumentException()
    }
    return { parentSize -> parentSize.minSide * (percent / 100f) }
}

/**
 * @percent the corner size defined in percents of the parent's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun /*inline*/ Corner(percent: Int) = Corner(percent.toFloat())

/**
 * Sharp corner has no corner radius or cut length.
 */
val SharpCorner: Corner = { 0.px }
