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

class RequireSampledAnnotation : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        listOf(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? = Enforcer(context)

    /**
     * Clear caches before and after a project run, as they are only relevant per project
     */
    override fun beforeCheckEachProject(context: Context) {
        sampleFunctionCache.clear()
        sampleLinkCache.clear()
    }

    override fun afterCheckEachProject(context: Context) {
        sampleFunctionCache.clear()
        sampleLinkCache.clear()
    }

    private class Enforcer(private val context: JavaContext) : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            val element = (node.sourceElement as? KtDeclaration) ?: return

            if (element.annotationEntries.any {
                    it.shortName.safeNameForLazyResolve().identifier == SAMPLED_ANNOTATION
                }) {
                return handleSampleCode(element, node)
            }

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

        private fun handleSampleCode(function: KtDeclaration, node: UMethod) {
            val currentPath = context.psiFile!!.virtualFile.path
            if (!currentPath.contains(SAMPLES_DIRECTORY)) {
                throw IllegalStateException("${function.name} in $currentPath is annotated with " +
                        "@$SAMPLED_ANNOTATION, but is not inside a $SAMPLES_DIRECTORY directory")
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
                OBSOLETE_ANNOTATION,
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

            // If we haven't found a path, it might be that we are on the same top level, i.e we are
            // in a/b/foo/integration-tests/sample, and the module is in a/b/foo/foo-xyz
            // So let's remove the last slash from the module, and try and see if that directory
            // matches
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

        private fun buildSampleFunctionCache(): List<KtNamedFunction> {
            val sampleDirectory = findSampleDirectory()
            val allKtFiles = sampleDirectory.getAllKtFiles()
            // Remove any functions without a valid fully qualified name, this includes things
            // such as overridden functions in anonymous classes like a Runnable
            return allKtFiles.flatMap { it.getAllFunctions() }.filter { it.fqName != null }
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

            // No @sample tags found
            if (sampleTags.isEmpty()) {
                return
            }

            if (sampleFunctionCache.isEmpty()) {
                sampleFunctionCache.addAll(buildSampleFunctionCache())
            }

            sampleTags.forEach checkSampleTags@{ pair ->
                val docTag = pair.first
                // Given a link such as foo.bar.Abc.xyz (function inside class Abc)
                // Or a link such as foo.bar.xyz (top level function inside bar folder)
                val link = pair.second

                sampleFunctionCache.forEach lookForFunctions@{ function ->
                    // We filtered out not-null fqNames when building the cache
                    if (link != function.fqName!!.asString()) {
                        return@lookForFunctions
                    }

                    function.modifierList?.annotationEntries?.forEach { annotation ->
                        // Return to checking any other tags if this function is correctly annotated
                        if (annotation.shortName.safeNameForLazyResolve().identifier
                            == SAMPLED_ANNOTATION
                        ) {
                            return@checkSampleTags
                        }
                    }

                    context.report(
                        MISSING_ANNOTATION,
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

        private fun navigateToDirectory(path: String): PsiDirectory {
            val filesystem = StandardFileSystems.local()
            val virtualFile = filesystem.findFileByPath(path)
            return context.psiFile!!.manager.findDirectory(virtualFile!!)
                ?: throw IllegalStateException("Couldn't find directory for $path")
        }

        /**
         * The sample directory could either be a direct child of the current module, or a
         * sibling directory
         *
         * For example, if we are in a/b/foo, the sample directory could either be:
         *     a/b/foo/.../samples
         *     a/b/.../samples
         *
         * For efficiency, first we look inside a/b/foo, and then if that fails we look inside a/b
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

        private fun PsiDirectory.getAllKtFiles(): List<PsiFile> {
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

        private fun PsiElement.findAllSampleLinks(): List<String> {
            val sampleLinks = mutableListOf<String>()
            if (this is KDoc) {
                val sections: List<KDocSection> = this.children.filter { it is KDocSection }.cast()
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

    companion object {
        // Cache containing every link referenced from @sample inside a project
        val sampleLinkCache = mutableListOf<String>()
        // Cache containing every function inside a project's corresponding samples
        val sampleFunctionCache = mutableListOf<KtNamedFunction>()

        const val SAMPLE_KDOC_ANNOTATION = "sample"
        const val SAMPLED_ANNOTATION = "Sampled"
        const val SAMPLES_DIRECTORY = "samples"

        val MISSING_ANNOTATION = Issue.create(
            "RequireSampledAnnotation",
            "Missing @Sampled annotation",
            "Functions referred to from KDoc with a @sample tag must be annotated with " +
                    "@Sampled, to provide visibility at the sample site and ensure that it " +
                    "doesn't get changed accidentally.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                RequireSampledAnnotation::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val OBSOLETE_ANNOTATION = Issue.create(
            "RequireSampledAnnotation",
            "Unused @Sampled annotation",
            "This function is annotated with @Sampled, but is not linked to from anywhere. " +
                    "Either remove this annotation, or add a valid @sample tag linking to it.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                RequireSampledAnnotation::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
