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
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException

class BenchmarkPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.exec {
            if (project.properties["android.useAndroidX"] != "true") {
                throw GradleException(
                    """androidx.benchmark can only be used in AndroidX enabled
                        | projects! To enable AndroidX, add android.useAndroidX=true to your
                        | project's gradle.properties file. If your project has dependencies on
                        | legacy artifacts that need to be migrated to AndroidX, you can enable the
                        | Jetifier to do so by adding android.enableJetifier=true to your project's
                        | gradle.properties file. For more information, visit
                        | https://developer.android.com/jetpack/androidx/migrate."""
                        .trimMargin()
                )
            }
        }

        var sdkPath: String? = null
        project.plugins.all {
            when (it) {
                is LibraryPlugin -> {
                    val extension = project.extensions.getByType(LibraryExtension::class.java)
                    sdkPath = extension.sdkDirectory.path
                }
                is AppPlugin -> {
                    val extension = project.extensions.getByType(AppExtension::class.java)
                    sdkPath = extension.sdkDirectory.path
                }
            }
        }

        if (sdkPath.isNullOrEmpty()) {
            throw StopExecutionException("Unable to find Android SDK")
        }

        project.tasks.create("lockClocks", LockClocksTask::class.java, sdkPath)
        project.tasks.create("unlockClocks", UnlockClocksTask::class.java, sdkPath)
    }
}
