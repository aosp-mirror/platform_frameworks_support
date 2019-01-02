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

import java.io.File
import androidx.build.DiffAndDocs
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.SupportLibraryExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.PackageOptions


object Dokka {
    fun createDocsTask(taskName: String, project: Project, hiddenPackages: List<String>, archiveTaskName: String) {
        project.apply<DokkaAndroidPlugin>()
        if (project.name != "support" && project.name != "docs-fake") {
            throw Exception("Illegal project passed to createDocsTask: " + project.name)
        }
        val docsTask = project.tasks.create(taskName, DokkaAndroidTask::class.java) { docsTask ->
            docsTask.moduleName = project.name
            docsTask.outputDirectory = File(project.buildDir, taskName).absolutePath
            docsTask.outputFormat = "dac"
            docsTask.dacRoot = "/reference/androidx"
            docsTask.moduleName = ""
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                docsTask.perPackageOptions.add(opts)
            }
        }

        // TODO(b/121270092) remove these `sed` commands when Dokka supports moving the tables of contents and rewriting their contained links
        var inputClassesHtml = File(docsTask.outputDirectory, "classes.html")
        var outputClassesHtml = File(File(docsTask.outputDirectory, "androidx"), "classes.html")
        var replaceHtmlLinksCommand = "-e 's|href=\"androidx/|href=\"|g' -e 's|androidx/androidx|androidx|g'" 
        var classesHtmlTask = createSedCommand(project, "rewriteClassesHtmlFor" + taskName, inputClassesHtml, outputClassesHtml, replaceHtmlLinksCommand)
        classesHtmlTask.dependsOn(docsTask)

        var inputPackagesHtml = File(docsTask.outputDirectory, "packages.html")
        var outputPackagesHtml = File(File(docsTask.outputDirectory, "androidx"), "packages.html")
        var packagesHtmlTask = createSedCommand(project, "rewritePackagesHtmlFor" + taskName, inputPackagesHtml, outputPackagesHtml, replaceHtmlLinksCommand)
        packagesHtmlTask.dependsOn(docsTask)

        var inputToc = File(docsTask.outputDirectory, "_toc.yaml")
        var outputToc = File(File(docsTask.outputDirectory, "androidx"), "_toc.yaml")
        var tocTask = createSedCommand(project, "rewriteTocFor" + taskName, inputToc, outputToc, "'s|androidx/androidx|androidx|g'")
        tocTask.dependsOn(docsTask)

        project.tasks.create(archiveTaskName, Zip::class.java) { zipTask ->
            zipTask.dependsOn(classesHtmlTask)
            zipTask.dependsOn(packagesHtmlTask)
            zipTask.dependsOn(tocTask)
            zipTask.description = "Generates documentation artifact for pushing to developer.android.com"
            zipTask.from(docsTask.outputDirectory) { copySpec ->
                copySpec.into("reference")
                // TODO(b/121270092) remove these moves and exclusions when Dokka supports moving the tables of contents and rewriting their contained links
                copySpec.rename("package-list", "androidx/package-list")
                copySpec.exclude("classes.html")
                copySpec.exclude("packages.html")
                copySpec.exclude("_toc.yaml")
            }
            zipTask.baseName = taskName
            zipTask.version = getBuildId()
            zipTask.destinationDir = project.getDistributionDirectory()
        }
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        DiffAndDocs.get(project).registerPrebuilts(extension)
        DokkaPublicDocs.registerAndroidProject(project, library, extension)
        DokkaSourceDocs.registerAndroidProject(project, library, extension)
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        DiffAndDocs.get(project).registerPrebuilts(extension)
        DokkaPublicDocs.registerJavaProject(project, extension)
        DokkaSourceDocs.registerJavaProject(project, extension)
    }

    fun createSedCommand(
        project: Project,
        taskName: String,
        inputFile: File,
        outputFile: File,
        sedText: String
    ): Task {
        return project.tasks.create(taskName, Exec::class.java) { task ->
            task.executable("bash")
            task.args("-c", "sed ${sedText} \"${inputFile}\" > \"${outputFile}\"")
            task.inputs.file(inputFile)
            task.outputs.file(outputFile)
        }
    }

}
