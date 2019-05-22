<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
=======
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

package androidx.ui.material.surface

import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.graphics.Color
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * A transparent [Surface] that draws [RippleEffect]s.
 *
 * While the material surface metaphor describes child widgets as printed on the
 * surface itself and do not hide [RippleEffect]s, in practice the [Surface]
 * draws children on top of the [RippleEffect].
 * A [TransparentSurface] can be placed on top of opaque components to show
 * [RippleEffect] effects on top of them.
 *
 * @param shape Defines the surface's shape.
 */
@Composable
fun TransparentSurface(
    shape: ShapeBorder = RoundedRectangleBorder(),
    @Children children: @Composable() () -> Unit
) {
    Surface(shape = shape, children = children, color = Color.Transparent)
}
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
