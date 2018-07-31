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

import androidx.build.gradle.isRoot
import com.sun.org.apache.xpath.internal.operations.Bool
import org.gradle.BuildAdapter
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A utility class that can discover which files are changed based on git history.
 *
 * On build server, it depends on sha's passed by the build system.
 * In local builds, it checks the git log to discover which files are changed since the last
 * merge CL.
 */
class AffectedModuleDetector private constructor(
        private val rootProject: Project,
        private val logger: Logger?,
        // set this only for debugging
        private val ignoreUknownProjects : Boolean = false
) {
    private val git by lazy {
        GitClient(rootProject.projectDir, logger)
    }

    private val dependencyTracker by lazy {
        DependencyTracker(rootProject, logger)
    }

    private val allProjects by lazy {
        AffectedModuleList(rootProject.subprojects)
    }

    private val projectGraph by lazy {
        ProjectGraph(rootProject)
    }

    val affectedProjects by lazy {
        findLocallyAffectedProjects()
    }

    fun shouldInclude(project : Project) : Boolean {
        return (project.isRoot || affectedProjects.projects.contains(project)).also {
            logger?.info("checking whether i should include ${project.path} and my answer is $it")
        }
    }

    private fun findLocallyAffectedProjects(): AffectedModuleList {
        val lastMergeSha = git.findPreviousMergeCL() ?: return allProjects
        val changedFiles = git.findChangedFilesSince(
                sha = lastMergeSha,
                includeUncommitted = true)
        val containingProjects = changedFiles
                .map(::findContainingProject)
                .let {
                    if (ignoreUknownProjects) {
                        it.filterNotNull()
                    } else {
                        it
                    }
                }
        if (containingProjects.any { it == null }) {
            logger?.info("couldn't find containing file for some projects, returning ALL")
            logger?.info(
                    """
                        if i was going to check for what i've found, i would've returned
                        ${expandToDependants(containingProjects.filterNotNull())}
                    """.trimIndent()
            )
            return allProjects
        }
        val wearProjects = rootProject.subprojects.filter {
            it.name.contains("wear")
        }
        val allAffectedProjects = expandToDependants(containingProjects + wearProjects)
        @Suppress("UNCHECKED_CAST")
        return AffectedModuleList(allAffectedProjects)
    }

    private fun expandToDependants(containingProjects: List<Project?>): Set<Project> {
        return containingProjects.flatMapTo(mutableSetOf()) {
            dependencyTracker.findAllDependants(it!!)
        }
    }

    private fun findContainingProject(filePath: String): Project? {
        val project = projectGraph.findContainingProject(filePath)
        logger?.info("search result for $filePath resulted in ${project?.path}")
        return project
    }

    fun debug() {
        logger?.info(git.getLogs().toString())
        val lastMergeCL = git.findPreviousMergeCL()
        logger?.info("non merge CL: $lastMergeCL")
        lastMergeCL?.let {
            val findChangedFilesSince = git.findChangedFilesSince(lastMergeCL)
            logger?.info("files changed since last merge: $findChangedFilesSince")
        }
    }

    fun debugForBuildServer(
            repoPropFile: File?,
            appliedPropFile: File?
    ) {
        if (repoPropFile == null || appliedPropFile == null) {
            logger?.info("repo or applied prop is null, return")
            return
        }
        val buildShas = BuildPropParser.getShaForThisBuild(appliedPropFile, repoPropFile, logger)
        if (buildShas == null) {
            logger?.info("build sha is null")
            return
        }
        logger?.info("build shas: $buildShas")
        logger?.info("changes based on build props repo sha: ${git.findChangedFilesSince(
                buildShas.repoSha)}")
        logger?.info("changes based on build props build sha: ${git.findChangedFilesSince(
                buildShas.buildSha)}")
        logger?.info("changes based on build props build sha pre: ${git.findChangedFilesSince(
                "${buildShas.buildSha}^")}")
        logger?.info("changes based on build props repo sha pre: ${git.findChangedFilesSince(
                "${buildShas.repoSha}^")}")
        logger?.info("changes from repo to build shas:${git.findChangedFilesSince(
                buildShas.repoSha, buildShas.buildSha)}")
        logger?.info("changes from build to repo shas:${git.findChangedFilesSince(
                buildShas.buildSha, buildShas.repoSha)}")
    }

    companion object {
        // TODO don't use static, use a property in project instead
        private var INSTANCE: AffectedModuleDetector? = null
        private val LOCK = Any()
        private const val LOG_FILE_NAME = "affected_module_detector_log.txt"
        private var listeners = CopyOnWriteArrayList<(AffectedModuleDetector) -> Unit?>()
        @JvmStatic
        fun configure(gradle: Gradle, rootProject: Project) {
            // TODO
            // detect if called after projects are loaded
            val logger = ToStringLogger.createWithLifecycle(gradle) { log ->
                val distDir = rootProject.properties["distDir"] as File?
                distDir?.let {
                    val outputFile = it.resolve(LOG_FILE_NAME)
                    outputFile.writeText(log)
                    println("wrote dependency log to ${outputFile.absolutePath}")
                }
            }
            gradle.addBuildListener(object : BuildAdapter() {
                override fun settingsEvaluated(settings: Settings?) {
                    logger.lifecycle("settings evaluated")
                }

                override fun projectsLoaded(gradle: Gradle?) {
                    logger.lifecycle("projects loaded")
                }

                override fun projectsEvaluated(gradle: Gradle?) {
                    logger.lifecycle("projects evaluated")
                    synchronized(LOCK) {
                        val detector = AffectedModuleDetector(
                                rootProject = rootProject,
                                logger = logger,
                                ignoreUknownProjects = true // TODO never merge this
                        ).also {
                            INSTANCE = it
                        }
                        logger.info("listener cnt: ${listeners.size}")
                        listeners.forEach {
                            logger.info("calling listeners")
                            it(detector)
                        }
                        listeners.clear()
                    }
                }
            })
        }

        @JvmStatic
        fun get(f : (AffectedModuleDetector) -> Unit?) {
            val detector = INSTANCE
            if (detector != null) {
                f(detector)
            } else {
                synchronized(LOCK) {
                    val detector = INSTANCE
                    if (detector != null) {
                        f(detector)
                    } else {
                        listeners.add(f)
                    }
                }
            }
        }

        @JvmStatic
        fun getOrThrow() : AffectedModuleDetector {
            val detector = INSTANCE ?: throw GradleException(
                    "canont get module detector until projects are laoded")
            return detector
        }
    }

    data class AffectedModuleList(val projects: Set<Project>)
}