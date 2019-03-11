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

import androidx.lifecycle.lint.LifecycleWhenChecks.Companion.ISSUE
import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class LifecycleWhenChecksTest {

    private fun check(body: String): TestLintResult {
        return TestLintTask.lint().files(viewStub, lifecycleStub, kt(template(body)))
            .allowMissingSdk(true)
            .issues(ISSUE)
            .run()
    }

    private val lifecycleStub = kt(
        "androidx/lifecycle/PausingDispatcher.kt", """
        package androidx.lifecycle;

        interface Lifecycle {}

        fun Lifecycle.whenStarted(block: () -> Unit) {}

        object GlobalScope {
            fun launch(block: suspend (b: Lifecycle) -> Unit) {}
        }

    """.trimIndent()
    ).within("src")

    private val viewStub = kt(
        """
        package android.view

        class View {}

        class FooView: View() {
            fun foo() {}
        }
    """.trimIndent()
    )

    private val IMPORTS = """
            import androidx.lifecycle.Lifecycle
            import androidx.lifecycle.whenStarted
            import android.view.FooView
            import androidx.lifecycle.GlobalScope
    """

    private fun template(body: String) = """
            package foo

            $IMPORTS

            fun foo(val lifecycle: Lifecycle, view: FooView) {
                lifecycle.whenStarted {
                    $body
                }
            }

            suspend fun suspendingFun() {
            }

            suspend fun suspendWithTryCatch() {
                try {
                    suspendingFun()
                } finally {
                    FooView().foo()
                }
            }
        """.trimIndent()

    fun error(lineNumber: Int, customExpression: String = "view.foo()"): String {
        val l = IMPORTS.lines().size + lineNumber + 5

        val trimmed = customExpression.trimStart().length
        val highlight = " ".repeat(customExpression.length - trimmed) + "~".repeat(trimmed)
        return """
            src/foo/test.kt:$l: Error: $VIEW_ERROR_MESSAGE [${ISSUE.id}]
                $customExpression
                $highlight
            1 errors, 0 warnings
        """.trimIndent()
    }

    @Test
    fun accessViewInFinally() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()

        check(input.trimIndent()).expect(error(4))
    }

    @Test
    fun suspendInMap() {
        val input = """
            try {
                "".apply {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()

        check(input.trimIndent()).expect(error(6))
    }

    @Test
    fun suspendInGlobalScope() {
        val input = """
            try {
                GlobalScope.launch {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun visitResolvedMethod() {
        val input = "suspendWithTryCatch()"
        check(input).expect(error(12, "    FooView().foo()"))
    }

    @Test
    fun finallyWithWhenFinally() {
        val input = """
            try {
                suspendingFun()
            } finally {
                lifecycle.whenStarted {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input).expectClean()
    }
}