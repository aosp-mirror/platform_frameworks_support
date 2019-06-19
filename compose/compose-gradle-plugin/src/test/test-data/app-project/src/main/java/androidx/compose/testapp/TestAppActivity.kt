/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.text.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.ui.layout.Column
import androidx.ui.core.Text

class CraneTextActivity : Activity() {
    val c = composer // pretend to use it to avoid warning. Works around compiler issue
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Text(text = "Hello World!")
            }
        }
    }
}

@Composable
fun Hello(name: String) {
    Text(text = "Hello $name!")
}
