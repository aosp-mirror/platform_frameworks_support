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

package androidx.ui.test

import androidx.compose.composer
import android.app.Activity
import androidx.compose.Composable
import androidx.compose.CompositionContext
import androidx.ui.core.composeIntoActivity
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import java.lang.AssertionError

fun ComposeMaterialIntoActivity(
    activity: Activity,
    composable: @Composable() () -> Unit
): CompositionContext? {
    return composeIntoActivity(activity) {
        MaterialTheme {
            Surface {
                composable()
            }
        }
    }
}

/**
 * Performs recomposition and throws exception if there were no pending changes.
 */
fun CompositionContext.recomposeSyncAssertHadChanges() {
    val hadChanges = recomposeSync()
    if (!hadChanges) {
        throw AssertionError("Expected pending changes on recomposition but there " +
            "were none. Did you forget to call FrameManager.next()?")
    }
}

/**
 * Performs recomposition and throws exception if there were any pending changes.
 */
fun CompositionContext.recomposeSyncAssertNoChanges() {
    val hadChanges = recomposeSync()
    if (hadChanges) {
        throw AssertionError("Expected no pending changes on recomposition but " +
                "there were some.")
    }
}