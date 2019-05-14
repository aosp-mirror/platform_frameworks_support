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

import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GenerateStubs : MetalavaTask() {
    @get:OutputDirectory
    lateinit var stubLocation: File

    @TaskAction
    fun generate() {
        val dependencyClasspath = checkNotNull(
            dependencyClasspath) { "Dependency classpath not set." }

        runWithArgs(
            "--classpath",
            (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

            "--stubs",
            stubLocation.absolutePath,

            "--warning",
            "HiddenTypedefConstant",

            "--format=v3",
            "--output-kotlin-nulls=yes"
        )
    }
}