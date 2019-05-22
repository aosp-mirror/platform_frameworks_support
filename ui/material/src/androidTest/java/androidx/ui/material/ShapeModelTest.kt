
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

package androidx.ui.material

import androidx.test.filters.SmallTest
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.material.shape.Corner
import androidx.ui.material.shape.CutShapeModel
import androidx.ui.material.shape.RoundedShapeModel
import androidx.ui.material.shape.ShapeFamily
import androidx.ui.material.shape.ShapeModel
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ShapeModelTest {

    private val density = Density(2f)
    private val size = PxSize(100.px, 150.px)

    @Test
    fun roundedUniformCorners() {
        val rounded = RoundedShapeModel(Corner(25))

        val expectedRadius = Radius.circular(25f)
        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.rrect).isEqualTo(RRect(
            size.toRect(), expectedRadius
        ))
    }

    @Test
    fun roundedDifferentRadius() {
        val radius1 = 12f
        val radius2 = 22f
        val radius3 = 32f
        val radius4 = 42f
        val rounded = ShapeModel(ShapeFamily.Rounded,
            Corner(radius1.px),
            Corner(radius2.px),
            Corner(radius3.px),
            Corner(radius4.px)
        )

        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.rrect).isEqualTo(
            RRect(
                size.toRect(),
                Radius.circular(radius1),
                Radius.circular(radius2),
                Radius.circular(radius3),
                Radius.circular(radius4)
            )
        )
    }

    @Test
    fun rectangularCornerWithRoundedFamily() {
        val rectangular = RoundedShapeModel(Corner(0.dp))

        val outline = rectangular.toOutline() as Outline.Rectangle
        assertThat(outline.rect).isEqualTo(size.toRect())
    }

    @Test
    fun rectangularCornerWithCutFamily() {
        val rectangular = CutShapeModel(Corner(0.dp))

        val outline = rectangular.toOutline() as Outline.Rectangle
        assertThat(outline.rect).isEqualTo(size.toRect())
    }

    @Test
    fun cutCornersUniformCorners() {
        val cut = CutShapeModel(Corner(10.px))

        val outline = cut.toOutline() as Outline.Convex
        assertPathsEquals(outline.path, Path().apply {
            moveTo(0f, 10f)
            lineTo(10f, 0f)
            lineTo(90f, 0f)
            lineTo(100f, 10f)
            lineTo(100f, 140f)
            lineTo(90f, 150f)
            lineTo(10f, 150f)
            lineTo(0f, 140f)
            close()
        })
    }

    @Test
    fun cutCornersDifferentCorners() {
        val size1 = 12f
        val size2 = 22f
        val size3 = 32f
        val size4 = 42f
        val cut = ShapeModel(
            ShapeFamily.Cut,
            Corner(size1.px),
            Corner(size2.px),
            Corner(size3.px),
            Corner(size4.px)
        )

        val outline = cut.toOutline() as Outline.Convex
        assertPathsEquals(outline.path, Path().apply {
            moveTo(0f, 12f)
            lineTo(12f, 0f)
            lineTo(78f, 0f)
            lineTo(100f, 22f)
            lineTo(100f, 118f)
            lineTo(68f, 150f)
            lineTo(42f, 150f)
            lineTo(0f, 108f)
            close()
        })
    }

    private fun assertPathsEquals(path1: Path, path2: Path) {
        val diff = Path()
        val reverseDiff = Path()
        assertTrue(diff.op(path1, path2, PathOperation.difference) &&
                reverseDiff.op(path2, path1, PathOperation.difference) &&
                diff.isEmpty &&
                reverseDiff.isEmpty)
    }

    private fun ShapeModel.toOutline() = withDensity(density) {
        createOutline(size)
    }
}