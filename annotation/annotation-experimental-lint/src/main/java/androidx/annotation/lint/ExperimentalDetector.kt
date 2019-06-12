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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement

class ExperimentalDetector : Detector(), SourceCodeScanner {
    override fun applicableAnnotations(): List<String>? = listOf(
        EXPERIMENTAL_ANNOTATION,
        USE_EXPERIMENTAL_ANNOTATION
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
        context.report(ISSUE, usage, context.getNameLocation(usage),
            "Illegal usage of experimental annotation: ${annotation.qualifiedName}")
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
