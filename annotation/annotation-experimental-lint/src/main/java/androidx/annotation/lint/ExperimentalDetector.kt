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

package androidx.annotation.lint

import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

class ExperimentalDetector : Detector(), SourceCodeScanner {
    override fun applicableAnnotations(): List<String>? = listOf(
        EXPERIMENTAL_ANNOTATION
    )

    override fun inheritAnnotation(annotation: String): Boolean = true

    override fun visitAnnotationUsage(
        context: JavaContext,
        usage: UElement,
        type: AnnotationUsageType,
        annotation: UAnnotation,
        qualifiedName: String,
        method: PsiMethod?,
        referenced: PsiElement?,
        annotations: List<UAnnotation>,
        allMemberAnnotations: List<UAnnotation>,
        allClassAnnotations: List<UAnnotation>,
        allPackageAnnotations: List<UAnnotation>
    ) {
        when (qualifiedName) {
            EXPERIMENTAL_ANNOTATION -> {
                checkExperimentalUsage(context, annotation, usage)
            }
        }
    }

    /**
     * Check whether the given [referenced] experimental API can be referenced from [usage] call
     * site.
     *
     * @param context the lint scanning context
     * @param annotation the experimental annotation detected on the referenced element
     * @param usage the element whose usage should be checked
     */
    private fun checkExperimentalUsage(
        context: JavaContext,
        annotation: UAnnotation,
        usage: UElement
    ) {
        val useAnnotation = (annotation.uastParent as? UClass)?.qualifiedName ?: return
        if (!hasOrUsesAnnotation(context, usage, useAnnotation)) {
            report(context, usage, """
                This declaration is experimental and its usage should be marked with
                '@$useAnnotation' or '@UseExperimental($useAnnotation.class)'
                """)
        }
    }

    private fun hasOrUsesAnnotation(
        context: JavaContext,
        usage: UElement,
        annotationName: String
    ): Boolean {
        val evaluator = context.evaluator
        var element: UElement? = usage
        while (element != null) {
            if (element is UAnnotated) {
                val annotations = evaluator.getAllAnnotations(element, false)
                for (annotation in annotations) {
                    if (annotation.qualifiedName == annotationName) {
                        return true
                    } else if (annotation.qualifiedName == USE_EXPERIMENTAL_ANNOTATION &&
                        annotation.attributeValues.isNotEmpty()) {
                        val markerClass = annotation.attributeValues[0]
                        if (getMarkerClassQualifiedName(context, markerClass) == annotationName) {
                            return true
                        }
                    }
                }
            }
            element = element.uastParent
        }
        return false
    }

    private fun getMarkerClassQualifiedName(
        context: JavaContext,
        expression: UExpression?
    ): String? {
        if (expression != null) {
            val evaluated = expression.evaluate()
            if (evaluated is PsiClassType) {
                return context.evaluator.getQualifiedName(evaluated)
            }
        }
        return null
    }

    /**
     * Reports an issue and trims indentation on the [message].
     */
    private fun report(
        context: JavaContext,
        usage: UElement,
        message: String
    ) {
        context.report(ISSUE, usage, context.getNameLocation(usage), message.trimIndent())
    }

    /**
     * Returns all annotations for which [originalElement] is within-scope.
     */
    private fun getAnnotationsFromElement(
        context: JavaContext,
        originalElement: UElement
    ): Set<String> {
        val evaluator = context.evaluator
        val results = mutableSetOf<String>()
        var element: UElement? = originalElement
        while (element != null) {
            if (element is UAnnotated) {
                val annotations = evaluator
                    .getAllAnnotations(element, false)
                    .mapNotNull(UAnnotation::qualifiedName)
                    .toTypedArray()
                results.addAll(annotations)
            }
            element = element.uastParent
        }
        return results.toSet()
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ExperimentalDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        private const val EXPERIMENTAL_ANNOTATION = "androidx.annotation.Experimental"
        private const val USE_EXPERIMENTAL_ANNOTATION = "androidx.annotation.UseExperimental"

        @JvmField
        val ISSUE = Issue.create(
            id = "UnsafeExperimentalUsage",
            briefDescription = "Unsafe experimental usage",
            explanation = """
                TODO
            """,
            category = Category.CORRECTNESS,
            priority = 4,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }
}
