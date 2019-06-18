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

package androidx.build.metalava

import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.ApiViolationBaselines
import com.google.common.io.Files
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Updates an API lint baseline file (api/api_lint.ignore) to match the current set of violations
 */
abstract class UpdateApiLintBaselineTask : MetalavaTask() {
    @get:Input
    abstract val baselines: Property<ApiViolationBaselines>

    @OutputFile
    fun getApiLintBaseline(): File = baselines.get().apiLintFile

    @TaskAction
    fun updateBaseline() {
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }
        val temporaryBaselineFile = File.createTempFile("baseline", "ignore")
        val args = getCommonBaselineUpdateArgs(
            bootClasspath,
            dependencyClasspath!!,
            sourcePaths,
            temporaryBaselineFile
        )
        args.add("--api-lint")
        runWithArgs(args)
        updateBaselineFile(temporaryBaselineFile, baselines.get().apiLintFile)
    }
}

/**
 * Updates an API tracking baseline file (api/X.Y.Z.ignore) to match the current set of violations
 */
abstract class IgnoreApiChangesTask : MetalavaTask() {
    // The API that the library is supposed to be compatible with
    @get:Input
    abstract val referenceApi: Property<ApiLocation>

    // The baseline files (api/*.*.*.ignore) to update
    @get:Input
    abstract val baselines: Property<ApiViolationBaselines>

    // Whether to update the file having restricted APIs too
    @get:Input
    var processRestrictedApis = false

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (processRestrictedApis) {
            return referenceApi.get().files()
        }
        return listOf(referenceApi.get().publicApiFile)
    }

    // Declaring outputs prevents Gradle from rerunning this task if the inputs haven't changed
    @OutputFiles
    fun getTaskOutputs(): List<File>? {
        if (processRestrictedApis) {
            return listOf(baselines.get().publicApiFile, baselines.get().restrictedApiFile)
        }
        return listOf(baselines.get().publicApiFile)
    }

    @TaskAction
    fun exec() {
        check(bootClasspath.isNotEmpty()) { "Android boot classpath not set." }

        updateBaseline(
            referenceApi.get().publicApiFile,
            baselines.get().publicApiFile,
            false
        )
        if (processRestrictedApis && referenceApi.get().restrictedApiFile.exists()) {
            updateBaseline(
                referenceApi.get().restrictedApiFile,
                baselines.get().restrictedApiFile,
                true
            )
        }
    }

    // Updates the contents of baselineFile to specify an exception for every API present in apiFile but not
    // present in the current source path
    private fun updateBaseline(
        apiFile: File,
        baselineFile: File,
        processRestrictedApis: Boolean
    ) {
        val temporaryBaselineFile = File.createTempFile("baseline", "ignore")
        val args = getCommonBaselineUpdateArgs(
            bootClasspath,
            dependencyClasspath!!,
            sourcePaths,
            temporaryBaselineFile
        )
        args += listOf(
            "--check-compatibility:api:released",
            apiFile.toString()
        )
        if (processRestrictedApis) {
            args += listOf(
                "--show-annotation",
                "androidx.annotation.RestrictTo"
            )
        }
        runWithArgs(args)
        updateBaselineFile(temporaryBaselineFile, baselineFile)
    }
}

private fun getCommonBaselineUpdateArgs(
    bootClasspath: Collection<File>,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    temporaryBaselineFile: File
): MutableList<String> {
    return mutableListOf(
        "--classpath",
        (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

        "--source-path",
        sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

        "--update-baseline",
        temporaryBaselineFile.toString(),
        "--baseline", temporaryBaselineFile.toString(),
        "--pass-baseline-updates",

        "--format=v3",
        "--omit-common-packages=yes"
    )
}

private fun updateBaselineFile(source: File, destination: File) {
    var moreThanHeader = false
    source.forEachLine {
        if (!it.startsWith("// Baseline format: ")) {
            moreThanHeader = true
            return@forEachLine
        }
    }
    if (moreThanHeader) {
        Files.copy(source, destination)
        source.delete()
    } else {
        if (destination.exists()) {
            destination.delete()
        }
    }
}