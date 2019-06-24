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

package androidx.ui.tooling

import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.SlotTable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus
import java.util.Collections
import java.util.WeakHashMap

/**
 * Toolable mode ambient. True if the the composition is composed inside a Preview component.
 */
val ToolsMode = Ambient.of("Tools Mode") { false }

/**
 * A wrapper for compositions in tools mode. The composition below the Toolable child composition is
 * considered in tools mode.
 *
 * @hide
 */
@Composable
fun Toolable(@Children children: @Composable() () -> Unit) {
    composer.composer.collectKeySourceInformation()
    tables.add(composer.composer.slotTable)
    ToolsMode.Provider(true) {
        children()
    }
}

val tables = Collections.newSetFromMap(WeakHashMap<SlotTable, Boolean>())

/**
 * A wrapper for tools-only behavior. It will the children will only be composed if the composition
 * is in tools mode.
 */
@Composable
fun InToolsModeOnly(@Children children: @Composable() () -> Unit) {
    if (+ambient(ToolsMode)) {
        children()
    }
}
