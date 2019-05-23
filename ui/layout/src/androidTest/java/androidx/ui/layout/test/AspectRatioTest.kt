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

package androidx.ui.layout.test

import androidx.compose.Children
import androidx.test.filters.SmallTest
import androidx.ui.core.Dp
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FixedSpacer
import androidx.ui.layout.Row
import androidx.ui.layout.Wrap
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.ComplexLayout
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.layout.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class AspectRatioTest : LayoutTest() {
    @Test
    fun testAspectRatio_placesChild() = withDensity(density) {
        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<PxSize>()
        val containerPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                AspectRatio(1f) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        containerSize.value = coordinates.size
                        containerPosition.value = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        positionedLatch.countDown()
                    }) {
                        Container(width = 30.dp, height = 40.dp) { }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertNotNull(containerSize.value)
        assertEquals(containerSize.value!!.width, containerSize.value!!.height)
        assertNotNull(containerPosition.value)
        assertEquals(containerPosition.value!!.x, 0.px)
        assertEquals(containerPosition.value!!.y, 0.px)
    }

    @Test
    fun testAspectRatio_sizesCorrectly() = withDensity(density) {
        assertEquals(PxSize(30.px, 30.px), getSize(1f, Constraints(maxWidth = 30.ipx)))
        assertEquals(PxSize(30.px, 15.px), getSize(2f, Constraints(maxWidth = 30.ipx)))
        assertEquals(
            PxSize(10.px, 10.px),
            getSize(1f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            PxSize(20.px, 10.px),
            getSize(2f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            PxSize(10.px, 5.px),
            getSize(2f, Constraints(minWidth = 10.ipx, minHeight = 5.ipx))
        )
        assertEquals(
            PxSize(20.px, 10.px),
            getSize(2f, Constraints(minWidth = 5.ipx, minHeight = 10.ipx))
        )
    }

    @Test
    fun testAspectRatio_intrinsicDimensions() = withDensity(density) {
        val positionedLatch = CountDownLatch(1)
        show {
            Align(alignment = Alignment.TopLeft) {
                val children = @Composable {
                    AspectRatio(2f) {
                        OnPositioned(onPositioned = { _ -> positionedLatch.countDown() })
                        Container(width = 30.dp, height = 40.dp) { }
                    }
                }
                ComplexLayout(children) {
                    layout { measurables, constraints ->
                        val measurable = measurables.first()
                        assertEquals(40.ipx, measurable.minIntrinsicWidth(20.ipx))
                        assertEquals(40.ipx, measurable.maxIntrinsicWidth(20.ipx))
                        assertEquals(20.ipx, measurable.minIntrinsicHeight(40.ipx))
                        assertEquals(20.ipx, measurable.maxIntrinsicHeight(40.ipx))

                        assertEquals(30.dp.toIntPx(), measurable.minIntrinsicWidth(IntPx.Infinity))
                        assertEquals(30.dp.toIntPx(), measurable.maxIntrinsicWidth(IntPx.Infinity))
                        assertEquals(40.dp.toIntPx(), measurable.minIntrinsicHeight(IntPx.Infinity))
                        assertEquals(40.dp.toIntPx(), measurable.maxIntrinsicHeight(IntPx.Infinity))

                        val placeable = measurable.measure(constraints)
                        layoutResult(placeable.width, placeable.height) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    }
                    minIntrinsicWidth { _, _ -> 0.ipx }
                    maxIntrinsicWidth { _, _ -> 0.ipx }
                    minIntrinsicHeight { _, _ -> 0.ipx }
                    maxIntrinsicHeight { _, _ -> 0.ipx }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    private fun getSize(aspectRatio: Float, aspectRatioConstraints: Constraints): PxSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<PxSize>()
        show {
            Align(alignment = Alignment.TopLeft) {
                val children = @Children {
                    AspectRatio(aspectRatio) {
                        SaveLayoutInfo(size, Ref<PxPosition>(), positionedLatch)
                    }
                }
                Layout(children) { measurables, constraints ->
                    val placeable = measurables.first().measure(aspectRatioConstraints)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertNotNull(size.value)
        return size.value!!
    }
}
