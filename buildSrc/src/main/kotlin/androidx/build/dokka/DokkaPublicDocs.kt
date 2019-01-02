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
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from DokkaPublicDocs to just PublicDocs
package androidx.build.dokka

import java.io.File
import androidx.build.androidJarFile
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import androidx.build.SupportLibraryExtension
import androidx.build.Release
import androidx.build.RELEASE_RULE
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.provider.Provider
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.PackageOptions
import androidx.build.DiffAndDocs


object DokkaPublicDocs {
    public val ARCHIVE_TASK_NAME: String = "distPublicDokkaDocs"
    private val RUNNER_TASK_NAME = "dokkaPublicDocs"
    private val UNZIP_DEPS_TASK_NAME = "unzipDokkaPublicDocsDeps"

    public val hiddenPackages = listOf(
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

    fun getRunnerProject(project: Project): Project {
        return project.rootProject.project(":docs-runner")
    }

    fun getDocsTask(project: Project): DokkaTask {
        var runnerProject = getRunnerProject(project)
        return runnerProject.tasks.getOrCreateDocsTask(runnerProject)
    }

    fun getUnzipDepsTask(project: Project): LocateJarsTask {
        var runnerProject = getRunnerProject(project)
        return runnerProject.tasks.getByName(DokkaPublicDocs.UNZIP_DEPS_TASK_NAME) as LocateJarsTask
    }

    @Synchronized fun TaskContainer.getOrCreateDocsTask(runnerProject: Project): DokkaTask {
        val tasks = this
        if (tasks.findByName(DokkaPublicDocs.RUNNER_TASK_NAME) == null) {
            Dokka.createDocsTask(DokkaPublicDocs.RUNNER_TASK_NAME, runnerProject, hiddenPackages, DokkaPublicDocs.ARCHIVE_TASK_NAME)
            val docsTask = runnerProject.tasks.getByName(DokkaPublicDocs.RUNNER_TASK_NAME) as DokkaTask
            tasks.create(DokkaPublicDocs.UNZIP_DEPS_TASK_NAME, LocateJarsTask::class.java) { unzipTask ->
                unzipTask.outputDirectory = runnerProject.file("${runnerProject.buildDir}/aars-unzipped/")
                unzipTask.doLast {
                    for (jar in unzipTask.foundJars) {
                        docsTask.classpath = docsTask.classpath.plus(runnerProject.file(jar))
                    }
                    docsTask.classpath += androidJarFile(runnerProject)
                }
                docsTask.dependsOn(unzipTask)
            }
	}
        val docsTask = runnerProject.tasks.getByName(DokkaPublicDocs.RUNNER_TASK_NAME) as DokkaTask
        return docsTask
    }

    // specifies that <project> exists and might need us to generate documentation for it
    fun registerProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        val projectSourcesLocationType = RELEASE_RULE.resolve(extension)?.strategy
        if (projectSourcesLocationType is Prebuilts) {
            val dependency = projectSourcesLocationType.dependency(extension)
            assignPrebuiltForProject(project, dependency)
        } else if (projectSourcesLocationType != null && projectSourcesLocationType !is Ignore) {
            throw Exception("Unsupported strategy " + projectSourcesLocationType + " specified for publishing public docs of project " + extension + "; must be Prebuilts or Ignore or null (which means Ignore)")
        }
    }

    // specifies that <project> has docs and that those docs come from a prebuilt
    private fun assignPrebuiltForProject(project: Project, dependency: String) {
        registerPrebuilt(dependency, getRunnerProject(project))
    }

    // specifies that <dependency> describes an artifact containing sources that we want to include in our generated documentation
    private fun registerPrebuilt(dependency: String, runnerProject: Project): Copy {
        val docsTask = getDocsTask(runnerProject)

        // unzip the sources jar
        val unzipTask = getPrebuiltSources(runnerProject, dependency + ":sources")
        val sourceDir = unzipTask.destinationDir
        docsTask.dependsOn(unzipTask)
        docsTask.sourceDirs += sourceDir

        // also make a note to unzip any dependencies too
        getUnzipDepsTask(runnerProject).inputDependencies.add(dependency)

        return unzipTask
    }

    // returns a Copy task that provides source files for the given prebuilt
    private fun getPrebuiltSources(
        runnerProject: Project,
        mavenId: String
    ): Copy {
        val configuration = runnerProject.configurations.detachedConfiguration(runnerProject.dependencies.create(mavenId))
        configuration.setTransitive(false)
        val artifacts = try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            runnerProject.logger.error("DokkaPublicDocs failed to find prebuilts for $mavenId. " +
                    "specified in PublichDocsRules.kt ." +
                    "You should either add a prebuilt sources jar, " +
                    "or add an overriding \"ignore\" rule into PublishDocsRules.kt")
            throw e
        }

        val sanitizedMavenId = mavenId.replace(":", "-")
        val destDir = runnerProject.file("${runnerProject.buildDir}/sources-unzipped/${sanitizedMavenId}")
        val unzipTask = runnerProject.tasks.create("unzip${sanitizedMavenId}" , Copy::class.java) { copyTask ->
            copyTask.from(runnerProject.zipTree(configuration.singleFile)
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/META-INF/**")
                }
            )
            copyTask.destinationDir = destDir
            // TODO(123020809) remove this filter once it is no longer necessary to prevent Dokka from failing
            val regex = Regex("@attr ref ([^*]*)styleable#([^_*]*)_([^*]*)$")
            copyTask.filter({ line ->
                regex.replace(line, "{@link $1attr#$3}")
            })
        }

        return unzipTask
    }

    private fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val docsTask = getDocsTask(project)
        docsTask.sourceDirs += inputs.sourcePaths
        docsTask.classpath = docsTask.classpath.plus(inputs.dependencyClasspath).plus(inputs.bootClasspath)
        docsTask.dependsOn(inputs.dependencyClasspath)
    }
}

open class LocateJarsTask : DefaultTask() {
    // dependencies to extract
    val inputDependencies = mutableListOf<String>()

    // directory to extract into
    var outputDirectory: File? = null

    val foundJars = mutableListOf<File>()

    @TaskAction
    fun Extract() {
        // setup
        val inputDependencies = checkNotNull(inputDependencies) { "inputDependencies not set" }
        val outputDirectory = checkNotNull(outputDirectory) { "outputDirectory not set" }
        val project = this.project
        if (outputDirectory.exists()) {
            // If this deleteAll ever gets removed, we should double-check that any up-to-date checks of tasks depending on this one still work correctly
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()

        // resolve the dependencies
        var dependenciesArray = inputDependencies.map { project.dependencies.create(it) }.toTypedArray()
        val artifacts = project.configurations.detachedConfiguration(*dependenciesArray).resolvedConfiguration.resolvedArtifacts

        // save the .jar files
        val files = artifacts.map({ artifact -> artifact.file })
        var jars = files.filter({ file -> file.name.endsWith(".jar") })
        foundJars.addAll(jars)

        // extract the classes.jar from each .aar file
        var aars = files.filter({ file -> file.name.endsWith(".aar") })
        for (aar in aars) {
            val tree = project.zipTree(aar)
            val classesJar = tree.matching({ filter: PatternFilterable -> filter.include("classes.jar") }).single()
            foundJars.plusAssign(classesJar)
        }
    }

}
