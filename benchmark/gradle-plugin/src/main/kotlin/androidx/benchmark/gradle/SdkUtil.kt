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

import org.gradle.api.Project
import java.io.File
import java.util.Properties

class SdkUtil {
    /**
     * Adapted from com.android.build.gradle.internal.SdkHandler
     */
    fun getSdkPath(project: Project): File {
        val rootDir = project.rootDir
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
        val localPropsFile = project.rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            val localPropsStream = localPropsFile.inputStream()
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