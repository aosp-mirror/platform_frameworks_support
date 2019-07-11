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

package androidx.build.releasenotes

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Task for verifying the androidx dependency-stability-suffix rule
 * (A library is only as stable as its lease stable dependency)
 */
open class GenerateReleaseNotes : DefaultTask() {

    init {
        group = "Documentation"
        description = "Task for creating release notes for a specific library"
    }

    var startSHA: String = ""
    var endSHA: String = ""

    /**
     * <Task description>
     */
    @TaskAction
    fun createReleaseNotes() {
        if (!project.hasProperty("startSHA")) {
            throw RuntimeException("The generate release notes task need a start SHA from which" +
                    "to start generating release notes.  You can pass it a start SHA by" +
                    "adding the argument -PstartSHA=<yourSHA>")
        } else {
            startSHA =  project.property("startSHA").toString()
        }
        if (project.hasProperty("endSHA")) {
            endSHA =  project.property("endSHA").toString()
        } else {
            endSHA = "HEAD"
        }
        println(project.rootDir)
        println(project.projectDir)
        println("git log $startSHA..$endSHA ${project.projectDir}")
        var gitLog: String? = "git log $startSHA..$endSHA ${project.projectDir}".runCommand(project.projectDir)
        var commitsString = gitLog?.split("commit ")
        var commits: MutableList<Commit> = mutableListOf()
        var i = 0
        commitsString?.forEach { gitCommit ->
            val gitCommitFixed = "commit " + gitCommit
            commits.add(Commit(gitCommitFixed))
        }
    }

    fun String.runCommand(workingDir: File): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            proc.waitFor(1, TimeUnit.SECONDS)
            return proc.inputStream.bufferedReader().readText()
        } catch(e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}

/*
commit c235d1797718ac89c4ee2f977649c038f29bcc09
Author: WenHung_Teng <wenhungteng@google.com>
Date:   Tue Jun 25 15:00:29 2019 +0800

    Callback onError when capture stage count invalid.

    (1) Invoke imageCaptureRequests.mListener.onError when capture stage
    count invalid.
    (2) Add related test for the capture stage related logic.

    Bug: 133343581
    Test: ./gradlew bOS &&
    ./gradlew camera:camera-core:connectedAndroidTest &&
    ./gradlew camera:camera-camera2:connectedAndroidTest

    Change-Id: Iecc8d5e5c59a6be8036a8ca7319f63da3e3502e3

 */

class Commit(gitCommit: String) {
    var bugs: MutableList<Int> = mutableListOf()
    var sha: String = ""
    var authorEmail:String = ""
    var changeId: String = ""

    init {
        populate(gitCommit)
    }

    fun populate(gitCommit: String) {
        var listedCommit: List<String> = gitCommit.split('\n')
        listedCommit.forEach { line ->
            if ("commit" in line) {
                getSHAFromGitLine(line)
            }
            if ("Change-Id:" in line) {
                getChangeIdFromGitLine(line)
            }
            if ("Author:" in line) {
                getAuthorEmailFromGitLine(line)
            }
            if ("Bug:" in line ||
                "b/" in line ||
                "bug:" in line ||
                "Fixes:" in line ||
                "fixes b/" in line
            ) {
                getBugsFromGitLine(line, bugs)
            }
        }
    }


    fun getSHAFromGitLine(line: String) {
        /* commit c235d1797718ac89c4ee2f977649c038f29bcc09 */
        sha = line.substringAfter("commit")
        sha = sha.trim()
    }

    fun getChangeIdFromGitLine(line: String) {
        /* Change-Id: Iecc8d5e5c59a6be8036a8ca7319f63da3e3502e3 */
        changeId = line.substringAfter("Change-Id:")
        changeId = changeId.trim()
    }

    fun getAuthorEmailFromGitLine(line: String) {
        /* Author: First Last <email@google.com> */
        authorEmail = line.substringAfter('<')
        authorEmail = authorEmail.substringBefore('>')
        authorEmail = authorEmail.trim()
    }


    fun getBugsFromGitLine(line: String, bugList: MutableList<Int>) {
        line.replace("b/", " ")
        line.replace(":", " ")
        var words: List<String> = line.split(' ')
        words.forEach { word ->
            var possibleBug: Int? = word.toIntOrNull()
            if (possibleBug != null && possibleBug > 1000) {
                bugList.add(possibleBug)
            }
        }
    }


}