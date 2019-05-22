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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.ui.baseui.shape.Border
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.CraneWrapper
import androidx.ui.core.DensityReceiver
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.FixedSpacer
import androidx.ui.layout.Wrap
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.painting.Path

class CustomShapeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CustomShapeDemo() }
    }
}

@Composable
fun CustomShapeDemo() {
    CraneWrapper {
        MaterialTheme {
            Wrap(Alignment.Center) {
                Button(shape = CustomShape(), color = Color.Aqua, onClick = {}) {
                    FixedSpacer(100.dp, 100.dp)
                }
            }
        }
    }
}

class CustomShape : Shape {
    override val border: Border = Border(Color.DarkGray)

    override fun DensityReceiver.applyToPath(path: Path, parentSize: PxSize) {
        path.moveTo(parentSize.width.value / 2f, 0f)
        path.lineTo(parentSize.width.value, parentSize.height.value)
        path.lineTo(0f, parentSize.height.value)
        path.close()
    }
}