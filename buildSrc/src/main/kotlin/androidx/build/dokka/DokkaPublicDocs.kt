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
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import androidx.build.SupportLibraryExtension
import androidx.build.Release
import androidx.build.RELEASE_RULE
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import com.android.build.gradle.LibraryExtension
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.PackageOptions
import androidx.build.DiffAndDocs


object DokkaPublicDocs {
    private val RUNNER_TASK_NAME = "dokkaPublicDocs"
    public val ARCHIVE_TASK_NAME: String = "distPublicDokkaDocs"

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

    fun getDocsTask(project: Project): DokkaTask {
        return project.rootProject.findProject(":docs-fake")!!.getOrCreateDocsTask()
    }

    @Synchronized fun Project.getOrCreateDocsTask(): DokkaTask {
        val runnerProject = this
        if (runnerProject.tasks.findByName(DokkaPublicDocs.RUNNER_TASK_NAME) == null) {
            Dokka.createDocsTask(DokkaPublicDocs.RUNNER_TASK_NAME, runnerProject, hiddenPackages, DokkaPublicDocs.ARCHIVE_TASK_NAME)
        }
        return runnerProject.tasks.getByName(DokkaPublicDocs.RUNNER_TASK_NAME) as DokkaTask
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        registerPrebuiltForProject(project, extension)
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        registerPrebuiltForProject(project, extension)
    }

    fun registerPrebuiltForProject(project: Project, extension: SupportLibraryExtension) {
        val publishDocsRules = RELEASE_RULE
        val rule = publishDocsRules.resolve(extension)
        val strategy = rule?.strategy
        if (strategy is Prebuilts) {
            val dependency = strategy.dependency(extension)
            val docsTask = getDocsTask(project)
            val unzipTask = getPrebuiltSources(project.rootProject, dependency)
            docsTask.sourceDirs += listOf(unzipTask.destinationDir)
            docsTask.dependsOn(unzipTask)
        } else if (strategy != null && strategy !is Ignore) {
            throw Exception("Unsupported strategy " + strategy + " specified for publishing public docs of project " + extension + "; must be Prebuilts or Ignore or null (which means Ignore)")
        }
    }

    private fun getPrebuiltSources(
        root: Project,
        mavenId: String
    ): Copy {
        val configName = "sources_${mavenId.replace(":", "-")}"
        val configuration = root.configurations.create(configName)
        root.dependencies.add(configName, mavenId)

        val artifacts = try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            root.logger.error("DokkaPublicDocs failed to find prebuilts for $mavenId. " +
                    "specified in PublichDocsRules.kt ." +
                    "You should either add a prebuilt sources jar, " +
                    "or add an overriding \"ignore\" rule into PublishDocsRules.kt")
            throw e
        }

        val artifact = artifacts.find { it.moduleVersion.id.toString() == mavenId }
                ?: throw GradleException()

        val sourceDir = artifact.file.parentFile
        val tree = root.zipTree(File(sourceDir, "${artifact.file.nameWithoutExtension}-sources.jar"))
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/*.html")
                    it.exclude("**/*.kt")
                    it.exclude("**/META-INF/**")
                }
        val destDir = root.file("${root.buildDir}/sources-unzipped/${configName}")
        val unzipTask = root.tasks.create("unzip" + configName, Copy::class.java) { copyTask ->
            copyTask.from(tree)
            copyTask.destinationDir = destDir
        }
        return unzipTask
    }

}
