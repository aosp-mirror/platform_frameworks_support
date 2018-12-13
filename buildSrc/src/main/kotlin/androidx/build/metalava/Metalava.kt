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

import androidx.build.AndroidXPlugin.Companion.BUILD_ON_SERVER_TASK
import androidx.build.Release
import androidx.build.SupportLibraryExtension
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.getCurrentApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.checkapi.hasApiFolder
import androidx.build.checkapi.hasApiTasks
import androidx.build.docsDir
import androidx.build.java.JavaCompileInputs
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getPlugin
import java.io.File

object Metalava {
    private fun Project.createMetalavaConfiguration(): Configuration {
        return configurations.create("metalava") {
            val dependency = dependencies.create("com.android:metalava:1.1.2-SNAPSHOT:shadow@jar")
            it.dependencies.add(dependency)
        }
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }

        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                if (!project.hasApiFolder()) {
                    project.logger.info(
                        "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
                    return@all
                }

                val javaInputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                setupProject(project, javaInputs, extension)
            }
        }
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }
        if (!project.hasApiFolder()) {
            project.logger.info(
                    "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
            return
        }

        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        val javaInputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
        setupProject(project, javaInputs, extension)
    }

    fun applyInputs(inputs: JavaCompileInputs, task: TaskProvider<out MetalavaTask>) {
        task.configure {
            it.sourcePaths = inputs.sourcePaths
            it.dependencyClasspath = inputs.dependencyClasspath
            it.bootClasspath = inputs.bootClasspath
        }
    }

    fun setupProject(project: Project, javaCompileInputs: JavaCompileInputs, extension: SupportLibraryExtension) {
        val metalavaConfiguration = project.createMetalavaConfiguration()

        // the api files whose file names contain the version of the library
        val libraryVersionApi = project.getCurrentApiLocation()
        // the api files whose file names contain "current.txt"
        val currentTxtApi = ApiLocation.fromPublicApiFile(File(libraryVersionApi.publicApiFile.parentFile, "current.txt"))

        // make sure to update current.txt if it wasn't previously planned to be updated
        val outputApiLocations: List<ApiLocation> = if (libraryVersionApi.publicApiFile.path.equals(currentTxtApi.publicApiFile.path)) {
            listOf(libraryVersionApi)
        } else {
            listOf(libraryVersionApi, currentTxtApi)
        }

        val builtApiLocation = ApiLocation.fromPublicApiFile(File(project.docsDir(), "release/${project.name}/current.txt"))

        var generateApi = project.tasks.register("generateApi", GenerateApiTask::class.java) { task ->
            task.apiLocation = builtApiLocation
            task.configuration = metalavaConfiguration
            task.dependsOn(metalavaConfiguration)
        }
        applyInputs(javaCompileInputs, generateApi)

        val checkApi = project.tasks.register("checkApi", CheckApiEquivalenceTask::class.java) { task ->
            task.builtApi = generateApi.get().apiLocation
            task.checkedInApis = outputApiLocations
            task.dependsOn(generateApi)
        }

        val lastReleasedApiFile = project.getRequiredCompatibilityApiLocation()
        if (lastReleasedApiFile != null) {
            val checkApiRelease = project.tasks.register("checkApiRelease", CheckApiCompatibilityTask::class.java) { task ->
                 task.configuration = metalavaConfiguration
                 task.apiLocation = lastReleasedApiFile
                 task.dependsOn(metalavaConfiguration)
             }
             applyInputs(javaCompileInputs, checkApiRelease)
             checkApi.dependsOn(checkApiRelease)
        }

        project.tasks.register("updateApi", UpdateApiTask::class.java) { task ->
            task.inputApiLocation = generateApi.get().apiLocation
            task.outputApiLocations = checkApi.get().checkedInApis
            task.dependsOn(generateApi)
        }

        project.tasks.getByName("check").dependsOn(checkApi)
        project.rootProject.tasks.named(BUILD_ON_SERVER_TASK).dependsOn(checkApi)
    }
}
