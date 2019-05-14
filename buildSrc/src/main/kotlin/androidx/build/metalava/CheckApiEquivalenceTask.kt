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
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Compares two API txt files against each other. */
open class CheckApiEquivalenceTask : DefaultTask() {
    /**
     * Api file (in the build dir) to check
     *
     * Note: Marked as an output so that this task will be properly incremental.
     */
    @get:InputFiles
    @get:OutputFiles
    lateinit var builtApi: ApiLocation

    /**
     * Api file (in source control) to compare against
     */
    var checkedInApis: List<ApiLocation> = listOf()

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (checkRestrictedAPIs) {
            return checkedInApis.flatMap { it.files() }
        }
        return checkedInApis.map { it.publicApiFile }
    }

    /**
     * Whether to check restricted APIs too
     */
    var checkRestrictedAPIs = false

    @TaskAction
    fun exec() {
        for (checkedInApi in checkedInApis) {
            if (!FileUtils.contentEquals(checkedInApi.publicApiFile, builtApi.publicApiFile)) {
                val message = "Public API definition has changed.\n\n" +
                        "Declared definition is ${checkedInApi.publicApiFile}\n" +
                        "True     definition is ${builtApi.publicApiFile}\n\n" +
                        "Please run `./gradlew updateApi` to confirm these changes are " +
                        "intentional by updating the public API definition"
                throw GradleException(message)
            }
            if (checkRestrictedAPIs) {
                if (!FileUtils.contentEquals(
                        checkedInApi.restrictedApiFile, builtApi.restrictedApiFile)) {
                    val message = "Restricted API definition (marked by the RestrictedTo " +
                            "annotation) has changed.\n\n" +
                            "Declared definition is ${checkedInApi.restrictedApiFile}\n" +
                            "True     definition is $builtApi.restrictedApiFile\n" +
                            "Please run `./gradlew updateApi` to confirm these changes are " +
                            "intentional by updating the restricted API definition"
                    throw GradleException(message)
                }
            }
        }
    }
}
