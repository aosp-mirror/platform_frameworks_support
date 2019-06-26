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

package androidx.ui.core.input

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.ui.input.EventType
import androidx.ui.input.KEYCODE_BACKSPACE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class KeyEventTest {

    @Test
    fun to_key_event_down_test() {
        val key = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL).toKeyEvent()
        assertEquals(KEYCODE_BACKSPACE, key.keyCode)
        assertEquals(EventType.KEY_DOWN, key.eventType)
    }

    @Test
    fun to_key_event_up_test() {
        val key = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL).toKeyEvent()
        assertEquals(KEYCODE_BACKSPACE, key.keyCode)
        assertEquals(EventType.KEY_UP, key.eventType)
    }
}