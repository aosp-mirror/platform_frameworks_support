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
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.safeNameForLazyResolve
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

class RequireSampledAnnotation : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        listOf(UClass::class.java, UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return Enforcer(context)
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
            val parentFqName = function.containingKtFile.packageFqName.asString()
            val fullFqName = "$parentFqName.${function.name}"

            val allKtFiles = getRootDirectory().getAllKtFiles()

            allKtFiles.forEach { file ->
                if (file.searchForSampleLink(fullFqName)) {
                    return
                }
            }

            context.report(
                OBSOLETE_ANNOTATION,
                node,
                context.getNameLocation(node),
                "${function.name} is annotated with @$SAMPLED_ANNOTATION, but is not linked " +
                        "to from a @$SAMPLE_KDOC_ANNOTATION tag."
            )
        }

        private fun handleSampleLink(kdoc: KDoc, sourceNodeName: String) {
            val sections: List<KDocSection> = kdoc.children.filter { it is KDocSection }.cast()

            // map of a KDocTag (which contains the location used when reporting issues) to the
            // method link specified in @sample
            val tags = sections.flatMap { section ->
                section.findTagsByName(SAMPLE_KDOC_ANNOTATION)
                    .mapNotNull { sampleTag ->
                        val linkText = sampleTag.getSubjectLink()?.getLinkText()
                        if (linkText == null) {
                            null
                        } else {
                            sampleTag to linkText
                        }
                    }.distinct()
            }

            // No @sample tags found
            if (tags.isEmpty()) {
                return
            }

            val rootDirectory = getRootDirectory()

            tags.forEach tagCheck@{ pair ->
                val docTag = pair.first
                // Given a link such as foo.bar.Abc.xyz (function inside class Abc)
                // Or a link such as foo.bar.xyz (top level function inside bar folder)
                val link = pair.second
                // The function is xyz
                val functionName = link.substringAfterLast(".")
                // The fully qualified name is foo.bar.Abc
                // Or foo.bar
                val fqName = link.substringBeforeLast(".")
                // The outer context is Abc
                // Or the outer context is bar
                val outerContext = fqName.substringAfterLast(".")
                // Whether we should look for a class file Abc.kt
                // Or just search for a top level function inside the bar folder
                val isFile = outerContext[0].isUpperCase()
                // The package name is foo.bar
                // Strip any capitalized classes from the package name
                val packageName = if (isFile) {
                    fqName.substringBeforeLast(".")
                } else {
                    fqName
                }

                val filesToSearch: List<PsiFile> = if (isFile) {
                    val filename = "$outerContext.kt"
                    // Look for PsiFile in frameworks/support
                    val file = rootDirectory.searchForFile(filename, packageName)
                        ?: throw IllegalStateException(
                            "Couldn't find $filename in frameworks/support"
                        )
                    listOf(file)
                } else {
                    val packageNames = packageName.split(".")
                    val dir = rootDirectory.searchForDirectory(packageNames)
                        ?: throw IllegalStateException(
                            "Couldn't find a directory for $packageName in frameworks/support"
                        )
                    dir.getAllKtFiles()
                }

                filesToSearch.forEach { file ->
                    val function = file.searchForFunction(functionName) ?: return@forEach

                    function.modifierList?.annotationEntries?.forEach { annotation ->
                        // Return to checking any other tags if this function is correctly annotated
                        if (annotation.shortName.safeNameForLazyResolve().identifier
                            == SAMPLED_ANNOTATION) {
                            return@tagCheck
                        }
                    }

                    context.report(
                        MISSING_ANNOTATION,
                        docTag,
                        context.getNameLocation(docTag),
                        "$functionName is not annotated with @$SAMPLED_ANNOTATION, but is " +
                                "linked to from the KDoc of $sourceNodeName"
                    )
                    return@tagCheck
                }

                fun getMessage(): String {
                    return if (isFile) {
                        "Couldn't find $functionName in $outerContext.kt"
                    } else {
                        "Couldn't find $functionName in $fqName"
                    }
                }

                throw IllegalStateException(getMessage())
            }
        }

        private fun getRootDirectory(): PsiDirectory {
            var currentDirectory = context.psiFile!!.parent!!
            // climb to frameworks/support
            while (currentDirectory.name != SUPPORT_DIRECTORY) {
                if (currentDirectory.parent == null) {
                    throw IllegalStateException("Couldn't find frameworks/support directory")
                }
                currentDirectory = currentDirectory.parent!!
            }
            return currentDirectory
        }

        private fun PsiDirectory.searchForFile(
            filename: String,
            packageName: String
        ): PsiFile? {
            files.forEach {
                if (it.name == filename) {
                    return it
                }
            }
            subdirectories.forEach {
                val file = it.searchForFile(filename, packageName)
                if (file != null && file.getPackageName() == packageName) {
                    return file
                }
            }
            return null
        }

        private fun PsiDirectory.searchForDirectory(
            packageNames: List<String>
        ): PsiDirectory? {
            fun PsiDirectory.isMatchingDirectory(packageNames: List<String>): Boolean {
                var currentDirectory = this
                packageNames.reversed().forEach { name ->
                    if (currentDirectory.name != name || currentDirectory.parentDirectory == null) {
                        return false
                    }
                    currentDirectory = currentDirectory.parentDirectory!!
                }
                return true
            }

            if (isMatchingDirectory(packageNames)) {
                return this
            }

            subdirectories.forEach { subDirectory ->
                val directory = subDirectory.searchForDirectory(packageNames)
                if (directory != null) {
                    return directory
                }
            }
            return null
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

        private fun PsiElement.getPackageName(): String? {
            // Breadth first
            val pendingChildren = mutableListOf<PsiElement>()
            children.forEach { child ->
                if (child is KtPackageDirective) {
                    return child.fqName.asString()
                }
                pendingChildren.addAll(child.children)
            }
            pendingChildren.forEach { child ->
                return child.getPackageName()
            }
            return null
        }

        private fun PsiElement.searchForFunction(functionName: String): KtNamedFunction? {
            if (this is KtNamedFunction && name == functionName) {
                return this
            }
            for (it in children) {
                val function = it.searchForFunction(functionName)
                if (function != null) {
                    return function
                }
            }
            return null
        }

        private fun PsiElement.searchForSampleLink(sampleLink: String): Boolean {
            if (this is KDoc) {
                val sections: List<KDocSection> = this.children.filter { it is KDocSection }.cast()
                sections.forEach { section ->
                    val sampleLinks = section.findTagsByName(SAMPLE_KDOC_ANNOTATION)
                    sampleLinks.forEach { sampleTag ->
                        if (sampleTag.getSubjectLink()?.getLinkText() == sampleLink) {
                            return true
                        }
                    }
                }
            }
            for (it in children) {
                if (it.searchForSampleLink(sampleLink)) {
                    return true
                }
            }
            return false
        }
    }

    companion object {
        const val SAMPLE_KDOC_ANNOTATION = "sample"
        const val SAMPLED_ANNOTATION = "Sampled"
        const val SUPPORT_DIRECTORY = "support"

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
