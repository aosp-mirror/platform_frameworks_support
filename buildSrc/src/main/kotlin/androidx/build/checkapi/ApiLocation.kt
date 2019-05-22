/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.checkapi

import java.io.File

import androidx.build.Version
import java.io.Serializable

// An ApiLocation contains the filepath of a public API and restricted API of a library
data class ApiLocation(
    // file specifying the public API of the library
    val publicApiFile: File,
    // file specifying the restricted API (marked by the RestrictTo annotation) of the library
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
    val restrictedApiFile: File
) {
=======
    val restrictedApiFile: File,
    // file specifying the API of the resources
    val resourceFile: File
) : Serializable {
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)

    fun files() = listOf(publicApiFile, restrictedApiFile)

    fun version(): Version? {
        val text = publicApiFile.name.removeSuffix(".txt")
        if (text == "current") {
            return null
        }
        return Version(text)
    }

    companion object {
        fun fromPublicApiFile(f: File): ApiLocation {
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
            return ApiLocation(f, File(f.parentFile, "restricted_" + f.name))
=======
            return ApiLocation(
                f,
                File(f.parentFile, "restricted_" + f.name),
                File(f.parentFile, "res-" + f.name)
            )
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
        }
    }
}

// An ApiViolationExclusions contains the paths of the API exclusions files for an API
data class ApiViolationExclusions(
    val publicApiFile: File,
    val restrictedApiFile: File
) : Serializable {

    fun files() = listOf(publicApiFile, restrictedApiFile)

    companion object {
        fun fromApiLocation(apiLocation: ApiLocation): ApiViolationExclusions {
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
            val publicExclusionsFile = File(apiLocation.publicApiFile.toString().removeSuffix(".txt") + ".ignore")
            val restrictedExclusionsFile = File(apiLocation.restrictedApiFile.parentFile.toString().removeSuffix(".txt") + ".ignore")
=======
            val publicExclusionsFile =
                File(apiLocation.publicApiFile.toString().removeSuffix(".txt") + ".ignore")
            val restrictedExclusionsFile =
                File(apiLocation.restrictedApiFile.toString().removeSuffix(".txt") + ".ignore")
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
            return ApiViolationExclusions(publicExclusionsFile, restrictedExclusionsFile)
        }
    }
}
