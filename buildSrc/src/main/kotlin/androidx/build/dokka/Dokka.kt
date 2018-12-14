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

// This file creates tasks for generating documentation from source code using Dokka
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from Dokka to just Docs
package androidx.build.dokka

import androidx.build.DiffAndDocs
import androidx.build.Release
import androidx.build.SupportLibraryExtension
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import androidx.build.lazyDependsOn
import androidx.build.maybeRegister
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaVersion
import org.jetbrains.dokka.gradle.PackageOptions
import java.io.File

object Dokka {
    private val RUNNER_TASK_NAME = "dokka"
    public val ARCHIVE_TASK_NAME: String = "distDokkaDocs"

    private val hiddenPackages = listOf(
            "androidx.core.internal",
            "androidx.preference.internal",
            "androidx.wear.internal.widget.drawer",
            "androidx.webkit.internal",
            "androidx.work.impl",
            "androidx.work.impl.background",
            "androidx.work.impl.background.systemalarm",
            "androidx.work.impl.background.systemjob",
            "androidx.work.impl.constraints",
            "androidx.work.impl.constraints.controllers",
            "androidx.work.impl.constraints.trackers",
            "androidx.work.impl.model",
            "androidx.work.impl.utils",
            "androidx.work.impl.utils.futures",
            "androidx.work.impl.utils.taskexecutor"
    )

    fun getDocsTask(project: Project): TaskProvider<DokkaTask> {
        return project.rootProject.getOrCreateDocsTask()
    }

    fun Project.getOrCreateDocsTask(): TaskProvider<DokkaTask> {
        if (DokkaVersion.version == null) {
            DokkaVersion.loadFrom(
                    DokkaPlugin::class.java.getResourceAsStream(
                            "/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))
        }

        val dokkaTask = maybeRegister<DokkaTask>(
                name = Dokka.RUNNER_TASK_NAME,
                onConfigure = { docsTask ->
                    docsTask.moduleName = project.name
                    docsTask.outputDirectory = File(project.buildDir, "dokka").absolutePath
                    docsTask.outputFormat = "dac"
                    for (hiddenPackage in hiddenPackages) {
                        val opts = PackageOptions()
                        opts.prefix = hiddenPackage
                        opts.suppress = true
                        docsTask.perPackageOptions.add(opts)
                    }
                },
                onRegister = {
                })
        project.maybeRegister<Zip>(
                name = ARCHIVE_TASK_NAME,
                onConfigure = { task ->
                    task.description =
                            "Generates documentation artifact for pushing to developer.android.com"
                    task.from(dokkaTask.map {
                        it.outputDirectory
                    })
                    task.baseName = "android-support-dokka-docs"
                    task.version = getBuildId()
                    task.destinationDir = project.getDistributionDirectory()
                },
                onRegister = {
                    it.lazyDependsOn(dokkaTask)
                }
        )
        return dokkaTask
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        getDocsTask(project).configure { dokkaTask ->
            library.libraryVariants.all { variant ->
                if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                    project.afterEvaluate {
                        val inputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                        registerInputs(dokkaTask, inputs, project)
                    }
                }
            }
            DiffAndDocs.get(project).registerPrebuilts(extension)
        }
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        getDocsTask(project).configure { dokkaTask ->
            val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
            val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
            project.afterEvaluate {
                val inputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
                registerInputs(dokkaTask, inputs, project)
            }
            DiffAndDocs.get(project).registerPrebuilts(extension)
        }
    }

    private fun registerInputs(dokkaTask: DokkaTask, inputs: JavaCompileInputs, project: Project) {
        dokkaTask.sourceDirs += inputs.sourcePaths
        dokkaTask.classpath =
                dokkaTask.classpath.plus(inputs.dependencyClasspath).plus(inputs.bootClasspath)
        dokkaTask.dependsOn(inputs.dependencyClasspath)
    }
}
