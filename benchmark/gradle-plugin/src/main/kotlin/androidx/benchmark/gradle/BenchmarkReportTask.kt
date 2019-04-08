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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.builder.testing.api.DeviceConnector
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class BenchmarkReportTask @Inject constructor(adbPath: String) : BenchmarkBaseTask(adbPath) {
    init {
        description = ""
    }

    // TODO(dustin): Get this path somehow...
    private val dataDir = "/storage/emulated/0/Download"

    @Suppress("unused")
    @TaskAction
    fun exec() {
        try {
            reportFromAppExtension()
        } catch (expected: UnknownDomainObjectException) {
            try {
                reportFromLibExtension()
            } catch (e: UnknownDomainObjectException) {
                throw StopExecutionException("Failed to fetch benchmark reports")
            }
        }
    }

    @Throws(UnknownDomainObjectException::class)
    private fun reportFromAppExtension() {
        val appExtension = project.extensions.getByType(AppExtension::class.java)
        val devices = appExtension.deviceProviders.map { it.devices }.flatten()
        getReportsForDevices(devices)
    }

    @Throws(UnknownDomainObjectException::class)
    private fun reportFromLibExtension() {
        val libExtension = project.extensions.getByType(LibraryExtension::class.java)
        val devices = libExtension.deviceProviders.map { it.devices }.flatten()
        getReportsForDevices(devices)
    }

    private fun getReportsForDevices(devices: List<DeviceConnector>) {
        val benchmarkReportDir = File(project.buildDir, "benchmark_reports")
        if (benchmarkReportDir.exists()) {
            benchmarkReportDir.deleteRecursively()
        }
        benchmarkReportDir.mkdirs()

        execAdbSync(arrayOf("devices", "-l"))

        for (device in devices) {
            val outDir = File(benchmarkReportDir, device.serialNumber)
            outDir.mkdirs()
            getReportsForDevice(device, outDir)
        }
    }

    private fun getReportsForDevice(device: DeviceConnector, benchmarkReportDir: File) {
        execAdbSync(
            arrayOf("shell", "ls", "-1", "$dataDir"),
            deviceId = device.serialNumber
        )
            .stdout
            .split("\n")
            .filter {
                it.matches(Regex(".*benchmarkData\\.(?:xml|json)$"))
            }
            .forEach {
                execAdbSync(
                    arrayOf("pull", "$dataDir/$it", "$benchmarkReportDir/$it"),
                    deviceId = device.serialNumber
                )
            }
    }
}
