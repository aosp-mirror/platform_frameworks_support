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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.engine.text.TextAlign
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.material.BottomDrawer
import androidx.ui.material.Divider
import androidx.ui.material.DrawerHeader
import androidx.ui.material.DrawerSectionLabel
import androidx.ui.material.DrawerState
import androidx.ui.material.DrawerVerticalSpace
import androidx.ui.material.ModalDrawer
import androidx.ui.material.NavigationDrawerItem
import androidx.ui.material.themeTextStyle

@Composable
fun ModalDrawerDemo() {
    val (state, onStateChange) = +state { DrawerState.Closed }
    ModalDrawer(
        state,
        onStateChange,
        drawerContent = { Profile(onStateChange) }
    ) {
        val text = if (state == DrawerState.Closed) "Drawer Closed.\n>>>> Pull to open >>>>"
        else "Drawer Opened.\n<<<< Swipe to close <<<<"
        Center {
            Text(text = text, textAlign = TextAlign.Center, style = +themeTextStyle { h5 })
        }
    }
}

@Composable
fun BottomDrawerDemo() {
    val (state, onStateChange) = +state { DrawerState.Closed }
    BottomDrawer(
        state,
        onStateChange,
        drawerContent = { Profile(onStateChange) }
    ) {
        val text =
            if (state == DrawerState.Closed) "Drawer Closed.\n▲▲▲ Pull to open ▲▲▲"
            else "Drawer Opened.\n▼▼▼ Drag down to close ▼▼▼"
        Center {
            Text(text = text, textAlign = TextAlign.Center, style = +themeTextStyle { h5 })
        }
    }
}

@Composable
fun Profile(onStateChange: (DrawerState) -> Unit) {
    val mainSection = listOf(
        "Primary",
        "Archived",
        "Reminders",
        "Favorites"
    )
    val secondarySection = listOf(
        "Starred",
        "Saved for later",
        "Important"
    )
    val (selected, onSelected) = +state { 0 }

    @Composable
    fun LocalNavItem(text: String, index: Int) {
        NavigationDrawerItem(
            selected = index == selected,
            onSelect = {
                onSelected(index)
                onStateChange(DrawerState.Closed)
            }) {
            Text(text)
        }
    }

    Column {
        DrawerHeader(
            title = { Text("Inbox") },
            subtitle = { Text("username@google.com") }
        )
        mainSection.forEachIndexed { index, item ->
            LocalNavItem(item, index)
        }
        DrawerVerticalSpace()
        Divider(color = Color(0x3a000000.toInt()))
        DrawerSectionLabel { Text("My folders") }
        DrawerVerticalSpace()
        secondarySection.forEachIndexed { index, item ->
            LocalNavItem(item, index + mainSection.size)
        }
    }
}
