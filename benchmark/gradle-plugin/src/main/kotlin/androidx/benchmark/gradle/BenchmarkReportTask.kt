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

import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class BenchmarkReportTask @Inject constructor(adbPath: String) : BenchmarkBaseTask(adbPath) {
    init {
        description = "Run benchmarks found in the current project and output reports to the " +
                "benchmark_reports folder under the project's build directory."
    }

    // TODO(dustin): Get this path somehow...
    private val dataDir = "/storage/emulated/0/Download"

    @Suppress("unused")
    @TaskAction
    fun exec() {
        getReportsForDevices()
    }

    private fun getReportsForDevices() {
        val benchmarkReportDir = File(project.buildDir, "benchmark_reports")
        if (benchmarkReportDir.exists()) {
            benchmarkReportDir.deleteRecursively()
        }
        benchmarkReportDir.mkdirs()

        val deviceIds = execAdbSync(arrayOf("devices", "-l")).stdout
            .split("\n")
            .drop(1)
            .map { it.split(Regex("\\s+")).first().trim() }
            .filter { !it.isBlank() }

        for (deviceId in deviceIds) {
            val outDir = File(benchmarkReportDir, deviceId)
            outDir.mkdirs()
            getReportsForDevice(deviceId, outDir)
        }
    }

    private fun getReportsForDevice(deviceId: String, benchmarkReportDir: File) {
        execAdbSync(
            arrayOf("shell", "ls", "-1", "$dataDir"),
            deviceId = deviceId
        )
            .stdout
            .split("\n")
            .filter {
                it.matches(Regex(".*benchmarkData\\.(?:xml|json)$"))
            }
            .forEach {
                execAdbSync(
                    arrayOf("pull", "$dataDir/$it", "$benchmarkReportDir/$it"),
                    deviceId = deviceId
                )
            }
    }
}
