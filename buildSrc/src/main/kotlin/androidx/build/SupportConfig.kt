/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.io.File

object SupportConfig {
    const val DEFAULT_MIN_SDK_VERSION = 14
    const val INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    const val BUILD_TOOLS_VERSION = "28.0.2"
    const val CURRENT_SDK_VERSION = 28

    fun getKeystore(project: Project): File {
        val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
                .get("supportRootFolder") as File
        return File(supportRoot, "development/keystore/debug.keystore")
    }
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
=======

    @JvmStatic
    fun getSupportRoot(project: Project): File {
        val extension = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        val file = extension.get("supportRootFolder") as File
        return file
    }

    @JvmStatic
    fun getPrebuiltsRootPath(project: Project): String {
        val reposProperties = (project.rootProject.property("ext") as ExtraPropertiesExtension)
            .get("repos") as Map<*, *>
        return reposProperties["prebuiltsRoot"].toString()
    }

    @JvmStatic
    fun getSupportRepoPath(project: Project): String {
        return project.getRepositoryDirectory().absolutePath
    }

    @JvmStatic
    fun getAGPVersion(project: Project): String {
        val studioProperties = (project.rootProject.property("ext") as ExtraPropertiesExtension)
            .let { it.get("build_versions") as Map<*, *> }
            .let { it["studio"] as Map<*, *> }
        return studioProperties["agp"].toString()
    }
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
}
