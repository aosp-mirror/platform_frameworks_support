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

package androidx.ui.engine.geometry

import androidx.ui.painting.Path

/**
 * Defines a simple shape, used for bounding graphical regions.
 *
 * Can be used for defining a shape of the component background, a shape of
 * shadows cast by the component, or to clip the contents.
 */
sealed class Outline {
    /**
     * Rectangular area with sharp corners.
     */
    data class Sharp(val rect: Rect) : Outline()
    /**
     * Rectangular area with rounded corners.
     */
    data class Rounded(val rrect: RRect) : Outline()
    /**
     * A convex area defined as a path.
     *
     * A path is convex if it has a single contour, and only ever curves in a
     * single direction.
     */
    data class Convex(val path: Path) : Outline() {
        init {
            if (!path.isConvex) throw IllegalArgumentException("Path should be convex")
        }
    }
}

/**
 * Converts an [Outline] to a [Path].
 */
fun Outline.toPath(): Path = when (this) {
    is Outline.Sharp -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRRect(rrect) }
    is Outline.Convex -> path
}
