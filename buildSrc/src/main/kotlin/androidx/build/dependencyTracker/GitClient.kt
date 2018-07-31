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

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitClient(private val workingDir : File) {
    @Throws(IOException::class)
    fun getLogs() : List<String> {
        return "git log --oneline -10".runCommand()
    }

    fun findChangedFilesSince(sha : String) : List<String> {
        return "git diff --name-only HEAD $sha".runCommand()
    }

    fun findPreviousMergeCL() : String? {
        val logs = getLogs()
        log("got logs:", logs)
        // if first one is a merge, sth is wrong, return
        if (logs.isEmpty()) {
            return null
        }
        logs.forEach {
            log("is merge:", it, it.isMerge())
        }
        return logs.firstOrNull {
            it.isMerge()
        }?.split(" ")?.firstOrNull()
    }

    private fun log(vararg msgs : Any?) {
        println("GitClient: ${msgs.joinToString(" ")}")
    }

    private fun String.isMerge(): Boolean {
        return MERGE_REGEX.find(this) != null
    }

    private fun String.runCommand(): List<String> {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readLines().filterNot {
            it.isNullOrEmpty()
        }
    }

    companion object {
        private val MERGE_REGEX = "\\w+ Merge (.*) into \\w+".toRegex()
    }
}