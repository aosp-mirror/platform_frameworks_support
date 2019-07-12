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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Text
import androidx.ui.graphics.Color
import androidx.ui.material.ContainedButton
import androidx.ui.material.OutlinedButton
import androidx.ui.material.TextButton
import androidx.ui.text.TextStyle

@Sampled
@Composable
fun OutlinedButtonSample() {
    OutlinedButton(
        text = "Outlined Button",
        onClick = {}
    )
}

@Sampled
@Composable
fun SlottedOutlinedButtonSample() {
    OutlinedButton(
        text = { Text(text = "Outlined Button", style = TextStyle(color = Color.Blue)) },
        onClick = {}
    )
}

@Sampled
@Composable
fun ContainedButtonSample() {
    ContainedButton(
        text = "Contained Button",
        onClick = {}
    )
}

@Sampled
@Composable
fun SlottedContainedButtonSample() {
    ContainedButton(
        text = { Text(text = "Contained Button", style = TextStyle(color = Color.Blue)) },
        onClick = {}
    )
}

@Sampled
@Composable
fun TextButtonSample() {
    TextButton(
        text = "Text Button",
        onClick = {}
    )
}

@Sampled
@Composable
fun SlottedTextButtonSample() {
    TextButton(
        text = { Text(text = "Text Button", style = TextStyle(color = Color.Blue)) },
        onClick = {}
    )
}