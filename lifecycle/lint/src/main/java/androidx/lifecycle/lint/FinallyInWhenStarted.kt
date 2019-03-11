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

package androidx.lifecycle.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

class FinallyInWhenStarted : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("whenLifecycleStarted")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)
        if (isLifecycleWhenExtension(context, method)) {
            node.accept(LifecycleWhenVisitor(context))
        }
    }

    companion object {
        val ISSUE = Issue.create(
                id = "whenLifecycleStarted.finally",
                briefDescription = "bla",
                explanation = "bla",
                category = Category.CORRECTNESS,
                severity = Severity.ERROR,
                implementation = Implementation(FinallyInWhenStarted::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}

class LifecycleWhenVisitor(private val context: JavaContext) : AbstractUastVisitor() {
    override fun visitTryExpression(node: UTryExpression): Boolean {
        val visitor = HasSuspendingCallsVisitor(context)
        node.tryClause.accept(visitor)
        if (visitor.hasSuspendingCalls()) {
            node.finallyClause?.accept(TouchViewVisitor(context))
        }
        return super.visitTryExpression(node)
    }
}

class TouchViewVisitor(private val context: JavaContext) : AbstractUastVisitor() {
    override fun visitCallExpression(node: UCallExpression): Boolean {
        val receiverClass = PsiTypesUtil.getPsiClass(node.receiverType)
        if (context.evaluator.extendsClass(receiverClass, VIEW_CLASS_NAME, true)) {
            context.report(FinallyInWhenStarted.ISSUE, node, context.getLocation(node),
                    "View is accessed from finally block")
        }
        return super.visitCallExpression(node)
    }
}

class HasSuspendingCallsVisitor(private val context: JavaContext) : AbstractUastVisitor() {
    private var hasSuspedingCalls = false

    // we found valid suspend call - don't care any more
    override fun visitExpression(node: UExpression) = hasSuspedingCalls

    fun hasSuspendingCalls() = hasSuspedingCalls

    override fun visitCallExpression(node: UCallExpression): Boolean {
        hasSuspedingCalls = hasSuspedingCalls or (node.resolve()?.isSuspend() ?: false)
        return super.visitCallExpression(node)
    }
}

private val DISPATCHER_CLASS_NAME = "androidx.lifecycle.PausingDispatcherKt"
private val VIEW_CLASS_NAME = "android.view.View"

private fun isLifecycleWhenExtension(context: JavaContext, method: PsiMethod): Boolean {
    if (!context.evaluator.isMemberInClass(method, DISPATCHER_CLASS_NAME)) {
        return false
    }

    if (!context.evaluator.isStatic(method)) {
        return false
    }

    return true
}

private fun PsiMethod.isSuspend(): Boolean {
    val modifiers = modifierList as? KtLightModifierList<*>
    return modifiers?.kotlinOrigin?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: false
}