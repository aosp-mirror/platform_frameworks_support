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

import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Properties
import java.util.concurrent.TimeUnit

open class ClockTask : DefaultTask() {
    init {
        group = "Android"
    }

    fun runAdb(adbCommand: Array<String>, errorString: String) {
        var adbPath = execSync(arrayOf("which", "adb"), errorString)
        if (adbPath.isEmpty()) {
            throw GradleException("Failed to find runnable path to adb bin")
        }
        adbPath = "${getSdkPath(project.rootDir).path}/platform-tools/adb"
        execSync(arrayOf(adbPath, *adbCommand), errorString)
    }

    private fun execSync(cmd: Array<String>, errorString: String): String {
        logger.log(LogLevel.QUIET, cmd.joinToString(" "))
        val process = Runtime.getRuntime().exec(cmd)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for ${cmd.joinToString(" ")}")
        }

        val stdout = IOUtils.toString(process.inputStream, Charset.defaultCharset())
        logger.log(LogLevel.QUIET, stdout)
        logStream(LogLevel.WARN, process.errorStream)

        if (process.exitValue() != 0) {
            throw GradleException(errorString)
        }

        return stdout
    }

    private fun logStream(level: LogLevel, stream: InputStream) {
        val string = IOUtils.toString(stream, Charset.defaultCharset())
        if (string.isNotEmpty()) {
            logger.log(level, string)
        }
    }

    /**
     * Returns the appropriate SDK path.
     */
    private fun getSdkPath(supportRoot: File): File {
        val properties = Properties()
        val propertiesFile = File(supportRoot, "local.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use(properties::load)
        }
        return findSdkLocation(properties, supportRoot)
    }

    /**
     * Adapted from com.android.build.gradle.internal.SdkHandler
     */
    private fun findSdkLocation(properties: Properties, rootDir: File): File {
        var sdkDirProp = properties.getProperty("sdk.dir")
        if (sdkDirProp != null) {
            var sdk = File(sdkDirProp)
            if (!sdk.isAbsolute) {
                sdk = File(rootDir, sdkDirProp)
            }
            return sdk
        }

        sdkDirProp = properties.getProperty("android.dir")
        if (sdkDirProp != null) {
            return File(rootDir, sdkDirProp)
        }

        val envVar = System.getenv("ANDROID_HOME")
        if (envVar != null) {
            return File(envVar)
        }

        val property = System.getProperty("android.home")
        if (property != null) {
            return File(property)
        }
        throw Exception("Could not find your SDK")
    }
}

open class LockClocksTask : ClockTask() {
    init {
        description = "locks clocks of connected, supported, rooted device"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val source = project.projectDir.path + "/benchmark/lockClocks.sh"
        val dest = "/data/local/tmp/lockClocks.sh"
        runAdb(arrayOf("root"), "Failed to run 'adb root'")
        runAdb(arrayOf("push", source, dest), "Failed to push locking script")
        runAdb(arrayOf("shell", dest), "Failed to run clock locking script")
        runAdb(arrayOf("shell", "rm", dest), "Failed to remove clock locking script")
    }
}

open class UnlockClocksTask : ClockTask() {
    init {
        description = "unlocks clocks of device by rebooting"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        project.logger.log(LogLevel.LIFECYCLE, "Rebooting device to reset clocks")
        runAdb(arrayOf("reboot"), "Failed to reboot device")
    }
}

fun Project.createClockLockTasks() {
    tasks.create("lockClocks", LockClocksTask::class.java)
    tasks.create("unlockClocks", UnlockClocksTask::class.java)
}
