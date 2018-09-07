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

package androidx.navigation.safeargs.gradle

import androidx.navigation.safe.args.generator.ErrorMessage
import androidx.navigation.safe.args.generator.NavSafeArgsGenerator
import androidx.navigation.safe.args.generator.models.NavFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

private const val MAPPING_FILE = "file_mappings.json"

open class ArgumentsGenerationTask : DefaultTask() {
    @get:Input
    lateinit var rFilePackage: String

    @get:Input
    lateinit var applicationId: String

    @get:Input
    var useAndroidX: Boolean = false

    @get:OutputDirectory
    lateinit var outputDir: File

    @get:InputFiles
    var resDirectories: List<File> = emptyList()

    @get:InputFiles
    var librariesResDirectories: FileCollection? = null

    @get:OutputDirectory
    lateinit var incrementalFolder: File

    @Suppress("unused")
    @TaskAction
    internal fun taskAction(inputs: IncrementalTaskInputs) {
        if (inputs.isIncremental) {
            doIncrementalTaskAction(inputs)
        } else {
            project.logger.info("Unable do incremental execution: full task run")
            doFullTaskAction()
        }
    }

    private fun doFullTaskAction() {
        if (outputDir.exists() && !outputDir.deleteRecursively()) {
            project.logger.warn("Failed to clear directory for navigation arguments")
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw GradleException("Failed to create directory for navigation arguments")
        }
        val navFiles = getNavigationFiles(resDirectories).map { NavFile(it, false) }
        val libraryNavFiles = getNavigationFiles(librariesResDirectories?.files ?: emptySet())
                .map { NavFile(it, true) }
        val (mappings, errors) = generateArgs(navFiles + libraryNavFiles, outputDir)
        writeMappings(mappings)
        failIfErrors(errors)
    }

    private fun doIncrementalTaskAction(inputs: IncrementalTaskInputs) {
        val modifiedFiles = mutableSetOf<File>()
        val removedFiles = mutableSetOf<File>()
        // Gather modified files, this includes new files (but not new folders).
        inputs.outOfDate { change -> if (change.file.isFile) modifiedFiles.add(change.file) }
        // Gather removed files and folders.
        inputs.removed { change -> removedFiles.add(change.file) }

        val oldMapping = readMappings()
        val affectedNavFiles = getAffectedNavFiles(modifiedFiles, oldMapping)
        val (newMapping, errors) = generateArgs(affectedNavFiles, outputDir)
        val newJavaFiles = newMapping.flatMap { it.javaFiles }.toSet()
        val changedInputs = affectedNavFiles.map { it.file } + removedFiles
        val (modified, unmodified) = oldMapping.partition {
            File(project.projectDir, it.navFile) in changedInputs
        }
        modified.flatMap { it.javaFiles }
                .filter { name -> name !in newJavaFiles }
                .forEach { javaName ->
                    val fileName = "${javaName.replace('.', File.separatorChar)}.java"
                    val file = File(outputDir, fileName)
                    if (file.exists()) {
                        file.delete()
                    }
                }
        writeMappings(unmodified + newMapping)
        failIfErrors(errors)
    }

    private fun getNavigationFiles(resDirectories: Collection<File>) = resDirectories
            .mapNotNull {
                File(it, "navigation").let { navFolder ->
                    if (navFolder.exists() && navFolder.isDirectory) navFolder else null
                }
            }
            .flatMap { navFolder -> navFolder.listFiles().asIterable() }
            .groupBy { file -> file.name }
            .map { entry -> entry.value.last() }

    private fun generateArgs(navFiles: Collection<NavFile>, out: File) =
            NavSafeArgsGenerator(
                    navigationFiles = navFiles,
                    rFilePackage = rFilePackage,
                    applicationId = applicationId,
                    outputDir = out,
                    useAndroidX = useAndroidX).generate().map { output ->
                Mapping(navFile = output.source.file.relativeTo(project.projectDir).path,
                        javaFiles = output.fileNames,
                        includedNavFiles = output.extraSources.map {
                            it.file.relativeTo(project.projectDir).path
                        }) to output.errors
            }.unzip().let { (mappings, errorLists) -> mappings to errorLists.flatten() }

    private fun getAffectedNavFiles(
        modifiedFiles: Set<File>,
        oldMapping: List<Mapping>
    ): Set<NavFile> {
        val projectNavFiles = getNavigationFiles(resDirectories)
        return mutableSetOf<NavFile>() + modifiedFiles.map { modifiedFile ->
            // All modified files are affected files
            NavFile(modifiedFile, !projectNavFiles.contains(modifiedFile))
        } + modifiedFiles.flatMap { modifiedFile ->
            // If modified files are referenced through an include then the referee file is also
            // affected.
            val modifiedFilePath = modifiedFile.relativeTo(project.projectDir).path
            oldMapping.filter { mapping ->
                mapping.navFile == modifiedFilePath ||
                        mapping.includedNavFiles.contains(modifiedFilePath)
            }.flatMap { mapping ->
                // To properly re-process the referee file we also need the referenced files,
                // consider them part of the affected files unless they have been removed.
                // Since flattening will occur on a set, there will be no duplicates.
                (mapping.includedNavFiles + mapping.navFile).mapNotNull { filename ->
                    val file = File(project.projectDir, filename).normalize()
                    if (file.exists()) {
                        NavFile(file, !projectNavFiles.contains(file))
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun readMappings(): List<Mapping> {
        val type = object : TypeToken<List<Mapping>>() {}.type
        val mappingsFile = File(incrementalFolder, MAPPING_FILE)
        if (mappingsFile.exists()) {
            return mappingsFile.reader().use { Gson().fromJson(it, type) }
        } else {
            return emptyList()
        }
    }

    private fun writeMappings(mappings: List<Mapping>) {
        File(incrementalFolder, MAPPING_FILE).writer().use { Gson().toJson(mappings, it) }
    }

    private fun failIfErrors(errors: List<ErrorMessage>) {
        if (errors.isNotEmpty()) {
            val errString = errors.joinToString("\n") { it.toClickableText() }
            throw GradleException(
                    "androidx.navigation.safeargs plugin failed.\n " +
                            "Following errors found: \n$errString")
        }
    }
}

private fun ErrorMessage.toClickableText() = "$path:$line:$column " +
        "(${File(path).name}:$line): \n" +
        "error: $message"

private data class Mapping(
    val navFile: String,
    val javaFiles: List<String>,
    val includedNavFiles: List<String>
)
