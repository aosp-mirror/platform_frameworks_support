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

package androidx.build.metalava

import androidx.build.checkapi.ApiLocation
<<<<<<< HEAD   (be0ce7 Merge "Merge empty history for sparse-5662278-L1600000033295)
=======
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
>>>>>>> BRANCH (e55c95 Merge "Merge cherrypicks of [990151, 990154] into sparse-568)
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Generate an API signature text file from a set of source files. */
open class GenerateApiTask : MetalavaTask() {
    /** Text file to which API signatures will be written. */
    var apiLocation: ApiLocation? = null

    var generateRestrictedAPIs = false

    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (generateRestrictedAPIs) {
            return apiLocation?.files()
        }
        return listOf(apiLocation!!.publicApiFile)
    }

    @TaskAction
    fun exec() {
        val dependencyClasspath = checkNotNull(
                dependencyClasspath) { "Dependency classpath not set." }
<<<<<<< HEAD   (be0ce7 Merge "Merge empty history for sparse-5662278-L1600000033295)
        val publicApiFile = checkNotNull(apiLocation?.publicApiFile) { "Current public API file not set." }
        val restrictedApiFile = checkNotNull(apiLocation?.restrictedApiFile) { "Current restricted API file not set." }
=======
>>>>>>> BRANCH (e55c95 Merge "Merge cherrypicks of [990151, 990154] into sparse-568)
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        check(sourcePaths.isNotEmpty()) { "Source paths not set." }

        project.generateApi(
            bootClasspath,
            dependencyClasspath,
            sourcePaths,
            apiLocation.get().publicApiFile,
            false
        )

        if (generateRestrictedAPIs) {
            project.generateApi(
                bootClasspath,
                dependencyClasspath,
                sourcePaths,
                apiLocation.get().restrictedApiFile,
                true
            )
        }
    }
<<<<<<< HEAD   (be0ce7 Merge "Merge empty history for sparse-5662278-L1600000033295)

    // until b/119617147 is done, remove lines containing "@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)"
    fun removeRestrictToLibraryLines(inputFile: File, outputFile: File) {
        val outputBuilder = StringBuilder()
        val lines = inputFile.readLines()
        for (line in lines) {
            if (!line.contains("@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")) {
                outputBuilder.append(line)
                outputBuilder.append("\n")
            }
        }
        outputFile.writeText(outputBuilder.toString())
    }
=======
>>>>>>> BRANCH (e55c95 Merge "Merge cherrypicks of [990151, 990154] into sparse-568)
}
