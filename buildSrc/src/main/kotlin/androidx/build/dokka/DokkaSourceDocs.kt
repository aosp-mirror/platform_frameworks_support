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

import androidx.build.java.JavaCompileInputs
import androidx.build.AndroidXExtension
import androidx.build.Release
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaTask

object DokkaSourceDocs {
    private val JAVA_RUNNER_TASK_NAME = Dokka.generatorTaskNameForType("TipOfTree", "java")
    private val KOTLIN_RUNNER_TASK_NAME = Dokka.generatorTaskNameForType("TipOfTree", "kotlin")
    public val ARCHIVE_TASK_NAME: String = Dokka.archiveTaskNameForType("TipOfTree")
    // TODO(b/72330103) make "generateDocs" be the only archive task once Doclava is fully removed
    private val ALTERNATE_ARCHIVE_TASK_NAME: String = "generateDocs"

    private val hiddenPackages = DokkaPublicDocs.hiddenPackages

    fun tryGetRunnerProject(project: Project): Project? {
        return project.rootProject.findProject(":docs-runner")
    }

    fun getRunnerProject(project: Project): Project {
        return tryGetRunnerProject(project)!!
    }

    fun getDocsTasks(project: Project): List<DokkaTask> {
        val runnerProject = getRunnerProject(project)
        return runnerProject.tasks.getOrCreateDocsTask(runnerProject)
    }

    @Synchronized fun TaskContainer.getOrCreateDocsTask(runnerProject: Project): List<DokkaTask> {
        val tasks = this
        if (tasks.findByName(DokkaSourceDocs.KOTLIN_RUNNER_TASK_NAME) == null) {
            Dokka.createDocsTask("TipOfTree", runnerProject, hiddenPackages)
            if (tasks.findByName(DokkaSourceDocs.ALTERNATE_ARCHIVE_TASK_NAME) == null) {
                tasks.create(ALTERNATE_ARCHIVE_TASK_NAME)
            }
            tasks.getByName(ALTERNATE_ARCHIVE_TASK_NAME)
                .dependsOn(tasks.getByName(ARCHIVE_TASK_NAME))
        }
        return listOf(runnerProject.tasks.getByName(KOTLIN_RUNNER_TASK_NAME) as DokkaTask,
            runnerProject.tasks.getByName(JAVA_RUNNER_TASK_NAME) as DokkaTask)
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        if (tryGetRunnerProject(project) == null) {
            return
        }
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
    }

    fun registerJavaProject(
        project: Project,
        extension: AndroidXExtension
    ) {
        if (tryGetRunnerProject(project) == null) {
            return
        }
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
    }

    fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val (kotlinDocsTask, javaDocsTask) = getDocsTasks(project)

        kotlinDocsTask.sourceDirs += inputs.sourcePaths
        kotlinDocsTask.classpath = kotlinDocsTask.classpath.plus(inputs.dependencyClasspath)
            .plus(inputs.bootClasspath)
        kotlinDocsTask.dependsOn(inputs.dependencyClasspath)

        javaDocsTask.sourceDirs += inputs.sourcePaths
        javaDocsTask.classpath = javaDocsTask.classpath.plus(inputs.dependencyClasspath)
            .plus(inputs.bootClasspath)
        javaDocsTask.dependsOn(inputs.dependencyClasspath)
    }
}
