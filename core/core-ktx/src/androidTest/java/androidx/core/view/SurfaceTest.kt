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

package androidx.core.view

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class SurfaceTest {
    @Test fun use() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        surface.use {
            assertSame(surface, it)
            assertTrue(it.isValid)
        }
        assertFalse(surface.isValid)
    }

    @Test fun withLockedCanvas() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        surface.withLockedCanvas {
            // TODO assert something?
        }
        // TODO assert post?
        surface.release()
    }

    @Test fun withLockedCanvasInOutRect() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        val rect = Rect()
        surface.withLockedCanvas(rect) {
            // TODO assert something on canvas?
            // TODO assert rect?
        }
        // TODO assert post?
        surface.release()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test fun withLockedHardwareCanvas() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        surface.withLockedHardwareCanvas {
            // TODO assert something on canvas?
        }
        // TODO assert post?
        surface.release()
    }
}
