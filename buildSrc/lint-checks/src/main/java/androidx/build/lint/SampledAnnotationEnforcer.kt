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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.safeNameForLazyResolve
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

/**
 * Class containing two lint detectors responsible for enforcing @Sampled annotation usage when
 * AndroidXExtension.enforceSampledAnnotation == true
 *
 * 1. @sampleLinkDetector, which enforces that any samples referenced from a @sample tag in KDoc
 * are correctly annotated with @Sampled
 *
 * 2. @SampledFunctionDetector, which enforces that any sample functions annotated with @Sampled
 * are linked to from KDoc in the parent module
 *
 * These lint checks make some assumptions about directory / module structure, and supports two
 * such setups:
 *
 * 1. Module foo which has a 'samples' dir/module inside it
 * 2. Module foo which has a 'samples' dir/module alongside it
 */
class SampledAnnotationEnforcer {

    companion object {
        // The name of the @sample tag in KDoc
        const val SAMPLE_KDOC_ANNOTATION = "sample"
        // The name of the @Sampled annotation that samples must be annotated with
        const val SAMPLED_ANNOTATION = "Sampled"
        // The name of the samples directory inside a project
        const val SAMPLES_DIRECTORY = "samples"

        val MISSING_SAMPLED_ANNOTATION = Issue.create(
            "EnforceSampledAnnotation",
            "Missing @$SAMPLED_ANNOTATION annotation",
            "Functions referred to from KDoc with a @$SAMPLE_KDOC_ANNOTATION tag must " +
                    "be annotated with @$SAMPLED_ANNOTATION, to provide visibility at the sample " +
                    "site and ensure that it doesn't get changed accidentally.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                `@sampleLinkDetector`::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val OBSOLETE_SAMPLED_ANNOTATION = Issue.create(
            "EnforceSampledAnnotation",
            "Obsolete @$SAMPLED_ANNOTATION annotation",
            "This function is annotated with @$SAMPLED_ANNOTATION, but is not linked to " +
                    "from a @$SAMPLE_KDOC_ANNOTATION tag. Either remove this annotation, or add " +
                    "a valid @$SAMPLE_KDOC_ANNOTATION tag linking to it.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                `@SampledFunctionDetector`::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    /**
     * Enforces that any @sample links in KDoc link to a function that is annotated with @Sampled
     *
     * Checks KDoc in classes, and in functions
     *
     * Throws an exception if the function cannot be found from KDoc - Dokka currently just prints
     * a warning to console and generates 'unresolved link' in documentation, so we want some
     * stricter enforcement for this
     */
    class `@sampleLinkDetector` : Detector(), SourceCodeScanner {

        override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf(UMethod::class.java, UClass::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler? =
            `@sampleLinkHandler`(context)

        /**
         * Clear caches before and after a project run, as they are only relevant per project
         */
        override fun beforeCheckEachProject(context: Context) {
            sampleFunctionCache.clear()
        }

        override fun afterCheckEachProject(context: Context) {
            sampleFunctionCache.clear()
        }

        companion object {
            // Cache containing every function inside a project's corresponding samples dir
            val sampleFunctionCache = mutableListOf<KtNamedFunction>()
        }

        private class `@sampleLinkHandler`(private val context: JavaContext) :
            BaseSampleHandler(context) {
            override fun visitMethod(node: UMethod) {
                val element = (node.sourceElement as? KtDeclaration) ?: return

                val kdoc = findDocComment(element)

                if (kdoc != null) {
                    return handleSampleLink(kdoc, node.name)
                }
            }

            override fun visitClass(node: UClass) {
                val element = (node.sourceElement as? KtDeclaration) ?: return

                val kdoc = findDocComment(element)

                if (kdoc != null) {
                    handleSampleLink(kdoc, node.qualifiedName!!)
                }
            }

            private fun handleSampleLink(kdoc: KDoc, sourceNodeName: String) {
                val sections: List<KDocSection> = kdoc.children.filter { it is KDocSection }.cast()

                // map of a KDocTag (which contains the location used when reporting issues) to the
                // method link specified in @sample
                val sampleTags = sections.flatMap { section ->
                    section.findTagsByName(SAMPLE_KDOC_ANNOTATION)
                        .mapNotNull { sampleTag ->
                            val linkText = sampleTag.getSubjectLink()?.getLinkText()
                            if (linkText == null) {
                                null
                            } else {
                                sampleTag to linkText
                            }
                        }
                }.distinct()

                // No @sample tags found, skip this node
                if (sampleTags.isEmpty()) {
                    return
                }

                if (sampleFunctionCache.isEmpty()) {
                    sampleFunctionCache.addAll(buildSampleFunctionCache())
                }

                sampleTags.forEach checkSampleTags@{ pair ->
                    val docTag = pair.first
                    // Link can look like foo.bar.Abc.xyz (function inside class Abc)
                    // Link can also look like foo.bar.xyz (top level function inside bar folder)
                    val link = pair.second

                    sampleFunctionCache.forEach lookForFunctions@{ function ->
                        // We filtered out not-null fqNames when building the cache, so safe to !!
                        if (link != function.fqName!!.asString()) {
                            return@lookForFunctions
                        }

                        function.modifierList?.annotationEntries?.forEach { annotation ->
                            // Return to checking any other tags if this function is correctly
                            // annotated
                            if (annotation.shortName.safeNameForLazyResolve().identifier
                                == SAMPLED_ANNOTATION
                            ) {
                                return@checkSampleTags
                            }
                        }

                        context.report(
                            MISSING_SAMPLED_ANNOTATION,
                            docTag,
                            context.getNameLocation(docTag),
                            "${function.name} is not annotated with @$SAMPLED_ANNOTATION, but " +
                                    "is linked to from the KDoc of $sourceNodeName"
                        )
                        return@checkSampleTags
                    }
                    throw IllegalStateException("Couldn't find a valid function matching $link")
                }
            }

            private fun buildSampleFunctionCache(): List<KtNamedFunction> {
                val sampleDirectory = findSampleDirectory()
                val allKtFiles = sampleDirectory.getAllKtFiles()
                // Remove any functions without a valid fully qualified name, this includes things
                // such as overridden functions in anonymous classes like a Runnable
                return allKtFiles.flatMap { it.getAllFunctions() }.filter { it.fqName != null }
            }

            /**
             * The sample directory could either be a direct child of the current module, or a
             * sibling directory
             *
             * For example, if we are in a/b/foo, the sample directory could either be:
             *     a/b/foo/.../samples
             *     a/b/.../samples
             *
             * For efficiency, first we look inside a/b/foo, and then if that fails we look
             * inside a/b
             */
            private fun findSampleDirectory(): PsiDirectory {
                val currentProjectPath = context.project.dir.absolutePath
                val currentProjectDir = navigateToDirectory(currentProjectPath)
                fun PsiDirectory.searchForSampleDirectory(): PsiDirectory? {
                    if (name == SAMPLES_DIRECTORY) {
                        return this
                    }
                    subdirectories.forEach {
                        val dir = it.searchForSampleDirectory()
                        if (dir != null) {
                            return dir
                        }
                    }
                    return null
                }

                // Look inside a/b/foo
                var sampleDir = currentProjectDir.searchForSampleDirectory()

                // Try looking inside /a/b
                if (sampleDir == null) {
                    sampleDir = currentProjectDir.parent!!.searchForSampleDirectory()
                }

                if (sampleDir == null) {
                    throw IllegalStateException("Could not find samples directory inside " +
                            currentProjectPath
                    )
                }

                return sampleDir
            }

            private fun PsiElement.getAllFunctions(): List<KtNamedFunction> {
                val functions = mutableListOf<KtNamedFunction>()
                if (this is KtNamedFunction) {
                    functions.add(this)
                }
                children.forEach {
                    functions.addAll(it.getAllFunctions())
                }
                return functions
            }
        }
    }

    /**
     * Checks all functions annotated with @Sampled to ensure that they are linked from KDoc
     *
     * Throws an exception if a function is annotated with @Sampled, and does not live in a
     * samples directory/module
     */
    class `@SampledFunctionDetector` : Detector(), SourceCodeScanner {

        override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf(UMethod::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler? =
            `@SampledFunctionHandler`(context)

        /**
         * Clear caches before and after a project run, as they are only relevant per project
         */
        override fun beforeCheckEachProject(context: Context) {
            sampleLinkCache.clear()
        }

        override fun afterCheckEachProject(context: Context) {
            sampleLinkCache.clear()
        }

        companion object {
            // Cache containing every link referenced from a @sample tag inside the parent project
            val sampleLinkCache = mutableListOf<String>()
        }

        private class `@SampledFunctionHandler`(private val context: JavaContext) :
            BaseSampleHandler(context) {
            override fun visitMethod(node: UMethod) {
                val element = (node.sourceElement as? KtDeclaration) ?: return

                if (element.annotationEntries.any {
                        it.shortName.safeNameForLazyResolve().identifier == SAMPLED_ANNOTATION
                    }) {
                    handleSampleCode(element, node)
                }
            }

            private fun handleSampleCode(function: KtDeclaration, node: UMethod) {
                val currentPath = context.psiFile!!.virtualFile.path
                if (!currentPath.contains(SAMPLES_DIRECTORY)) {
                    throw IllegalStateException("${function.name} in $currentPath is annotated " +
                            "with @$SAMPLED_ANNOTATION, but is not inside a $SAMPLES_DIRECTORY " +
                            "directory")
                }

                // The package name of the file we are in
                val parentFqName = function.containingKtFile.packageFqName.asString()
                // The full name of the current function that will be referenced in a @sample tag
                val fullFqName = "$parentFqName.${function.name}"

                // If this is the first time we encountered a @Sampled tag in this module, build
                // the cache containing a list of @sample links in the parent module
                if (sampleLinkCache.isEmpty()) {
                    sampleLinkCache.addAll(buildSampleLinkCache())
                }

                if (sampleLinkCache.any { it == fullFqName }) {
                    return
                }

                context.report(
                    OBSOLETE_SAMPLED_ANNOTATION,
                    node,
                    context.getNameLocation(node),
                    "${function.name} is annotated with @$SAMPLED_ANNOTATION, but is not " +
                            "linked to from a @$SAMPLE_KDOC_ANNOTATION tag."
                )
            }

            /**
             * At this point we are inside some sample module, which is depending on a module that
             * would end up referencing the sample
             *
             * For example, we could be in :foo:integration-tests:sample, and we want to find the
             * path for module :foo
             */
            private fun buildSampleLinkCache(): List<String> {
                val currentProjectPath = context.project.dir.absolutePath

                // The paths of every module the current module depends on
                val dependenciesPathList = context.project.directLibraries.map {
                    it.dir.absolutePath
                }

                // Try and find a common path, i.e if we are in a/b/foo/integration-tests/sample, we
                // will match a/b/foo for the parent
                var parentProjectPath = dependenciesPathList.find {
                    currentProjectPath.startsWith(it)
                }

                // If we haven't found a path, it might be that we are on the same top level, i.e
                // we are in a/b/foo/integration-tests/sample, and the module is in a/b/foo/foo-xyz
                // Try removing the last slash from the module, and see if that directory matches
                if (parentProjectPath == null) {
                    parentProjectPath = dependenciesPathList.find {
                        currentProjectPath.startsWith(it.substringBeforeLast("/"))
                    }
                }

                // There is no dependent module that exists above us, or alongside us, so throw
                if (parentProjectPath == null) {
                    throw IllegalStateException("Couldn't find a parent project for " +
                            currentProjectPath
                    )
                }

                val parentProjectDirectory = navigateToDirectory(parentProjectPath)

                val allKtFiles = parentProjectDirectory.getAllKtFiles()

                val sampleLinkCache = mutableListOf<String>()

                allKtFiles.forEach { file ->
                    sampleLinkCache.addAll(file.findAllSampleLinks())
                }

                return sampleLinkCache
            }

            private fun PsiElement.findAllSampleLinks(): List<String> {
                val sampleLinks = mutableListOf<String>()
                if (this is KDoc) {
                    val sections: List<KDocSection> = this.children.filter {
                        it is KDocSection
                    }.cast()
                    sections.forEach { section ->
                        section.findTagsByName(SAMPLE_KDOC_ANNOTATION).forEach { sampleTag ->
                            sampleTag.getSubjectLink()?.getLinkText()?.let { sampleLinks.add(it) }
                        }
                    }
                }
                children.forEach { sampleLinks.addAll(it.findAllSampleLinks()) }
                return sampleLinks
            }
        }
    }

    /**
     * Base handler that provides some utility functions shared between both handlers
     */
    private abstract class BaseSampleHandler(private val context: JavaContext) : UElementHandler() {
        fun navigateToDirectory(path: String): PsiDirectory {
            val filesystem = StandardFileSystems.local()
            val virtualFile = filesystem.findFileByPath(path)
            return context.psiFile!!.manager.findDirectory(virtualFile!!)
                ?: throw IllegalStateException("Couldn't find directory for $path")
        }

        fun PsiDirectory.getAllKtFiles(): List<PsiFile> {
            val psiFiles = mutableListOf<PsiFile>()
            files.forEach {
                if (it.name.endsWith("kt")) {
                    psiFiles.add(it)
                }
            }
            subdirectories.forEach {
                psiFiles.addAll(it.getAllKtFiles())
            }
            return psiFiles
        }
    }
}
