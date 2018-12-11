/*
 * Copyright (C) 2018 The Android Open Source Project
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
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import java.util.Collections

class LimitReflectionUsage : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return Collections.singletonList(UImportStatement::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return ImportVisitor(context)
    }

    companion object {
        val ISSUE = Issue.create("LimitReflectionUsage",
                "Class imports java.lang.reflect",
                "Reflection is occasionally required (e.g. when loading classes" +
                        " from XML); however, there are many cases where AndroidX modules " +
                        "are using reflection to load classes in ways that can be avoided." +
                        " When we do end up using reflection, that usually results in " +
                        "overly-broad Proguard keep rules that prevent app developers " +
                        "from stripping unused classes. There are of course legitimate " +
                        "uses of reflection on platform APIs (e.g reflection from android.* " +
                        "is generally acceptable.) If you are confident that your use" +
                        " of reflection is justifiable then feel free to suppress this lint " +
                        "error using @SuppressLint(\"LimitReflectionUsage\")" +
                        " on top of your class declaration" +
                        ", otherwise kindly investigate a better way of accomplishing " +
                        "your goal.",
                Category.CORRECTNESS, 5, Severity.ERROR,
                Implementation(LimitReflectionUsage::class.java, Scope.JAVA_FILE_SCOPE))
    }

    class ImportVisitor(context: JavaContext) : UElementHandler() {
        val context = context
        override fun visitImportStatement(node: UImportStatement) {
            val resolved = node.resolve()
            if (resolved is PsiClass) {
                val qualifiedName = resolved.qualifiedName
                if (qualifiedName != null && qualifiedName.startsWith("java.lang.reflect")) {
                    context.report(ISSUE, node, context.getNameLocation(node),
                            "Class imports java.lang.reflect")
                }
            }
        }
    }
}
