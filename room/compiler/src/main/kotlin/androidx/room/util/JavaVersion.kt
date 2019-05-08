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

package androidx.room.util

/**
 * Class representing a Java version.
 *
 * NOTE: This class is greatly simplified to be used by the
 * [androidx.room.RoomProcessor.methodParametersVisibleInClassFiles] check only. If you want to use
 * this class, consider expanding the implementation or use a different library.
 */
data class JavaVersion(val major: Int, val minor: Int, val update: Int? = null) :
    Comparable<JavaVersion> {

    override fun compareTo(other: JavaVersion): Int {
        var result = major.compareTo(other.major)
        if (result != 0) {
            return result
        }

        result = minor.compareTo(other.minor)
        if (result != 0) {
            return result
        }

        if (update != null && other.update != null) {
            return update.compareTo(other.update)
        } else {
            return 0
        }
    }

    companion object {

        fun getCurrentVersion(): JavaVersion? {
            return tryParse(System.getProperty("java.runtime.version"))
        }

        /**
         * Parses the Java version from the given string. Returns `null` if it can't be parsed
         * successfully.
         */
        fun tryParse(version: String?): JavaVersion? {
            if (version == null) {
                return null
            }

            val parts = version.split('.')
            if (parts.size != 3) {
                return null
            }

            if (parts[0] == "1") {
                val major = parts[1]
                val minorAndUpdate = parts[2].substringBefore('-').split('_')
                if (minorAndUpdate.size != 2) {
                    return null
                }
                try {
                    return JavaVersion(
                        major.toInt(),
                        minorAndUpdate[0].toInt(),
                        minorAndUpdate[1].toInt()
                    )
                } catch (e: NumberFormatException) {
                    return null
                }
            } else {
                val major = parts[0]
                val minor = parts[1]
                try {
                    return JavaVersion(major.toInt(), minor.toInt())
                } catch (e: NumberFormatException) {
                    return null
                }
            }
        }
    }
}