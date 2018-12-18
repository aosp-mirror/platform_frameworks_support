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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.java.JavaCodeFile
import androidx.navigation.safe.args.generator.java.JavaNavWriter
import androidx.navigation.safe.args.generator.kotlin.KotlinCodeFile
import androidx.navigation.safe.args.generator.kotlin.KotlinNavWriter
import androidx.navigation.safe.args.generator.models.Destination
import java.io.File

fun SafeArgsGenerator(
    rFilePackage: String,
    applicationId: String,
    navigationXml: File,
    outputDir: File,
    useAndroidX: Boolean,
    generateKotlin: Boolean
): NavSafeArgsGenerator<*> {
    return if (generateKotlin) {
        object : NavSafeArgsGenerator<KotlinCodeFile>(
            rFilePackage,
            applicationId,
            navigationXml,
            outputDir
        ) {
            override val writer =
                KotlinNavWriter(useAndroidX)
        }
    } else {
        object : NavSafeArgsGenerator<JavaCodeFile>(
            rFilePackage,
            applicationId,
            navigationXml,
            outputDir
        ) {
            override val writer =
                JavaNavWriter(useAndroidX)
        }
    }
}

abstract class NavSafeArgsGenerator<T : CodeFile> protected constructor(
    val rFilePackage: String,
    val applicationId: String,
    val navigationXml: File,
    val outputDir: File
) {
    abstract val writer: NavWriter<T>

    fun generate(): GeneratorOutput {
        val context = Context()
        val rawDestination = NavParser.parseNavigationFile(
            navigationXml,
            rFilePackage,
            applicationId,
            context
        )
        val resolvedDestination = resolveArguments(rawDestination)
        val codeFiles = mutableSetOf<T>()
        fun writeCodeFiles(
            destination: Destination,
            parentDirection: T?
        ) {
            val directionsCodeFile = if (destination.actions.isNotEmpty() ||
                parentDirection != null
            ) {
                writer.generateDirectionsCodeFile(destination, parentDirection)
            } else {
                null
            }
            val argsCodeFile = if (destination.args.isNotEmpty()) {
                writer.generateArgsCodeFile(destination)
            } else {
                null
            }
            directionsCodeFile?.let { codeFiles.add(it) }
            argsCodeFile?.let { codeFiles.add(it) }
            destination.nested.forEach { it ->
                writeCodeFiles(it, directionsCodeFile)
            }
        }
        writeCodeFiles(resolvedDestination, null)
        codeFiles.forEach { it.writeTo(outputDir) }
        return GeneratorOutput(codeFiles.toList(), context.logger.allMessages())
    }
}
