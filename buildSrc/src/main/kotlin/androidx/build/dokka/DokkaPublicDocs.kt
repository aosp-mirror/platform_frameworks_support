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

// This file sets up building public docs from source jars
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from DokkaPublicDocs to just publicDocs
package androidx.build.dokka

import androidx.build.AndroidXExtension
import androidx.build.RELEASE_RULE
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import androidx.build.androidJarFile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import java.io.File

object DokkaPublicDocs {
    val ARCHIVE_TASK_NAME: String = Dokka.archiveTaskNameForType("Public")

    val RUNNER_KOTLIN_TASK_NAME = Dokka.generatorTaskNameForType("Public", "Kotlin")
    val RUNNER_JAVA_TASK_NAME = Dokka.generatorTaskNameForType("Public", "Java")

    private const val UNZIP_DEPS_TASK_NAME = "unzipDokkaPublicDocsDeps"

    val hiddenPackages = listOf(
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
        "androidx.work.impl.utils.taskexecutor")

    fun tryGetRunnerProject(project: Project): Project? {
        return project.rootProject.findProject(":docs-runner")
    }

    fun getRunnerProject(project: Project): Project {
        return tryGetRunnerProject(project)!!
    }

    @Synchronized fun TaskContainer.registerDocsTask(runnerProject: Project) {
        val tasks = this

        runnerProject.tasks.findByName(RUNNER_KOTLIN_TASK_NAME)?.let {
            return
        }

        Dokka.registerDocsTasks("Public",
            runnerProject,
            hiddenPackages)
        val kotlinDocsTask = runnerProject.tasks.named(RUNNER_KOTLIN_TASK_NAME)
        val javaDocsTask = runnerProject.tasks.named(RUNNER_JAVA_TASK_NAME)
        val unzipTask = tasks.register(UNZIP_DEPS_TASK_NAME, LocateJarsTask::class.java)

        javaDocsTask.configure { it as DokkaAndroidTask
//            it.dependsOn(unzipTask)
            it.classpath += androidJarFile(runnerProject)

            unzipTask.map { locateJars ->
                for (jar in locateJars.outputJars) {
                    it.classpath = it.classpath.plus(runnerProject.file(jar))
                }
            }
        }

        kotlinDocsTask.configure { it as DokkaAndroidTask
//            it.dependsOn(unzipTask)
            it.classpath += androidJarFile(runnerProject)

            unzipTask.map { locateJars ->
                for (jar in locateJars.outputJars) {
                    it.classpath = it.classpath.plus(runnerProject.file(jar))
                }
            }
        }
    }

    // specifies that <project> exists and might need us to generate documentation for it
    fun registerProject(
        project: Project,
        extension: AndroidXExtension
    ) {
        if (tryGetRunnerProject(project) == null) {
            return
        }
        val projectSourcesLocationType = RELEASE_RULE.resolve(extension)?.strategy
        if (projectSourcesLocationType is Prebuilts) {
            val dependency = projectSourcesLocationType.dependency(extension)
            assignPrebuiltForProject(project, dependency)
        } else if (projectSourcesLocationType != null && projectSourcesLocationType !is Ignore) {
            throw Exception("Unsupported strategy " + projectSourcesLocationType +
                " specified for publishing public docs of project " + extension +
                "; must be Prebuilts or Ignore or null (which means Ignore)")
        }
    }

    // specifies that <project> has docs and that those docs come from a prebuilt
    private fun assignPrebuiltForProject(project: Project, dependency: String) {
        registerPrebuilt(dependency, getRunnerProject(project))
    }

    // specifies that <dependency> describes an artifact containing sources that we want to include in our generated documentation
    private fun registerPrebuilt(dependency: String, runnerProject: Project): TaskProvider<Copy> {

        runnerProject.tasks.registerDocsTask(runnerProject)

        val kotlinDocsTask = runnerProject.tasks.named(RUNNER_KOTLIN_TASK_NAME)
        val javaDocsTask = runnerProject.tasks.named(RUNNER_JAVA_TASK_NAME)

        // unzip the sources jar
        val unzipTask = getPrebuiltSources(runnerProject, "$dependency:sources")

        kotlinDocsTask.configure { it as DokkaAndroidTask
//            it.dependsOn(unzipTask)

            unzipTask.map { copy ->
                it.sourceDirs += copy.destinationDir
            }
        }

        javaDocsTask.configure { it as DokkaAndroidTask
//            it.dependsOn(unzipTask)
            unzipTask.map { copy ->
                it.sourceDirs += copy.destinationDir
            }
        }

        // also make a note to unzip any dependencies too
        runnerProject.tasks.named(UNZIP_DEPS_TASK_NAME).configure { it as LocateJarsTask
            it.inputDependencies.add(dependency)
        }

        return unzipTask
    }

    // returns a Copy task that provides source files for the given prebuilt
    private fun getPrebuiltSources(
        runnerProject: Project,
        mavenId: String
    ): TaskProvider<Copy> {
        val configuration = runnerProject.configurations.detachedConfiguration(
            runnerProject.dependencies.create(mavenId)
        )
        configuration.isTransitive = false
        try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            runnerProject.logger.error("DokkaPublicDocs failed to find prebuilts for $mavenId. " +
                    "specified in publichDocsRules.kt ." +
                    "You should either add a prebuilt sources jar, " +
                    "or add an overriding \"ignore\" rule into PublishDocsRules.kt")
            throw e
        }

        val sanitizedMavenId = mavenId.replace(":", "-")
        val buildDir = runnerProject.buildDir
        val destDir = runnerProject.file("$buildDir/sources-unzipped/$sanitizedMavenId")
        return runnerProject.tasks.register("unzip$sanitizedMavenId", Copy::class.java) {
            it.from(runnerProject.zipTree(configuration.singleFile)
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/META-INF/**")
                }
            )
            it.destinationDir = destDir
            // TODO(123020809) remove this filter once it is no longer necessary to prevent Dokka from failing
            val regex = Regex("@attr ref ([^*]*)styleable#([^_*]*)_([^*]*)$")
            it.filter { line ->
                regex.replace(line, "{@link $1attr#$3}")
            }
        }
    }
}

open class LocateJarsTask : DefaultTask() {
    // dependencies to search for .jar files
    @get:Input
    var inputDependencies: ListProperty<String> = project.objects.listProperty(String::class.java)

    // .jar files found in any dependencies
    @OutputFiles
    var outputJars = mutableListOf<File>()

    @TaskAction
    fun Extract() {
        // setup
        val inputDependencies = checkNotNull(inputDependencies.get()) {
            "inputDependencies not set"
        }
        val project = this.project

        // resolve the dependencies
        val dependenciesArray = inputDependencies.map {
            project.dependencies.create(it)
        }.toTypedArray()

        val artifacts = project.configurations.detachedConfiguration(*dependenciesArray)
                        .resolvedConfiguration.resolvedArtifacts

        // save the .jar files
        val files = artifacts.map({ artifact -> artifact.file })
        val jars = files.filter({ file -> file.name.endsWith(".jar") })
        outputJars.addAll(jars)

        // extract the classes.jar from each .aar file
        val aars = files.filter({ file -> file.name.endsWith(".aar") })
        for (aar in aars) {
            val tree = project.zipTree(aar)
            val classesJar = tree.matching { filter: PatternFilterable ->
                filter.include("classes.jar")
            }.single()
            outputJars.plusAssign(classesJar)
        }
    }
}
