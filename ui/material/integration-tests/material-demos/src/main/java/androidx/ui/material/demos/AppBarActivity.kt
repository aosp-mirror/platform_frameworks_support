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
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.Button
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.FloatingActionButtonPosition
import androidx.ui.painting.imageFromResource

class AppBarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    val favouriteImage = imageFromResource(resources, R.drawable.ic_favorite)
                    val navigationImage = imageFromResource(resources, R.drawable.ic_menu)
                    val navigationIcon: @Composable() () -> Unit = { AppBarIcon(navigationImage) {} }
                    val smallActionsList = (0..1).map { favouriteImage }
                    val largeActionsList = (0..5).map { favouriteImage }
                    FlexColumn {
                        val showingTopAppBars = +state { true }
                        val showingSmallActions = +state { true }
                        val actionItems = if (showingSmallActions.value) smallActionsList else largeActionsList
                        inflexible {
                            HeightSpacer(15.dp)
                            Text("Currently showing: ${if (showingTopAppBars.value) "top" else "bottom"} app bars")
                            HeightSpacer(15.dp)
                            Text("Number of actions: ${if (showingSmallActions.value) smallActionsList.size else largeActionsList.size}")
                            HeightSpacer(15.dp)
                        }
                        expanded(1f) {
                            if (showingTopAppBars.value) {
                                Column {
                                    TopAppBar(title = { Text("Default") })
                                    HeightSpacer(height = 24.dp)
                                    TopAppBar(
                                        title = { Text("Custom color") },
                                        color = Color(0xFFE91E63.toInt())
                                    )
                                    HeightSpacer(height = 24.dp)
                                    TopAppBar(
                                        title = { Text("With icons") },
                                        navigationIcon = navigationIcon,
                                        actionItems = actionItems
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "No title", style = +themeTextStyle { h6 })
                                    TopAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = actionItems
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                }
                            } else {
                                Column {
                                    Text(text = "Empty", style = +themeTextStyle { h6 })
                                    BottomAppBar()
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "Custom color", style = +themeTextStyle { h6 })
                                    BottomAppBar(color = Color(0xFFE91E63.toInt()))
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "With icons", style = +themeTextStyle { h6 })
                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        actionItems = actionItems
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "Center FAB", style = +themeTextStyle { h6 })
                                    BottomAppBar(
                                        navigationIcon = navigationIcon,
                                        floatingActionButton = {
                                            FloatingActionButton(color = Color.Black, icon = favouriteImage, onClick = {})
                                        },
                                        floatingActionButtonPosition = FloatingActionButtonPosition.CENTER,
                                        actionItems = actionItems
                                    ) { icon ->
                                        AppBarIcon(icon) {}
                                    }
                                    HeightSpacer(height = 12.dp)
                                    Text(text = "End FAB", style = +themeTextStyle { h6 })
                                    BottomAppBar(
                                        floatingActionButton = {
                                            FloatingActionButton(color = Color.Black, icon = favouriteImage, onClick = {})
                                        },
                                        floatingActionButtonPosition = FloatingActionButtonPosition.END,
                                        actionItems = actionItems
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
                            HeightSpacer(10.dp)
                            Button(
                                "Toggle between 2 and 6 actions",
                                onClick = { showingSmallActions.value = !showingSmallActions.value })
                        }
                    }
                }
            }
        }
    }
}
