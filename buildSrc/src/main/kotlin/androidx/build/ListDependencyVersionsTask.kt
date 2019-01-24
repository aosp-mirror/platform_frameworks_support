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
        var outBuildDir = File(project.parent?.buildDir.toString() + "/dependencyGraph")
        while (outBuildDir.name != "support") {
            outBuildDir = outBuildDir.parentFile
            if (outBuildDir.name == "out") {
                return
            }
        }
        if (!outBuildDir.exists()) {
            if (!outBuildDir.mkdirs()){
                println("Failed to create output directory: $outBuildDir")
                return
            }
        }
        val depGraphFile = File("${outBuildDir}/${project.name}.json")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonArtifact: String = gson.toJson(artifact)
        if (!depGraphFile.parentFile.exists()) {
            println("Failed to find output directory: ${depGraphFile.parentFile}")
            return
        }
        if (!depGraphFile.exists()) {
            if (!depGraphFile.createNewFile()) {
                println("Failed to find create output file: $depGraphFile")
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


    /**
     * Iterate through each configuration of the project and build the set of all dependencies.
     * Then add each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * write these dependencies to a json file.
     */
    @TaskAction
    fun createDependencyGraphFile() {
        val depGraphDir = File(project.buildDir.toString() + "/dependencyGraph")
        val depGraphFile = File("$depGraphDir/AndroidXDependencyGraph.json")
        if (!depGraphDir.exists()) {
            println("Failed to find output dependency Graph directory: $depGraphDir")
            return
        }
        if (!depGraphFile.createNewFile()){
            println("Failed to create output dependency graph file: $depGraphFile")
            return
        }
        var output = StringBuilder()
        output.append("{ \"artifacts\": [\n")
        depGraphDir.walk().filter { it.isFile and (it.name != depGraphFile.name) }.forEach { file ->
            println(file.name)
            var fileText: String = file.readText(Charsets.UTF_8)
            output.append("$fileText,")
        }
        output.append("]}")
        depGraphFile.writeText(output.toString(), Charsets.UTF_8)
    }
}
