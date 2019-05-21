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

package androidx.build.inspection

import androidx.build.SupportConfig
import androidx.build.getSdkPath
import androidx.build.gradle.getByType
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import java.io.File

fun Project.isAgentProject() = name.endsWith("-agent")

object Inspections {
    fun createAgentsTasks(project: Project) {
        project.plugins.all { plugin ->
            if (plugin !is LibraryPlugin) {
                return@all
            }

            val extension = project.extensions.getByType<LibraryExtension>()
            extension.libraryVariants.all { variant ->
                if (variant.name == "release") {
                    val unzip = variant.registerUnzipTask(project, variant.packageLibraryProvider!!)
                    variant.registerDxTask(project, unzip)
                }
            }
        }
    }

}


fun BaseVariant.registerUnzipTask(
    project: Project,
    zipTask: TaskProvider<Zip>
): TaskProvider<Copy> {
    val varName = name.capitalize()
    return project.tasks.register("unzip$varName", Copy::class.java) {
        it.apply {
            from(project.zipTree(zipTask.get().archiveFile))
            into(project.file("${project.buildDir}/unpacked$varName"))
            dependsOn(assembleProvider)
        }
    }
}

fun BaseVariant.registerDxTask(project: Project, unzipTaskProvider: TaskProvider<Copy>) =
    project.tasks.register("dxAgent$name", Exec::class.java) {
        it.apply {
            val sdkDir = getSdkPath(project.rootProject.rootDir)
            executable = File(sdkDir, "build-tools/${SupportConfig.BUILD_TOOLS_VERSION}/dx").absolutePath
            doFirst {
                workingDir.mkdir()
                val jars = project.fileTree(unzipTaskProvider.get().destinationDir) {
                    it.include("**/*.jar")
                }
                val list = jars.files.map { it.absolutePath }
                args(listOf("--dex", "--output", "dexed_inst.jar") + list)
            }
            workingDir = project.file("${project.buildDir}/dxAgent${this@registerDxTask.name}")

            setErrorOutput(System.out)
            setStandardOutput(System.out)
            dependsOn(unzipTaskProvider)
        }
    }
