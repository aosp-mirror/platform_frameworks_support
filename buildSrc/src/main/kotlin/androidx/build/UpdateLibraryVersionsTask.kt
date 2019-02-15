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

    @TaskAction
    fun incrementDependencyVersion() {
        var libraryVersionsLines = File("${project.rootDir}/buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt").readLines()
        for (i in 0..(libraryVersionsLines.size-1)) {
            val currLine = libraryVersionsLines[i]
            if ("val " in currLine) {
                println(currLine)
            }
        }
    }
}
