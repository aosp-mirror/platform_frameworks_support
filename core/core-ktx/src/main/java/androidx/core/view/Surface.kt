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

@file:Suppress("NOTHING_TO_INLINE") // Aliases to other public API.

package androidx.core.view

import android.graphics.Canvas
import android.graphics.Rect
import android.view.Surface
import androidx.annotation.RequiresApi

inline fun <R> Surface.use(block: (Surface) -> R): R {
    return try {
        block(this)
    } finally {
        release()
    }
}

inline fun <R> Surface.withLockedCanvas(inOutDirty: Rect? = null, block: (Canvas) -> R): R {
    val canvas = lockCanvas(inOutDirty)
    val result = block(canvas)
    unlockCanvasAndPost(canvas)
    return result
}

@RequiresApi(23)
inline fun <R> Surface.withLockedHardwareCanvas(block: (Canvas) -> R): R {
    val canvas = lockHardwareCanvas()
    val result = block(canvas)
    unlockCanvasAndPost(canvas)
    return result
}
