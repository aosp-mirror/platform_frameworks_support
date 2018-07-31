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

class DependencyTracker(private val rootProject : Project) {
    private val gitClient by lazy {
        GitClient(workingDir = rootProject.projectDir)
    }
    private val dependantList : Map<Project, Set<Project>> by lazy {
        val result = mutableMapOf<Project, MutableSet<Project>>()
        rootProject.subprojects.forEach { project ->
            project.configurations.forEach { config ->
                config
                        .dependencies
                        .filterIsInstance(ProjectDependency::class.java)
                        .forEach {
                            result.getOrPut(it.dependencyProject) {mutableSetOf()}
                                    .add(project)
                        }
            }
        }
        result
    }
    fun debug() {
        log(gitClient.getLogs())
        val lastMergeCL = gitClient.findPreviousMergeCL()
        log("non merge CL: $lastMergeCL")
        lastMergeCL?.let {
            val findChangedFilesSince = gitClient.findChangedFilesSince(lastMergeCL)
            log("files changed since last merge: $findChangedFilesSince")
        }

//        log(findAllDependants(rootProject.findProject(":room:room-compiler")!!))
    }

    fun findAllDependants(project: Project) : Set<Project> {
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

    companion object {
        fun log(msg : Any?) {
            println("DT: $msg")
        }
    }
}