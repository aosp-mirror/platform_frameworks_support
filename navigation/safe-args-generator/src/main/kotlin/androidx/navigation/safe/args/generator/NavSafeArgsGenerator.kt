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

import androidx.navigation.safe.args.generator.ext.toClassName
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.NavFile
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import java.io.File

class NavSafeArgsGenerator(
    val navigationFiles: Collection<NavFile>,
    val rFilePackage: String,
    val applicationId: String,
    val outputDir: File,
    val useAndroidX: Boolean = false
) {
    private val destinationCache = mutableMapOf<NavFile, Destination>()

    fun generate() = navigationFiles.filterNot { it.isLibraryFile }.map { navFile ->
        val context = Context(navFile)
        val destination = destinationCache.getOrPut(navFile) {
            NavParser.parseNavigationFile(
                    navigationFile = navFile,
                    rFilePackage = rFilePackage,
                    applicationId = applicationId,
                    context = context)
        }
        context to destination
    }.map { (context, destination) ->
        context to resolveArguments(parseIncludedDestinations(context, destination))
    }.map { (context, resolvedDestination) ->
        val javaFiles = generateClasses(resolvedDestination)
        GeneratorOutput(context.navFile, javaFiles, context.logger.allMessages())
    }

    private fun parseIncludedDestinations(context: Context, destination: Destination): Destination {
        destination.included.forEach { includedDestination ->
            navigationFiles.firstOrNull {
                val navFilename = it.file.name.substring(0, it.file.name.lastIndexOf('.'))
                navFilename == includedDestination.id.name
            }?.let { navFile ->
                includedDestination.actual = destinationCache.getOrPut(navFile) {
                    NavParser.parseNavigationFile(
                            navigationFile = navFile,
                            rFilePackage = rFilePackage,
                            applicationId = applicationId,
                            context = context)
                }
            }
        }
        destination.nested.forEach { parseIncludedDestinations(context, it) }
        return destination
    }

    private fun generateClasses(destination: Destination): List<JavaFile> {
        val javaFiles = mutableSetOf<JavaFile>()
        fun writeJavaFiles(
            destination: Destination,
            parentDirectionName: ClassName?
        ) {
            val directionsJavaFile = if (destination.actions.isNotEmpty() ||
                    parentDirectionName != null) {
                generateDirectionsJavaFile(destination, parentDirectionName, useAndroidX)
            } else {
                null
            }
            val argsJavaFile = if (destination.args.isNotEmpty()) {
                generateArgsJavaFile(destination, useAndroidX)
            } else {
                null
            }
            directionsJavaFile?.let { javaFiles.add(it) }
            argsJavaFile?.let { javaFiles.add(it) }
            destination.nested.forEach { it ->
                writeJavaFiles(it, directionsJavaFile?.toClassName())
            }
        }
        writeJavaFiles(destination, null)
        javaFiles.forEach { javaFile -> javaFile.writeTo(outputDir) }
        return javaFiles.toList()
    }
}
