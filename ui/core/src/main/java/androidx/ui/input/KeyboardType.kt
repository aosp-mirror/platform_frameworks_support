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

enum class KeyboardType {
    /**
     * A keyboard type used to request an IME that shows regular keyboard.
     */
    KEYBOARD_TYPE_TEXT,

    /**
     * A keyboard type used to request an IME that is capable of inputting ASCII characters.
     */
    KEYBOARD_TYPE_ASCII,

    /**
     * A keyboard type used to request an that is capable of inputting digits.
     */
    KEYBOARD_TYPE_NUMBER,

    /**
     * A keyboard type used to request an IME is capable of inputting phone numbers.
     */
    KEYBOARD_TYPE_PHONE,

    /**
     * A keyboard type used to request an IME is capable of inputting URIs.
     */
    KEYBOARD_TYPE_URI,

    /**
     * A keyboard type used to request an IME is capable of inputting email addresses.
     */
    KEYBOARD_TYPE_EMAIL
}
