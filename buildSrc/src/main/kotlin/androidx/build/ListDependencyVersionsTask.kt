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

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.StringBuilder
import java.util.concurrent.locks.ReentrantLock

/**
 * Task for a json file of all dependencies for each artifactId
 */
open class ListProjectDependencyVersionsTask : DefaultTask() {

    init {
        group = "Help"
        description = "Creates a json file of the dependency graph in developer/dependencyGraph"
    }

    data class ArtifactDependency(
        val artifactId: String,
        val groupId: String,
        val version: String,
        val isProjectDependency: Boolean)
    data class Artifact(
        val artifactId: String,
        val groupId: String,
        val version: String)
    {
        val prebuiltDependencies: MutableList<ArtifactDependency> = mutableListOf()
        val projectDependency: MutableList<ArtifactDependency> = mutableListOf()
    }


    private fun writeJsonToFile(artifact: Artifact) {
        // Create dependencyGraph folder and file in build directory of project
        var outBuildDir = File(project.buildDir.toString() + "/dependencyGraph")
        if (!outBuildDir.exists()) {
            if (!outBuildDir.mkdirs()){
                println("Failed to create output directory: $outBuildDir")
                return
            }
            // Sanity Check to confirm directory exists
            if (!outBuildDir.exists()) {
                println("Failed to create output directory: ${outBuildDir}")
                return
            }
        }
        val depGraphFile = File("${outBuildDir}/${project.name}-dependency-graph.json")
        // Create json object from the artifact instance
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonArtifact: String = gson.toJson(artifact)
        if (!depGraphFile.exists()) {
            if (!depGraphFile.createNewFile()) {
                println("Failed to find create output dependency graph file: $depGraphFile")
                return
            }
        }
        depGraphFile.writeText("$jsonArtifact\n")
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

/**
 * Task for a json file of all dependencies for each artifactId
 */
open class DependencyGraphFileTask : DefaultTask() {

    init {
        group = "Help"
        description = "Creates a json file of the dependency graph in developer/dependencyGraph"
    }

    private data class DependencyGraph(
        val artifacts: MutableList<ListProjectDependencyVersionsTask.Artifact>
    )

    /**
     * Reads in file and checks that json is valid
     */
    private fun jsonFileIsValid(jsonFile: File): Boolean {
        if (!jsonFile.exists()) {
            return(false)
        }
        var gson = Gson()

        var jsonString: String = jsonFile.readText(Charsets.UTF_8)
        var depGraph = gson.fromJson(jsonString, DependencyGraph::class.java)
        if (depGraph.artifacts.size > 20) {
            return(true)
        } else {
            return(false)
        }
    }


    /**
     * Iterate through each configuration of the project and build the set of all dependencies.
     * Then add each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * write these dependencies to a json file.
     */
    @TaskAction
    fun createDependencyGraphFile() {
        // Create dependencyGraph folder and total output file in support/build directory
        val depGraphDir = File(project.buildDir.parentFile.toString() + "/dependencyGraph")
        if (!depGraphDir.exists()) {
            if (!depGraphDir.mkdirs()){
                println("Failed to find create dependency Graph directory: $depGraphDir")
                return
            }
        }
        val depGraphFile = File("$depGraphDir/AndroidXDependencyGraph.json")
        if (!depGraphFile.exists()) {
            if (!depGraphFile.createNewFile()) {
                println("Failed to create output dependency graph file: $depGraphFile")
                return
            }
        }

        // Loop through each file in the out/host support directory and collect all dependency
        // graph data from each $project-dependency-graph.json file
        var output = StringBuilder()
        output.append("{ \"artifacts\": [\n")
        val supportOutDir = (project.buildDir.parentFile)
        supportOutDir.walk().filter {(
                (it.isFile and (it.name != depGraphFile.name) )
                and (it.name.contains("-dependency-graph.json"))
            )}.forEach { file ->
                println(file.name)
                var fileText: String = file.readText(Charsets.UTF_8)
                output.append("$fileText,")
        }
        output.append("]}")
        depGraphFile.writeText(output.toString(), Charsets.UTF_8)
        if (!jsonFileIsValid(depGraphFile)){
            println("JSON written to $depGraphFile was invalid.")
        }
    }
}
