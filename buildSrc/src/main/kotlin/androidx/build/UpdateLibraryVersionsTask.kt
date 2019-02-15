/*
 * Copyright (C) 2019 The Android Open Source Project
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
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.InputStream
import java.net.URL


/**
 * Task for checking Google Maven and determining which libraries are in the correct dev versions
 * and which libraries are outdated.
 */
open class VerifyLibraryVersion : DefaultTask() {

    init {
        group = "Versions"
        description = "Checks GMaven to verify library version not outdated"
    }

    // Returns true if the URL specified by the urlStr throws a 404
    private fun urlThrows404(urlStr: String): Boolean {
        val url = URL(urlStr)
        var urlThrows404 = false
        var inputStream: InputStream? = null
        try {
            /* Now read the retrieved document from the stream. */
            inputStream = url.openStream()
        } catch (e: java.io.FileNotFoundException) {
            urlThrows404 = true
        } finally {
            inputStream?.close()
        }
        return urlThrows404
    }

    var GMAVEN_BASE_URL = "https://dl.google.com/dl/android/maven2/"

    @TaskAction
    fun verifyProjectVersion() {
        println("${project.group}:${project.name}:${project.version}")
        val projectMavenDirName = "${project.group.toString().replace('.','/')}/${project.name}/${project.version}"
        val pomfileName = "${project.name}-${project.version}.pom"
        val mavenURLStr = "$GMAVEN_BASE_URL$projectMavenDirName/$pomfileName"
        println(mavenURLStr)

        println(project.rootDir)

        if (urlThrows404(mavenURLStr)) {
            println("Version is up to date")
        } else {
            println("VERSION IS OUT OF DATE")
        }
    }

    private fun increment_alpha_beta_version(version: String): String {
        // Only increment alpha and beta versions.
        // rc and stable should never need to be incremented in the androidx-master-dev branch
        // Suffix changes should be done manually.
        if ("alpha" in version || "beta" in version) {
            // Assure we don't violate a version naming policy
            val suffixInt = version.substring(version.length-3, version.length-1)
            val isInt = suffixInt.toIntOrNull()
            var newVersion: String
            when(isInt) {
                null -> {
                    var newVersionInt = version.substring(version.length-2).toInt() + 1
                    val formattedVersion = "%02d".format(newVersionInt)
                    newVersion = version.substring(0, version.length-2) + formattedVersion
                }
                else -> {
                    var newVersionInt = version.substring(version.length-3).toInt() + 1
                    val formattedVersion = "%02d".format(newVersionInt)
                    newVersion = version.substring(0, version.length-3) + formattedVersion
                }
            }
            return newVersion
        }
        else {
            return version
        }
    }


    @TaskAction
    fun incrementDependencyVersion() {
        var libraryVersionsLines = File("${project.rootDir}/buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt").readLines()
        for (i in 0..(libraryVersionsLines.size-1)) {
            val currLine = libraryVersionsLines[i]
            if (currLine.contains("val ") &&
                currLine.contains("Version(") &&
                currLine.contains(project.group.toString().removePrefix("androidx."), true)) {
                println(currLine)

                var start = currLine.indexOf("Version(\"") + "Version(\"".length
                println(increment_alpha_beta_version(project.version.toString()))

                // println(currLine.removePrefix("    val "))
//                var lineSplit = currLine.split("val")[1].trim()
//                var groupId = lineSplit.split('=')[0].trim()
//                var versionPre = lineSplit.split('=')[1].trim()
//                var versionPre2 = versionPre.removePrefix("Version(")
//                var version =  versionPre2.replace("()''\"\"", "")
//                println("$groupId : $version : ${project.version}")

            }
        }
    }
}


