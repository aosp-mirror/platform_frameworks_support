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
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.baseui.Clickable
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.Padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TabLayout
import androidx.ui.painting.Image
import androidx.ui.painting.TextStyle

class TabActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    val icon =
                        Image(BitmapFactory.decodeResource(resources, R.drawable.ic_favorite))
                    TabDemo(icon)
                }
            }
        }
    }
}

@Composable
private fun TabDemo(icon: Image) {
    FlexColumn {
        expanded(flex = 1f) {
            val state = +state { 0 }
            TabLayout(
                tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
                onSelected = { state.value = it },
                selectedIndex = state.value
            ) { selected ->
                Center {
                    Text(
                        text = "Text tab ${selected + 1} selected",
                        style = TextStyle(fontSize = 40f)
                    )
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            TabLayout(
                tabs = listOf(icon, icon, icon),
                onSelected = { state.value = it },
                selectedIndex = state.value
            ) { selected ->
                Center {
                    Text(
                        text = "Image tab ${selected + 1} selected",
                        style = TextStyle(fontSize = 40f)
                    )
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            TabLayout(
                tabs = listOf("Tab 1" to icon, "Tab 2" to icon, "Tab 3" to icon),
                onSelected = { state.value = it },
                selectedIndex = state.value
            ) { selected ->
                Center {
                    Text(
                        text = "Text and image tab ${selected + 1} selected",
                        style = TextStyle(fontSize = 40f)
                    )
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            val tabs = listOf("Tab 1", "Tab 2", "Tab 3")
            TabLayout(
                tabRow = {
                    TabRow(selectedIndex = state.value) {
                        @Suppress("RemoveExplicitTypeArguments")
                        tabs.mapIndexed<String, @Composable() () -> Unit> { index, title ->
                            {
                                CustomTab(
                                    title = title,
                                    onClick = { state.value = index },
                                    selected = (index == state.value)
                                )
                            }
                        }
                    }
                }) {
                Center {
                    Text(
                        text = "Custom tab ${state.value + 1} selected",
                        style = TextStyle(fontSize = 40f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomTab(title: String, onClick: () -> Unit, selected: Boolean) {
    Container(height = 50.dp) {
        Clickable(onClick = { onClick() }) {
            Padding(10.dp) {
                FlexColumn {
                    inflexible {
                        if (selected) {
                            ColoredRect(height = 10.dp, width = 10.dp, color = Color.Red)
                        } else {
                            ColoredRect(height = 10.dp, width = 10.dp, color = Color.Gray)
                        }
                    }
                    inflexible {
                        Padding(5.dp) {
                            Text(text = title, style = TextStyle(fontSize = 40f))
                        }
                    }
                }
            }
        }
    }
}