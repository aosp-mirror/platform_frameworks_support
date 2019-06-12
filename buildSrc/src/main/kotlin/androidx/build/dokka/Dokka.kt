/*
 * Copyright 2018 The Android Open Source Project
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

// This file creates tasks for generating documentation using Dokka
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from Dokka to just Docs
package androidx.build.dokka

import androidx.build.AndroidXExtension
import androidx.build.DiffAndDocs
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.PackageOptions
import java.io.File

object Dokka {
    fun generatorTaskNameForType(docsType: String, language: String = ""): String {
        return "dokka${language}${docsType}Docs"
    }
    fun archiveTaskNameForType(docsType: String): String {
        return "dist${docsType}DokkaDocs"
    }
    fun registerDocsTasks(
        docsType: String, // "public" or "tipOfTree"
        project: Project,
        hiddenPackages: List<String>
    ) {
        val archiveTaskName = archiveTaskNameForType(docsType)
        project.apply<DokkaAndroidPlugin>()
        // We don't use the `dokka` task, but it normally appears in `./gradlew tasks`
        // so replace it with a new task that doesn't show up and doesn't do anything
        project.tasks.replace("dokka")
        if (project.name != "support" && project.name != "docs-runner") {
            throw Exception("Illegal project passed to createDocsTask: " + project.name)
        }

        val kotlinDocTaskName = generatorTaskNameForType(docsType, "Kotlin")
        val kotlinDocDir = File(project.buildDir, kotlinDocTaskName).absolutePath
        val kotlinDocsTask = project.tasks.register(kotlinDocTaskName,
            DokkaAndroidTask::class.java) { docsTask ->
            docsTask.moduleName = project.name
            docsTask.outputDirectory = kotlinDocDir
            docsTask.description = "Generates $docsType Kotlin documentation in the style of " +
                    "d.android.com.  Places docs in ${docsTask.outputDirectory}"
            docsTask.outputFormat = "dac"
            docsTask.outlineRoot = "androidx/"
            docsTask.dacRoot = "/reference/kotlin"
            docsTask.moduleName = ""
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                docsTask.perPackageOptions.add(opts)
            }
        }

        val javaDocTaskName = generatorTaskNameForType(docsType, "Java")
        val javaDocDir = File(project.buildDir, javaDocTaskName).absolutePath
        val javaDocsTask = project.tasks.register(javaDocTaskName,
            DokkaAndroidTask::class.java) { docsTask ->
            docsTask.moduleName = project.name
            docsTask.outputDirectory = javaDocDir
            docsTask.description = "Generates $docsType Java documentation in the style of " +
                    "d.android.com.  Places docs in ${docsTask.outputDirectory}"
            docsTask.outputFormat = "dac-as-java"
            docsTask.outlineRoot = "androidx/"
            docsTask.dacRoot = "/reference/"
            docsTask.moduleName = ""
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                docsTask.perPackageOptions.add(opts)
            }
        }

        project.tasks.register(archiveTaskName, Zip::class.java) { zipTask ->
            zipTask.dependsOn(javaDocsTask)
            zipTask.from(javaDocDir) { copySpec ->
                copySpec.into("reference/")
            }

            zipTask.dependsOn(kotlinDocsTask)
            zipTask.from(kotlinDocDir) { copySpec ->
                copySpec.into("reference/kotlin")
            }
            val buildId = getBuildId()
            val archiveBaseName = generatorTaskNameForType(docsType)
            zipTask.archiveBaseName.set(archiveBaseName)
            zipTask.archiveVersion.set(buildId)
            zipTask.destinationDirectory.set(project.getDistributionDirectory())
            val filePath = "${project.getDistributionDirectory().canonicalPath}/"
            val fileName = "$archiveBaseName-$buildId.zip"
            zipTask.description = "Zips $docsType Kotlin documentation (generated via " +
                "Dokka in the style of d.android.com) into ${filePath + fileName}"
            zipTask.group = JavaBasePlugin.DOCUMENTATION_GROUP
        }
    }

    fun Project.configureAndroidProjectForDokka(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        afterEvaluate {
            if (name != "docs-runner") {
                DiffAndDocs.get(this).registerAndroidProject(library, extension)
            }

            DokkaPublicDocs.registerProject(this, extension)
            DokkaSourceDocs.registerAndroidProject(this, library, extension)
        }
    }

    fun Project.configureJavaProjectForDokka(extension: AndroidXExtension) {
        afterEvaluate {
            if (name != "docs-runner") {
                DiffAndDocs.get(this).registerJavaProject(this, extension)
            }
            DokkaPublicDocs.registerProject(this, extension)
            DokkaSourceDocs.registerJavaProject(this, extension)
        }
    }
}
