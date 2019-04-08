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

package androidx.benchmark.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException

class BenchmarkPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        var adbPath: String? = null
        project.plugins.all {
            when (it) {
                is BasePlugin<*> -> {
                    adbPath = it.extension.adbExecutable.absolutePath
                }
            }
        }

        if (adbPath.isNullOrEmpty()) {
            throw StopExecutionException(
                """Unable to locate a usable Android SDK. The androidx.benchmark plugin currently
                    | only supports android application or library modules. Ensure that the
                    | androidx.plugin is applied after the com.android.application or
                    | com.android.library plugin.""".trimMargin()
            )
        }

        project.tasks.register("lockClocks", LockClocksTask::class.java, adbPath)
        project.tasks.register("unlockClocks", UnlockClocksTask::class.java, adbPath)
        val benchmarkReportTask =
            project.tasks.register("benchmarkReport", BenchmarkReportTask::class.java, adbPath!!)
        benchmarkReportTask.dependsOn("connectedAndroidTest")

        val basePlugin = project.plugins.first { it is AppPlugin || it is LibraryPlugin }
        val extensionVariants = when (basePlugin) {
            is AppPlugin -> project.extensions.getByType(AppExtension::class.java)
                .applicationVariants
            is LibraryPlugin -> project.extensions.getByType(LibraryExtension::class.java)
                .libraryVariants
            else -> throw StopExecutionException(
                """A required plugin, com.android.application or com.android.library was not found.
                    | The androidx.benchmark plugin currently only supports android application or
                    | library modules. Ensure that the androidx.plugin is applied after a supported
                    | plugin in the project build.gradle file.""".trimMargin()
            )
        }

        extensionVariants.first().apply {
            project.tasks.named("connectedAndroidTest").configure {
                applyPostAgpConfigure(project, it)
            }
        }
    }

    private fun applyPostAgpConfigure(project: Project, connectedAndroidTest: Task) {
        // The task benchmarkReport must be registered by this point, and is responsible for
        // pulling report data from all connected devices onto host machine through adb.
        connectedAndroidTest.finalizedBy("benchmarkReport")

        val hasJetpackBenchmark = project.configurations.any { config ->
            config.allDependencies.any {
                it.group == "androidx.benchmark" && it.name == "benchmark"
            }
        }

        if (!hasJetpackBenchmark) {
            throw StopExecutionException(
                """Missing required project dependency, androidx.benchmark:benchmark. The
                    | androidx.benchmark plugin is meant to be used in conjunction with the
                    | androix.benchmark library, but it was not found within this project's
                    | dependencies. You can add the androidx.benchmark library to your project by
                    | including androidTestImplementation 'androidx.benchmark:benchmark:<version>'
                    | in the dependencies block of the project build.gradle file"""
                    .trimMargin()
            )
        }
    }
}
