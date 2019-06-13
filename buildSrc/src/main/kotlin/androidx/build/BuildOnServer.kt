/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.build.jetpad.LibraryBuildInfoFile
import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException
import java.util.ArrayList

/**
 * Task for building all of Androidx libraries and documentation
 *
 * AndroidXPlugin configuration adds dependencies to BuildOnServer for all of the tasks that
 * produce artifacts that we want to build on server builds
 * When BuildOnServer executes, it double-checks that all expected artifacts were built
 */
open class BuildOnServer : DefaultTask() {

    init {
        group = "Build"
        description = "Builds all of the Androidx libraries and documentation"
    }

    @InputFiles
    fun getRequiredFiles(): List<File> {
        val distributionDirectory = project.getDistributionDirectory()
        val buildId = getBuildId()

        val filesNames = mutableListOf(
        // TODO: re-add after merge to compose merge to master
        // "android-support-public-docs-$buildId.zip",
        // "dokkaPublicDocs-$buildId.zip",
        "android-support-tipOfTree-docs-$buildId.zip",
        "dokkaTipOfTreeDocs-$buildId.zip",

        "gmaven-diff-all-$buildId.zip",
        "top-of-tree-m2repository-all-$buildId.zip")

        if (project.findProject(":jetifier-standalone") != null) {
            filesNames.add("jetifier-standalone.zip")
            filesNames.add("top-of-tree-m2repository-partially-dejetified-$buildId.zip")
        }

        return filesNames.map { fileName -> File(distributionDirectory, fileName) }
    }

    @TaskAction
    fun checkAllBuildOutputs() {

        val missingFiles = mutableListOf<String>()
        getRequiredFiles().forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.name)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
        }
    }

    @OutputFile
    val outputFile = File(project.getDistributionDirectory(),
        getAndroidxAggregateBuildInfoFilename())

    private fun getAndroidxAggregateBuildInfoFilename(): String {
        return "androidx_aggregate_build_info.txt"
    }

    private data class AllLibraryBuildInfoFiles(
        val artifacts: ArrayList<LibraryBuildInfoFile>
    )

    /**
     * Reads in file and checks that json is valid
     */
    private fun jsonFileIsValid(jsonFile: File, artifactList: MutableList<String>): Boolean {
        if (!jsonFile.exists()) {
            return(false)
        }
        val gson = Gson()
        val jsonString: String = jsonFile.readText(Charsets.UTF_8)
        val aggregateBuildInfoFile = gson.fromJson(jsonString, AllLibraryBuildInfoFiles::class.java)
        aggregateBuildInfoFile.artifacts.forEach { artifact ->
            if (!(artifactList.contains("${artifact.groupId}_${artifact.artifactId}"))) {
                println("Failed to find ${artifact.artifactId} in artifact list!")
                return false
            }
        }
        return true
    }

    /**
     * Create the output file to contain the final complete AndroidX project build info graph
     * file.  Iterate through the list of project-specific build info files, and collects
     * all dependencies as a JSON string. Finally, write this complete dependency graph to a text
     * file as json list of every project's build information
     */
    @TaskAction
    fun createAndroidxAggregateBuildInfoFile() {
        // Loop through each file in the list of libraryBuildInfoFiles and collect all build info
        // data from each of these $groupId-$artifactId-_build_info.txt files
        var output = StringBuilder()
        output.append("{ \"artifacts\": [\n")
        var artifactList = mutableListOf<String>()
        val distFiles = project.getDistributionDirectory().listFiles()
        distFiles.filter {
            ((it.isFile and (it.name != outputFile.name))
                    and (it.name.contains("_build_info.txt")))
        }.forEach { file ->
            println(file)
            var fileText: String = file.readText(Charsets.UTF_8)
            output.append("$fileText,")
            artifactList.add(file.name.replace("_build_info.txt", ""))
        }
        // Remove final ',' from list (so a null object doesn't get added to list
        output.setLength(output.length - 1)
        output.append("]}")
        outputFile.writeText(output.toString(), Charsets.UTF_8)
        if (!jsonFileIsValid(outputFile, artifactList)) {
            throw RuntimeException("JSON written to $outputFile was invalid.")
        }
    }
}
