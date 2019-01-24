/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Task for a json file of all dependencies for each artifactId
 */
open class ListDependencyVersionsTask : DefaultTask() {

    init {
        group = "Help"
        description = "Creates a json file of the dependency graph in developer/dependencyGraph"
    }

    private data class ArtifactDependency(
        val artifactId: String,
        val groupId: String,
        val version: String,
        val isProjectDependency: Boolean)
    private data class Artifact(
        val artifactId: String,
        val groupId: String,
        val version: String)
    {
        val prebuiltDependencies: MutableList<ArtifactDependency> = mutableListOf()
        val projectDependency: MutableList<ArtifactDependency> = mutableListOf()
    }


    private fun writeJsonToFile(artifact: Artifact) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonArtifact: String = gson.toJson(artifact)
        val depGraphFile = File("./development/dependencyGraph/${project.name}.json")
        if (!depGraphFile.parentFile.exists()) { return }
        if (!depGraphFile.exists()){  depGraphFile.createNewFile() }
        FileOutputStream(depGraphFile, true).bufferedWriter().use { writer ->
            writer.write("$jsonArtifact,\n")
        }
    }


    /**
     * Iterate through each configuration of the project and build the set of all dependencies.
     * Then add each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * write these dependencies to a json file.
     */
    @TaskAction
    fun listDependencyVersions() {

        val dependencySet: MutableSet<String> = mutableSetOf()
        val artifact = Artifact(project.name.toString(), project.group.toString(), project.version.toString())
        project.configurations.all { configuration ->
            configuration.allDependencies.forEach { dep ->
                // Only consider androidx dependencies
                if (dep.group != null &&
                    dep.group.toString().startsWith("androidx.") &&
                    !dep.group.toString().startsWith("androidx.test")) {
                        val depString: String = "${dep.group}:${dep.name}:${dep.version}"
                        if (!(dependencySet.contains(depString))) {
                            if (dep is DefaultProjectDependency) {
                                artifact.projectDependency.add(
                                    ArtifactDependency(
                                        dep.name.toString(),
                                        dep.group.toString(),
                                        dep.version.toString(),
                                        true)
                                )
                                dependencySet.add(depString)
                            } else if (dep is DefaultExternalModuleDependency) {
                                artifact.prebuiltDependencies.add(
                                    ArtifactDependency(
                                        dep.name.toString(),
                                        dep.group.toString(),
                                        dep.version.toString(),
                                        false)
                                )
                                dependencySet.add(depString)
                            }
                        }
                }
            }
        }
        writeJsonToFile(artifact)
    }
}

open class IterateOverProjectsTask : DefaultTask() {
    @TaskAction
    fun collectDependencyData() {

    }

}
