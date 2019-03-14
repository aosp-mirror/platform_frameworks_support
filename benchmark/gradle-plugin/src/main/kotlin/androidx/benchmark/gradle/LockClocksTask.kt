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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

open class ClockTask : DefaultTask() {
    init {
        group = "Android"
    }

    fun runAdb(adbCommand: Array<String>) {
        val adbPath = "${getSdkPath(project.rootDir).path}/platform-tools/adb"
        execSync(arrayOf(adbPath, *adbCommand))
    }

    private fun execSync(cmd: Array<String>): String {
        logger.log(LogLevel.QUIET, cmd.joinToString(" "))
        val process = Runtime.getRuntime().exec(cmd)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for ${cmd.joinToString(" ")}")
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        logger.log(LogLevel.QUIET, stdout)
        logger.log(LogLevel.WARN, stderr)

        if (process.exitValue() != 0) {
            throw GradleException(stderr)
        }

        return stdout
    }

    /**
     * Adapted from com.android.build.gradle.internal.SdkHandler
     */
    private fun getSdkPath(rootDir: File): File {
        var sdkDirProp = project.rootProject.findProperty("sdk.dir")
        if (sdkDirProp is String) {
            val sdk = File(sdkDirProp)
            return when {
                !sdk.isAbsolute -> File(rootDir, sdkDirProp)
                else -> sdk
            }
        }

        // For some reason, project.rootProject.findProperty above fails to read properties in
        // local.properties file.

        val localProps = Properties()
        val localPropsStream = project.rootProject.file("local.properties").inputStream()
        localProps.load(localPropsStream)
        localPropsStream.close()
        sdkDirProp = localProps.getProperty("sdk.dir")
        if (sdkDirProp != null) {
            val sdk = File(sdkDirProp)
            return when {
                !sdk.isAbsolute -> File(rootDir, sdkDirProp)
                else -> sdk
            }
        }

        sdkDirProp = project.rootProject.findProperty("android.dir")
        if (sdkDirProp is String) {
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
        // The resource's path member returns a URI with missing trailing slashes after the scheme.
        // We need to first turn it into a URI object to get a valid path for the following shell
        // command.
        val source = javaClass.classLoader.getResource("scripts/lockClocks.sh").toURI().path
        val dest = "/data/local/tmp/lockClocks.sh"
        runAdb(arrayOf("root"))
        runAdb(arrayOf("push", source, dest))
        runAdb(arrayOf("shell", dest))
        runAdb(arrayOf("shell", "rm", dest))
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
        runAdb(arrayOf("reboot"))
    }
}

fun Project.createClockLockTasks() {
    tasks.create("lockClocks", LockClocksTask::class.java)
    tasks.create("unlockClocks", UnlockClocksTask::class.java)
}
