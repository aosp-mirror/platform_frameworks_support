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

package androidx.compose

internal val keyInfo = mutableMapOf<Int, String>()

internal fun recordSourceKeyInfo(key: Any) {
    when (key) {
        is Int -> keyInfo.getOrPut(key, {
            val frame = Thread.currentThread().stackTrace[5]
            "${frame.fileName}:${frame.lineNumber}"
        })
        is JoinedKey -> {
            key.left?.let { recordSourceKeyInfo(it) }
            key.right?.let { recordSourceKeyInfo(it) }
        }
    }
}

fun keySourceInfoOf(key: Any): String? = keyInfo[key]
