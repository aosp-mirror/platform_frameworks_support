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
import androidx.build.SupportLibraryExtension
import androidx.build.androidJarFile
import androidx.build.checkapi.getCurrentApiFile
import androidx.build.checkapi.getRequiredCompatibilityApiFile
import androidx.build.checkapi.hasApiFolder
import androidx.build.checkapi.hasApiTasks
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getPlugin

object Metalava {
    private fun Project.getOrCreateMetalavaConfiguration(): Configuration {
        return configurations.findByName("metalava")
            ?: configurations.create("metalava") {
                val dependency =
                    dependencies.create("com.android:metalava:1.1.2-SNAPSHOT:shadow@jar")
                it.dependencies.add(dependency)
            }
    }

    fun registerAndroidProjectApiStubs(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }

        val metalavaConfiguration = project.getOrCreateMetalavaConfiguration()

        library.libraryVariants.all { variant ->
            if (variant.name == "release") {
                val doesNotHaveKotlin: (Task) -> Boolean = {
                    project.files(variant.sourceSets.flatMap { it.javaDirectories })
                        .asFileTree
                        .files
                        .filter { it.extension == "kt" }
                        .isEmpty()
                }

                val workingDir = project.buildDir.resolve("intermediates/stubs/${variant.name}/")

                val apiStubsSourceDir = workingDir.resolve("source/")
                val generateApiStubs = project.tasks.create(
                    "generate${variant.name.capitalize()}ApiStubs",
                    ApiStubsTask::class.java
                ).apply {
                    configuration = metalavaConfiguration
                    bootClasspath = library.bootClasspath
                    setVariant(variant)
                    apiStubsDir = apiStubsSourceDir

                    dependsOn(metalavaConfiguration)
                    onlyIf(doesNotHaveKotlin)
                }

                val apiStubsClasses = workingDir.resolve("classes/")
                val compileApiStubs = project.tasks.create(
                    "compile${variant.name.capitalize()}ApiStubs",
                    JavaCompile::class.java
                ).apply {
                    @Suppress("DEPRECATION")
                    val compileTask = variant.javaCompile

                    val generatedFiles = compileTask.source.asFileTree.files
                        .filter {"build/generated" in it.path }
                    source = project.files(generatedFiles, apiStubsSourceDir).asFileTree

                    classpath = compileTask.classpath
                    destinationDir = apiStubsClasses
                    options.compilerArgs = compileTask.options.compilerArgs.toMutableList()
                    options.bootstrapClasspath = compileTask.options.bootstrapClasspath
                    sourceCompatibility = compileTask.sourceCompatibility
                    targetCompatibility = compileTask.targetCompatibility

                    dependsOn(compileTask.dependsOn, generateApiStubs)
                    onlyIf(doesNotHaveKotlin)
                }

                val apiStubsJar = workingDir.resolve("api.jar")
                val packageApiStubs = project.tasks.create(
                    "package${variant.name.capitalize()}ApiStubs",
                    Zip::class.java
                ).apply {
                    inputs.dir(apiStubsClasses)
                    outputs.file(apiStubsJar)

                    from(apiStubsClasses)
                    destinationDir = apiStubsJar.parentFile
                    archiveName = apiStubsJar.name

                    dependsOn(compileApiStubs)
                    onlyIf(doesNotHaveKotlin)
                }

                variant.packageLibrary.apply {
                    inputs.file(apiStubsJar)
                    // TODO roll this out!
                    //from(apiStubsJar)
                    dependsOn(packageApiStubs)
                }
            }
        }
    }

    fun registerAndroidProjectApiChecks(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }

        val metalavaConfiguration = project.getOrCreateMetalavaConfiguration()

        library.libraryVariants.all { variant ->
            if (variant.name == "release") {
                if (!project.hasApiFolder()) {
                    project.logger.info(
                        "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
                    return@all
                }

                val apiTxt = project.getCurrentApiFile()

                val checkApi = project.tasks.create("checkApi", CheckApiTask::class.java) { task ->
                    task.configuration = metalavaConfiguration
                    task.bootClasspath = library.bootClasspath
                    task.setVariant(variant)
                    task.apiTxtFile = apiTxt

                    task.dependsOn(metalavaConfiguration)
                }
                project.tasks.getByName("check").dependsOn(checkApi)
                project.rootProject.tasks.getByName(BUILD_ON_SERVER_TASK).dependsOn(checkApi)

                project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
                    task.configuration = metalavaConfiguration
                    task.bootClasspath = library.bootClasspath
                    task.setVariant(variant)
                    task.apiTxtFile = apiTxt

                    task.dependsOn(metalavaConfiguration)
                }
            }
        }
    }

    fun registerJavaProjectApiChecks(
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

        val metalavaConfiguration = project.getOrCreateMetalavaConfiguration()

        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")

        val currentApiFile = project.getCurrentApiFile()

        val checkApi = project.tasks.create("checkApi", CheckApiTask::class.java) { task ->
            task.configuration = metalavaConfiguration
            task.bootClasspath = androidJarFile(project).files
            task.sourcePaths = mainSourceSet.allSource.srcDirs
            task.dependencyClasspath = mainSourceSet.compileClasspath
            task.apiTxtFile = currentApiFile

            task.dependsOn(metalavaConfiguration)
        }

       val lastReleasedApiFile = project.getRequiredCompatibilityApiFile()
       if (lastReleasedApiFile != null) {
           val checkApiRelease = project.tasks.create("checkApiRelease", CheckApiTask::class.java) { task ->
                task.configuration = metalavaConfiguration
                task.bootClasspath = androidJarFile(project).files
                task.sourcePaths = mainSourceSet.allSource.srcDirs
                task.dependencyClasspath = mainSourceSet.compileClasspath
                task.apiTxtFile = lastReleasedApiFile
                task.allowApiAdditions = true
    
                task.dependsOn(metalavaConfiguration)
            }
            checkApi.dependsOn(checkApiRelease)
        }

        project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
            task.configuration = metalavaConfiguration
            task.bootClasspath = androidJarFile(project).files
            task.sourcePaths = mainSourceSet.allSource.srcDirs
            task.dependencyClasspath = mainSourceSet.compileClasspath
            task.apiTxtFile = currentApiFile

            task.dependsOn(metalavaConfiguration)
        }

        project.tasks.getByName("check").dependsOn(checkApi)
        project.rootProject.tasks.getByName(BUILD_ON_SERVER_TASK).dependsOn(checkApi)
    }
}
