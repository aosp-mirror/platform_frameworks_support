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
import androidx.compose.setContent
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.HeightSpacer
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TopAppBar
import androidx.ui.material.themeTextStyle
import androidx.ui.graphics.Color
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.Padding
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.Button
import androidx.ui.painting.imageFromResource

class AppBarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    val favouriteImage = imageFromResource(resources, R.drawable.ic_favorite)
                    val navigationIcon: @Composable() () -> Unit = { AppBarIcon(favouriteImage) {} }
                    val smallActionsList = (0..1).map { favouriteImage }
                    val largeActionsList = (0..5).map { favouriteImage }
                    FlexColumn {
                        val showingTopAppBars = +state { true }
                        inflexible {
                            Padding(10.dp) {
                                Text("Currently showing ${if (showingTopAppBars.value) "top" else "bottom"} app bars")
                            }
                        }
                        expanded(1f) {
                            if (showingTopAppBars.value) {
                                Column {
                                    TopAppBar(title = "Default")
                                    HeightSpacer(height = 24.dp)
                                    TopAppBar(
                                        title = "Custom color",
                                        color = Color(0xFFE91E63.toInt())
                                    )
                                    HeightSpacer(height = 24.dp)
                                    TopAppBar(
                                        title = "Custom icons",
                                        navigationIcon = navigationIcon,
                                        actionItems = smallActionsList
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "No title", style = +themeTextStyle { h6 })
                                    TopAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = smallActionsList
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 24.dp)
                                    TopAppBar(
                                        title = "Too many icons",
                                        navigationIcon = navigationIcon,
                                        actionItems = largeActionsList
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                }
                            } else {
                                Column {
                                    Text(text = "No title", style = +themeTextStyle { h6 })
                                    BottomAppBar()
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "Custom color", style = +themeTextStyle { h6 })
                                    BottomAppBar(color = Color(0xFFE91E63.toInt()))
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "Custom icons", style = +themeTextStyle { h6 })
                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = smallActionsList
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "Too many icons", style = +themeTextStyle { h6 })
                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = largeActionsList
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    /** Menu items could either have a separate lambda for them:
                                     *
                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = largeActionsList,
                                        menuItem = { icon -> MenuListItem(icon) { launchSomething() } }
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }

                                     Or alternatively it could be a parameter inside the action lambda:

                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = largeActionsList
                                    ) { icon, isMenu ->
                                        if (isMenu) {
                                            MenuListItem(icon) { launchSomething() }
                                        } else {
                                            AppBarIcon(icon) { launchSomethingElse() }
                                        }
                                    }
                                     */
                                }
                            }
                        }
                        inflexible {
                            Button(
                                "Toggle between top/bottom app bars",
                                onClick = { showingTopAppBars.value = !showingTopAppBars.value })
                        }
                    }
                }
            }
        }
    }
}
