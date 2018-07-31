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

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitClient(
        private val workingDir: File,
        private val logger: Logger? = null) {
    @Throws(IOException::class)
    fun getLogs(): List<String> {
        return "git log --oneline -50".runCommand()
    }

    /**
     * Finds changed file paths since the given sha
     */
    fun findChangedFilesSince(sha: String, top:String = "HEAD"): List<String> {
        return "git diff --name-only HEAD $sha".runCommand()
    }

    /**
     * checks the history to find the first merge CL.
     */
    fun findPreviousMergeCL(): String? {
        return "git log -1 --merges --oneline"
                .runCommand()
                .firstOrNull()
                ?.split(" ")
                ?.firstOrNull()
    }

    private fun String.runCommand(): List<String> {
        val parts = this.split("\\s".toRegex())
        logger?.info("running command $this")
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        val response = proc.inputStream.bufferedReader().readLines().filterNot {
            it.isNullOrEmpty()
        }
        logger?.info("Response: ${response.joinToString(System.lineSeparator())}")
        return response
    }

    companion object {
        private val MERGE_REGEX = "\\w+ Merge (.*) into \\w+".toRegex()
    }

    data class BuildRange(
            val repoSha : String,
            val buildSha : String
    )
}