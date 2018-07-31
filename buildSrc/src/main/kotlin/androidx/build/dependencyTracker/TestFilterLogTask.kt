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

package androidx.build.dependencyTracker

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Logging task that uses a [DependencyTracker] to discover which files are changed in current
 * setup and logs the information about which modules should be built & tested.
 *
 * This is a temporary task to extract logging information from builds.
 */
open class TestFilterLogTask : DefaultTask() {
    @OutputFile
    private lateinit var outputFile: File
    @InputDirectory
    private lateinit var gitRootFolder: File
    @Optional
    @InputFile
    private var repoPropFile: File? = null
    @Optional
    @InputFile
    private var appliedPropFile: File? = null
    @OutputFile
    @Optional
    private var distLogFile: File? = null

    @TaskAction
    fun prepareLog() {
        val prevLevel = logging.level
        logging.level = LogLevel.DEBUG
        val myLogBuilder = StringBuilder()
        val myLogger = StandardOutputListener { p0 -> myLogBuilder.append(p0) }
        logging.addStandardOutputListener(myLogger)
        logging.addStandardErrorListener(myLogger)
        DependencyTracker(
                rootProject = project.rootProject,
                logger = logger).also {
            it.debug()
            it.debugForBuildServer(
                    repoPropFile = repoPropFile,
                    appliedPropFile = appliedPropFile
            )
        }
        logging.removeStandardErrorListener(myLogger)
        logging.removeStandardOutputListener(myLogger)
        distLogFile?.let {
            println("writing to dist file: ${it.absolutePath}")
            it.writeText(myLogBuilder.toString())
        }
        prevLevel?.let {
            // hmm might be null and it does not like it when null
            logging.level = it
        }
        throw GradleException("fail build to end fast")
    }

    /**
     * Config action that configures the task when necessary.
     */
    class ConfigAction(
            private val outputFile: File,
            private val gitRootFolder: File,
            private val repoPropFile: File?,
            private val appliedPropFile: File?,
            private val distLogFile: File?) : Action<TestFilterLogTask> {
        override fun execute(task: TestFilterLogTask) {
            task.outputFile = outputFile
            task.gitRootFolder = gitRootFolder
            task.repoPropFile = repoPropFile
            task.appliedPropFile = appliedPropFile
            task.distLogFile = distLogFile
        }
    }

    companion object {
        @JvmStatic
        fun configure(rootProject: Project, name: String): Task {
            val distDir = rootProject.properties["distDir"] as File?
            return rootProject.tasks.create(
                    name,
                    TestFilterLogTask::class.java,
                    ConfigAction(
                            outputFile = File(rootProject.buildDir, "changes_log.txt"),
                            gitRootFolder = rootProject.projectDir,
                            repoPropFile = distDir?.resolve("repo.prop"),
                            appliedPropFile = distDir?.resolve("applied.prop"),
                            distLogFile = distDir?.resolve("dep_resolver_log.txt")
                    )
            )
        }
    }
}