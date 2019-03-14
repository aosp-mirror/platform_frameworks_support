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
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

open class ClockTask : DefaultTask() {
    init {
        group = "Android"
    }

    fun execAdbSync(adbCmd: Array<String>, shouldThrow: Boolean = true): Process {
        val sdkPath = SdkUtil.getSdkPath(project).path
        val cmd = arrayOf("$sdkPath/platform-tools/adb", *adbCmd)

        logger.log(LogLevel.QUIET, cmd.joinToString(" "))
        val process = Runtime.getRuntime().exec(cmd)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for ${cmd.joinToString(" ")}")
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        logger.log(LogLevel.QUIET, stdout)
        logger.log(LogLevel.WARN, stderr)

        if (shouldThrow && process.exitValue() != 0) {
            throw GradleException(stderr)
        }

        return process
    }
}

open class LockClocksTask : ClockTask() {
    init {
        description = "locks clocks of connected, supported, rooted device"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        // Skip "adb root" if already rooted as it will fail.
        if (execAdbSync(arrayOf("shell", "su exit"), false).exitValue() != 0) {
            execAdbSync(arrayOf("root"))
        }

        val dest = "/data/local/tmp/lockClocks.sh"
        val source = javaClass.classLoader.getResource("scripts/lockClocks.sh")
        val tmpSource = Files.createTempFile("lockClocks.sh", null).toString()
        Files.copy(
            source.openStream(),
            Paths.get(tmpSource),
            StandardCopyOption.REPLACE_EXISTING
        )
        execAdbSync(arrayOf("push", tmpSource, dest))

        // Files pushed by adb push don't always preserve file permissions.
        execAdbSync(arrayOf("shell", "chmod", "700", dest))
        execAdbSync(arrayOf("shell", dest))
        execAdbSync(arrayOf("shell", "rm", dest))
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
        execAdbSync(arrayOf("reboot"))
    }
}
