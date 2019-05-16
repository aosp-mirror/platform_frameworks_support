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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.IntPx
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.material.surface.Surface
import androidx.ui.test.android.AndroidUiTestRunner
import com.google.common.truth.Truth

fun AndroidUiTestRunner.setMaterialContent(composable: @Composable() () -> Unit) {
    setContent {
        MaterialTheme {
            Surface {
                composable()
            }
        }
    }
}

private val BigConstraints = DpConstraints(maxWidth = 5000.dp, maxHeight = 5000.dp)

fun AndroidUiTestRunner.performSizesTest(
    expectingWidth: IntPx,
    expectingHeight: IntPx,
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
) {
    sizesTestImpl(expectingWidth, expectingHeight, parentConstraints, children)
}

private fun AndroidUiTestRunner.sizesTestImpl(
    expectingWidth: IntPx?,
    expectingHeight: IntPx?,
    parentConstraints: DpConstraints,
    children: @Composable() () -> Unit
) {
    var realSize: PxSize? = null
    setMaterialContent {
        Wrap {
            Container(constraints = parentConstraints) {
                OnChildPositioned(onPositioned = { coordinates ->
                    realSize = coordinates.size
                }) {
                    children()
                }
            }
        }
    }
    withDensity(density) {
        if (expectingWidth != null) {
            Truth.assertThat(realSize?.width?.round()).isEqualTo(expectingWidth)
        }
        if (expectingHeight != null) {
            Truth.assertThat(realSize?.height?.round()).isEqualTo(expectingHeight)
        }
    }
}

fun AndroidUiTestRunner.performHeightTest(
    expectingHeight: IntPx?,
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
) {
    sizesTestImpl(null, expectingHeight, parentConstraints, children)
}

fun AndroidUiTestRunner.performWidthTest(
    expectingWidth: IntPx?,
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
) {
    sizesTestImpl(expectingWidth, null, parentConstraints, children)
}