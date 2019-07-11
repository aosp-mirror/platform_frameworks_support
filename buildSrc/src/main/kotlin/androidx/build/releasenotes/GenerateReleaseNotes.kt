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
        println(gitLog)
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

class Commit {
    var bugs: MutableList<Int> = mutableListOf()
    var sha: String = ""
    var authorEmail:String = ""
    var changeId: String = ""

}


fun getBugsFromCommit(gitCommit: String): MutableList<Int> {

}

fun getSHAFromCommit(gitCommit: String): String {

}

fun getChangeIdFromCommit(gitCommit: String): String {
    var listedCommit = gitCommit.split('\n')
    listedCommit.forEach {

    }

}

fun getAuthorEmailFromCommit(gitCommit: String): String {

}
