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
import androidx.ui.material.Tab
import androidx.ui.material.TabRow
import androidx.ui.painting.Image
import androidx.ui.painting.TextStyle
import androidx.ui.painting.imageFromResource

class TabActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    val favouriteImage = imageFromResource(resources, R.drawable.ic_favorite)
                    TabDemo(favouriteImage)
                }
            }
        }
    }
}

@Composable
private fun TabDemo(image: Image) {
    FlexColumn {
        expanded(flex = 1f) {
            val state = +state { 0 }
            val titles = listOf("TAB 1", "TAB 2", "TAB 3")
            FlexColumn {
                inflexible {
                    TabRow(items = titles, selectedIndex = state.value) { index, text ->
                        Tab(text = text, selected = state.value == index) { state.value = index }
                    }
                }
                flexible(flex = 1f) {
                    TabContent("Text tab", state.value)
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            val icons = listOf(image, image, image)
            FlexColumn {
                inflexible {
                    TabRow(items = icons, selectedIndex = state.value) { index, icon ->
                        Tab(icon = icon, selected = state.value == index) { state.value = index }
                    }
                }
                flexible(flex = 1f) {
                    TabContent("Icon tab", state.value)
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            val titlesAndIcons = listOf("TAB 1" to image, "TAB 2" to image, "TAB 3" to image)
            FlexColumn {
                inflexible {
                    TabRow(items = titlesAndIcons, selectedIndex = state.value) { index, data ->
                        // TODO: r8 bug preventing us from destructuring inline
                        val (title, icon) = data
                        Tab(text = title, icon = icon, selected = state.value == index) {
                            state.value = index
                        }
                    }
                }
                flexible(flex = 1f) {
                    TabContent("Text and icon tab", state.value)
                }
            }
        }
        expanded(flex = 1f) {
            val state = +state { 0 }
            val titles = listOf("TAB 1", "TAB 2", "TAB 3")
            FlexColumn {
                inflexible {
                    TabRow(items = titles, selectedIndex = state.value) { index, title ->
                        FancyTab(
                            title = title,
                            onClick = { state.value = index },
                            selected = (index == state.value)
                        )
                    }
                }
                flexible(flex = 1f) {
                    TabContent("Custom tab", state.value)
                }
            }
        }
    }
}

@Composable
private fun TabContent(tabType: String, selected: Int) {
    Center {
        Text(
            text = "$tabType ${selected + 1} selected",
            style = TextStyle(fontSize = 50f)
        )
    }
}

@Composable
private fun FancyTab(title: String, onClick: () -> Unit, selected: Boolean) {
    Clickable(onClick = { onClick() }) {
        Container(height = 50.dp) {
            Padding(10.dp) {
                FlexColumn {
                    inflexible {
                        val color = if (selected) Color.Red else Color.Gray
                        ColoredRect(height = 10.dp, width = 10.dp, color = color)
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
