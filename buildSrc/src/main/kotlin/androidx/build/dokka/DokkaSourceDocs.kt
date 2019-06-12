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
import androidx.build.dokka.DokkaSourceDocs.registerDocsTask
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidTask

object DokkaSourceDocs {
    val RUNNER_KOTLIN_TASK_NAME = Dokka.generatorTaskNameForType("TipOfTree", "Kotlin")
    val RUNNER_JAVA_TASK_NAME = Dokka.generatorTaskNameForType("TipOfTree", "Java")

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

    @Synchronized fun TaskContainer.registerDocsTask(runnerProject: Project) {
        val tasks = this
        runnerProject.tasks.findByName(RUNNER_KOTLIN_TASK_NAME)?.let {
            return
        }

        Dokka.registerDocsTasks("TipOfTree", runnerProject, hiddenPackages)

        val kotlinDocsTask = runnerProject.tasks.named(RUNNER_KOTLIN_TASK_NAME)
        val javaDocsTask = runnerProject.tasks.named(RUNNER_JAVA_TASK_NAME)

        if (tasks.findByName(ALTERNATE_ARCHIVE_TASK_NAME) == null) {
            tasks.register(ALTERNATE_ARCHIVE_TASK_NAME) {
                it.dependsOn(kotlinDocsTask)
                it.dependsOn(javaDocsTask)
            }
        }
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
        getRunnerProject(project).tasks.registerDocsTask(getRunnerProject(project))

        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                project.afterEvaluate {
                    val inputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                    registerInputs(inputs, project)
                }
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
        getRunnerProject(project).tasks.registerDocsTask(getRunnerProject(project))
        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        project.afterEvaluate {
            val inputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
            registerInputs(inputs, project)
        }
    }

    private fun registerInputs(inputs: JavaCompileInputs, project: Project) {

        val runnerProject = getRunnerProject(project)
        val kotlinDocsTask = runnerProject.tasks.named(RUNNER_KOTLIN_TASK_NAME)
        val javaDocsTask = runnerProject.tasks.named(RUNNER_JAVA_TASK_NAME)

        kotlinDocsTask.configure {
            it as DokkaAndroidTask
            it.sourceDirs += inputs.sourcePaths
            it.classpath += inputs.dependencyClasspath
            it.classpath += inputs.bootClasspath
//            it.dependsOn(inputs.dependencyClasspath)
        }

        javaDocsTask.configure {
            it as DokkaAndroidTask
            it.sourceDirs += inputs.sourcePaths
            it.classpath += inputs.dependencyClasspath
            it.classpath += inputs.bootClasspath
//            it.dependsOn(inputs.dependencyClasspath)
        }
    }
}
