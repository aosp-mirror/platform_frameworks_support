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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

/**
 * Task for building all of Androidx libraries and documentation
 */
open class BuildOnServer : DefaultTask() {

    init {
        group = "Build"
        description = "Builds all of the Androidx libraries and documentation"
    }

    @TaskAction
    fun checkAllBuildOutputs() {

        val distributionDirectory = project.getDistributionDirectory()

        val buildId = getBuildId()
        val requiredFiles = listOf(
            "android-support-public-docs-$buildId.zip",
            "android-support-tipOfTree-docs-$buildId.zip",
            "dokkaTipOfTreeDocs-$buildId.zip",
            "dokkaPublicDocs-$buildId.zip",
            "gmaven-diff-all-$buildId.zip",
            "jetifier-standalone.zip",
            "top-of-tree-m2repository-all-$buildId.zip",
            "top-of-tree-m2repository-partially-dejetified-$buildId.zip"
        )

        requiredFiles.forEach { fileName ->
            if (!File(distributionDirectory, fileName).exists()) {
                throw FileNotFoundException("buildOnServer required output missing: $fileName")
            }
        }
    }
}