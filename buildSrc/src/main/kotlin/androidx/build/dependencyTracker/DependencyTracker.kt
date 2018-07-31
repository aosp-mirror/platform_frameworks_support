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

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import java.io.File

class DependencyTracker(
        private val rootProject : Project,
        private val logger : Logger?) {
    private val gitClient by lazy {
        GitClient(workingDir = rootProject.projectDir,
                logger = rootProject.logger)
    }
    private val dependantList: Map<Project, Set<Project>> by lazy {
        val result = mutableMapOf<Project, MutableSet<Project>>()
        rootProject.subprojects.forEach { project ->
            project.configurations.forEach { config ->
                config
                        .dependencies
                        .filterIsInstance(ProjectDependency::class.java)
                        .forEach {
                            result.getOrPut(it.dependencyProject) { mutableSetOf() }
                                    .add(project)
                        }
            }
        }
        result
    }

    fun debug() {
        logger?.info(gitClient.getLogs().toString())
        val lastMergeCL = gitClient.findPreviousMergeCL()
        logger?.info("non merge CL: $lastMergeCL")
        lastMergeCL?.let {
            val findChangedFilesSince = gitClient.findChangedFilesSince(lastMergeCL)
            logger?.info("files changed since last merge: $findChangedFilesSince")
        }
    }

    fun debugForBuildServer(
            repoPropFile : File?,
            appliedPropFile : File?
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
        logger?.info("changes based on build props: ${gitClient.findChangedFilesSince(buildShas.repoSha)}")
    }



    fun findAllDependants(project: Project): Set<Project> {
        val result = mutableSetOf<Project>()
        fun addAllDependants(project: Project) {
            dependantList.get(project)?.forEach {
                if (result.add(it)) {
                    addAllDependants(it)
                }
            }
        }
        addAllDependants(project)
        return result
    }
}