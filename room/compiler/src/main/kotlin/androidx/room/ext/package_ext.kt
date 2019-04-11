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

package androidx.room.ext

import java.io.File

/**
 * Map of overridden packages names. Useful for letting Room know which packages names to use
 * when generating code in a dejetified environment. To use this map pass to the annotation
 * processor parameter 'room.packageOverrideConfig' with the file location of a file containing
 * one key-value pair per line separated by '=' where the key is the androidx package name to
 * override and the value is the dejetified package name.
 *
 * Example of a typical config:
 * ```
 * # Room dejetifier packages for JavaPoet class names.
 * androidx.sqlite = android.arch.persistence
 * androidx.room = android.arch.persistence.room
 * androidx.paging = android.arch.paging
 * androidx.collection = com.android.support
 * ```
 */
val PACKAGE_NAME_OVERRIDES: MutableMap<String, String> = mutableMapOf()
internal fun parsePackageOverrideConfig(file: File) {
    try {
        file.bufferedReader().use {
            it.readLines()
                .filterNot { it.startsWith('#') }
                .map { it.split('=').let { split -> split[0].trim() to split[1].trim() } }
        }.let { PACKAGE_NAME_OVERRIDES.putAll(it) }
    } catch (ex: Exception) {
        throw RuntimeException("Unable to parse package override config file.", ex)
    }
}
val SQLITE_PACKAGE: String by lazyGetOrDefault("androidx.sqlite")
val ROOM_PACKAGE: String by lazyGetOrDefault("androidx.room")
val PAGING_PACKAGE: String by lazyGetOrDefault("androidx.paging")
val COLLECTION_PACKAGE: String by lazyGetOrDefault("androidx.collection")

private fun lazyGetOrDefault(key: String) = lazy { PACKAGE_NAME_OVERRIDES.getOrDefault(key, key) }
