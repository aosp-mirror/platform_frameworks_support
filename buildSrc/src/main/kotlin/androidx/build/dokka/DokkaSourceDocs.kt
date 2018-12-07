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
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from DokkaSourceDocs to just SourceDocs
package androidx.build.dokka

import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import androidx.build.SupportLibraryExtension
import androidx.build.Release
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.PackageOptions
import androidx.build.DiffAndDocs

object DokkaSourceDocs {
    private val RUNNER_TASK_NAME = "dokkaTipOfTreeDocs"
    public val ARCHIVE_TASK_NAME: String = "distDokkaTipOfTreeDocs"

    private val hiddenPackages = DokkaPublicDocs.hiddenPackages

    fun getDocsTask(project: Project): DokkaTask {
        return project.rootProject.getOrCreateDocsTask()
    }

    @Synchronized fun Project.getOrCreateDocsTask(): DokkaTask {
        val runnerProject = this
        if (runnerProject.tasks.findByName(DokkaSourceDocs.RUNNER_TASK_NAME) == null) {
            Dokka.createDocsTask(DokkaSourceDocs.RUNNER_TASK_NAME, runnerProject, hiddenPackages, DokkaSourceDocs.ARCHIVE_TASK_NAME)
        }
        return runnerProject.tasks.getByName(DokkaSourceDocs.RUNNER_TASK_NAME) as DokkaTask
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
        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                project.afterEvaluate({
                    val inputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                    registerInputs(inputs, project)
                })
            }
        }
        DiffAndDocs.registerPrebuilts(extension)
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        project.afterEvaluate({
            val inputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
            registerInputs(inputs, project)
        })
        DiffAndDocs.registerPrebuilts(extension)
    }

    fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val docsTask = getDocsTask(project)
        docsTask.sourceDirs += inputs.sourcePaths
        docsTask.classpath = docsTask.classpath.plus(inputs.dependencyClasspath).plus(inputs.bootClasspath)
        docsTask.dependsOn(inputs.dependencyClasspath)
    }
}
