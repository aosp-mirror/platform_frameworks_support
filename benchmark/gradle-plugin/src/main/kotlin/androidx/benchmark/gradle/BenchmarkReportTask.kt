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

package androidx.benchmark.gradle

import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class BenchmarkReportTask @Inject constructor(private val adbPath: String) :
    BenchmarkBaseTask() {
    private val benchmarkReportDir: File

    init {
        description = "Run benchmarks found in the current project and output reports to the " +
                "benchmark_reports folder under the project's build directory."

        benchmarkReportDir = File(project.buildDir, "benchmark_reports")
        outputs.dir(benchmarkReportDir)
        outputs.upToDateWhen { false } // Mirrors connectedAndroidTest behavior.
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        getReportsForDevices()
    }

    private fun getReportsForDevices() {
        if (benchmarkReportDir.exists()) {
            benchmarkReportDir.deleteRecursively()
        }
        benchmarkReportDir.mkdirs()

        val deviceIds = AdbUtil.execSync(adbPath, arrayOf("devices", "-l"), logger).stdout
            .split("\n")
            .drop(1)
            .map { it.split(Regex("\\s+")).first().trim() }
            .filter { !it.isBlank() }

        for (deviceId in deviceIds) {
            val dataDir = getReportDirForDevice(deviceId)
            if (dataDir.isBlank()) {
                throw StopExecutionException(
                    "Failed to find benchmark reports on device: $deviceId"
                )
            }

            val outDir = File(benchmarkReportDir, deviceId)
            outDir.mkdirs()
            getReportsForDevice(outDir, dataDir, deviceId)
        }
    }

    private fun getReportsForDevice(benchmarkReportDir: File, dataDir: String, deviceId: String) {
        AdbUtil.execSync(adbPath, arrayOf("shell", "ls", dataDir), logger, deviceId)
            .stdout
            .split("\n")
            .map { it.trim() }
            .filter { it.matches(Regex(".*benchmarkData[.](?:xml|json)$")) }
            .forEach {
                AdbUtil.execSync(
                    adbPath,
                    arrayOf("pull", "$dataDir/$it", "$benchmarkReportDir/$it"),
                    logger,
                    deviceId
                )

                AdbUtil.execSync(
                    adbPath,
                    arrayOf("shell", "rm", "$dataDir/$it"),
                    logger,
                    deviceId
                )
            }
    }

    /**
     * Query for test runner user's Download dir on shared public external storage via content
     * provider APIs.
     *
     * This folder is typically accessed in Android code via
     * Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
     */
    private fun getReportDirForDevice(deviceId: String): String {
        val cmd = arrayOf(
            "shell", "content", "query", "--uri", "content://media/external/file", "--projection",
            "_data", "--where", "\"_data LIKE '%Download'\""
        )

        // NOTE: stdout of the above command is of the form:
        // Row: 0 _data=/storage/emulated/0/Download
        return AdbUtil.execSync(adbPath, cmd, logger, deviceId)
            .stdout
            .split("\n")
            .first()
            .trim()
            .split(Regex("\\s+"))
            .last()
            .split("=")
            .last()
            .trim()
    }
}
