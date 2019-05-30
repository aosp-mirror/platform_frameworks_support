<<<<<<< HEAD   (45b71a Merge "Merge empty history for sparse-5611686-L1840000032132)
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

package androidx.ui.layout.test

import androidx.test.filters.SmallTest
import androidx.ui.core.ComplexLayout
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.toPx
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.layout.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class PaddingTest : LayoutTest() {

    @Test
    fun testPadding_IsApplied() = withDensity(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(padding = EdgeInsets(padding)) {
                child()
            }
        }
    }

    @Test
    fun testPadding_overloadAll_IsApplied() = withDensity(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(padding = padding) {
                child()
            }
        }
    }

    @Test
    fun testPadding_overloadSides_IsApplied() = withDensity(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(left = padding, top = padding, right = padding, bottom = padding) {
                child()
            }
        }
    }

    @Test
    fun testPadding_differentInsets() {
        val padding = EdgeInsets(10.dp, 15.dp, 20.dp, 30.dp)
        testPaddingWithDifferentInsetsImplementation(
            padding.left,
            padding.top,
            padding.right,
            padding.bottom
        ) { child: @Composable() () -> Unit ->
            Padding(padding = padding) {
                child()
            }
        }
    }

    @Test
    fun testPadding_overloadSides_differentInsets() {
        val left = 10.dp
        val top = 15.dp
        val right = 20.dp
        val bottom = 30.dp
        testPaddingWithDifferentInsetsImplementation(
            left,
            top,
            right,
            bottom
        ) { child: @Composable() () -> Unit ->
            Padding(left = left, top = top, right = right, bottom = bottom) {
                child()
            }
        }
    }

    @Test
    fun testPadding_withInsufficientSpace() = withDensity(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(padding = EdgeInsets(padding)) {
                child()
            }
        }
    }

    @Test
    fun testPadding_overloadAll_withInsufficientSpace() = withDensity(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(padding = padding) {
                child()
            }
        }
    }

    @Test
    fun testPadding_overloadSides_withInsufficientSpace() = withDensity(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable() () -> Unit ->
            Padding(left = 30.dp, right = 30.dp, top = 30.dp, bottom = 30.dp) {
                child()
            }
        }
    }

    private fun testPaddingIsAppliedImplementation(
        padding: Dp,
        paddingContainer: @Composable() (@Composable() () -> Unit) -> Unit
    ) = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = PxSize(-1.px, -1.px)
        var childPosition = PxPosition(-1.px, -1.px)
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints.tightConstraints(sizeDp, sizeDp)) {
                    val children = @Composable {
                        Container {
                            OnPositioned(onPositioned = { coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            })
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        val innerSize = (size - paddingPx * 2)
        assertEquals(PxSize(innerSize, innerSize), childSize)
        val left = ((root.width.ipx - size) / 2) + paddingPx
        val top = ((root.height.ipx - size) / 2) + paddingPx
        assertEquals(
            PxPosition(left.toPx(), top.toPx()),
            childPosition
        )
    }

    private fun testPaddingWithDifferentInsetsImplementation(
        left: Dp,
        top: Dp,
        right: Dp,
        bottom: Dp,
        paddingContainer: @Composable() ((@Composable() () -> Unit) -> Unit)
    ) = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = PxSize(-1.px, -1.px)
        var childPosition = PxPosition(-1.px, -1.px)
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints.tightConstraints(sizeDp, sizeDp)) {
                    val children = @Composable {
                        Container {
                            OnPositioned(onPositioned = { coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            })
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        val paddingLeft = left.toIntPx()
        val paddingRight = right.toIntPx()
        val paddingTop = top.toIntPx()
        val paddingBottom = bottom.toIntPx()
        assertEquals(
            PxSize(
                size - paddingLeft - paddingRight,
                size - paddingTop - paddingBottom
            ),
            childSize
        )
        val viewLeft = ((root.width.ipx - size) / 2) + paddingLeft
        val viewTop = ((root.height.ipx - size) / 2) + paddingTop
        assertEquals(
            PxPosition(viewLeft.toPx(), viewTop.toPx()),
            childPosition
        )
    }

    private fun testPaddingWithInsufficientSpaceImplementation(
        padding: Dp,
        paddingContainer: @Composable() ((@Composable() () -> Unit) -> Unit)
    ) =
        withDensity(density) {
            val sizeDp = 50.dp
            val size = sizeDp.toIntPx()
            val paddingPx = padding.toIntPx()

            val drawLatch = CountDownLatch(1)
            var childSize = PxSize(-1.px, -1.px)
            var childPosition = PxPosition(-1.px, -1.px)
            show {
                Center {
                    ConstrainedBox(constraints = DpConstraints.tightConstraints(sizeDp, sizeDp)) {
                        val children = @Composable {
                            Container {
                                OnPositioned(onPositioned = { coordinates ->
                                    childSize = coordinates.size
                                    childPosition = coordinates
                                        .localToGlobal(PxPosition(0.px, 0.px))
                                    drawLatch.countDown()
                                })
                            }
                        }
                        paddingContainer(children)
                    }
                }
            }
            drawLatch.await(1, TimeUnit.SECONDS)

            val root = findAndroidCraneView()
            waitForDraw(root)

            assertEquals(PxSize(0.px, 0.px), childSize)
            val left = ((root.width.ipx - size) / 2) + paddingPx
            val top = ((root.height.ipx - size) / 2) + paddingPx
            assertEquals(PxPosition(left.toPx(), top.toPx()), childPosition)
        }


    @Test
    fun testPadding_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        val layoutLatch = CountDownLatch(1)
        show {
            Center {
                val paddedChild = @Composable {
                    Padding(padding = 20.dp) {
                        AspectRatio(2f) { }
                    }
                }
                ComplexLayout(children = paddedChild) {
                    layout { measurables, _ ->
                        val paddingMeasurable = measurables.first()
                        // Min width.
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.minIntrinsicWidth(0.dp.toIntPx())
                        )
                        assertEquals(
                            100.dp.toIntPx(),
                            paddingMeasurable.minIntrinsicWidth(70.dp.toIntPx())
                        )
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.minIntrinsicWidth(IntPx.Infinity)
                        )
                        // Min height.
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.minIntrinsicHeight(0.dp.toIntPx())
                        )
                        assertEquals(
                            55.dp.toIntPx(),
                            paddingMeasurable.minIntrinsicHeight(70.dp.toIntPx())
                        )
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.minIntrinsicHeight(IntPx.Infinity)
                        )
                        // Max width.
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.maxIntrinsicWidth(0.dp.toIntPx())
                        )
                        assertEquals(
                            100.dp.toIntPx(),
                            paddingMeasurable.maxIntrinsicWidth(70.dp.toIntPx())
                        )
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.maxIntrinsicWidth(IntPx.Infinity)
                        )
                        // Max height.
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.maxIntrinsicHeight(0.dp.toIntPx())
                        )
                        assertEquals(
                            55.dp.toIntPx(),
                            paddingMeasurable.maxIntrinsicHeight(70.dp.toIntPx())
                        )
                        assertEquals(
                            20.dp.toIntPx() * 2,
                            paddingMeasurable.maxIntrinsicHeight(IntPx.Infinity)
                        )
                        layoutLatch.countDown()
                    }
                    minIntrinsicWidth { _, _ -> 0.ipx }
                    maxIntrinsicWidth { _, _ -> 0.ipx }
                    minIntrinsicHeight { _, _ -> 0.ipx }
                    maxIntrinsicHeight { _, _ -> 0.ipx }
                }
            }
        }
        layoutLatch.await(1, TimeUnit.SECONDS)
        Unit
    }

}
>>>>>>> BRANCH (2c8b21 Merge "Merge cherrypicks of [973155, 973156] into sparse-561)
