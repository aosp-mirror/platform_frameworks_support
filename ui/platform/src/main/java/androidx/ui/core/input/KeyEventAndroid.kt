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

import androidx.ui.input.EventType
import androidx.ui.input.KEYCODE_BACKSPACE
import androidx.ui.input.KeyCode
import androidx.ui.input.KeyEvent
import java.lang.IllegalArgumentException

/**
 * Convert from Android KeyEvent to Jetpack Compose KeyEvent.
 */
fun android.view.KeyEvent.toKeyEvent(): KeyEvent = KeyEvent(
    eventType = androidActionToEventType(action),
    keyCode = androidKeyToJetpackComposeKey(keyCode))

private fun androidActionToEventType(action: Int): EventType {
    return if (action == android.view.KeyEvent.ACTION_DOWN) {
        EventType.KEY_DOWN
    } else if (action == android.view.KeyEvent.ACTION_UP) {
        EventType.KEY_UP
    } else {
        throw IllegalArgumentException("Unsupported Action: $action")
    }
}

private fun androidKeyToJetpackComposeKey(androidKeyCode: Int): KeyCode =
    when (androidKeyCode) {
        android.view.KeyEvent.KEYCODE_DEL -> KEYCODE_BACKSPACE
        else -> throw IllegalArgumentException("Unsupported KeyCode: $androidKeyCode")
    }