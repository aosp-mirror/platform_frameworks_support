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

package androidx.ui.input

// This file provides the definition of key code values in Jetpack Compose.
// Use HID USB usage ID as the KeyCode value which is likely the superset of key codes among the
// platforms. (see https://www.usb.org/sites/default/files/documents/hut1_12v2.pdf more details)
// All values listed here is came from Chromium's keycode conversion table.
// https://cs.chromium.org/chromium/src/ui/events/keycodes/dom/keycode_converter_data.inc

/* inline */ data class KeyCode(val keyCode: Int)

val KEYCODE_BACKSPACE = KeyCode(0x07002a)