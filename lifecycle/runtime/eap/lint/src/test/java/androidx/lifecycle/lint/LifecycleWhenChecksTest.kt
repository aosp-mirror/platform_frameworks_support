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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
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

        import kotlinx.coroutines.CoroutineScope

        interface Lifecycle {}
        interface CoroutineScope {}

        suspend fun <T> Lifecycle.whenStarted(block: suspend CoroutineScope.() -> T): T {
            throw Error()
        }

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

    private val TEMPLATE_SIZE_BEFORE_BODY = 5

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
        val l = IMPORTS.lines().size + lineNumber + TEMPLATE_SIZE_BEFORE_BODY

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
    fun tryWithNonSuspendLamda() {
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
    fun tryWithSuspendLamda() {
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
    fun suspendLamdaWithTry() {
        // some weird stuff is going, but it is not our business
        val input = """
            GlobalScope.launch {
                try {
                    suspendingFun()
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun nonSuspendLamdaWithTry() {
        // some weird stuff is going, but it is not our business
        val input = """
            "".apply {
                try {
                    suspendingFun()
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expect(error(5, "    view.foo()"))
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

    @Test
    fun tryInTrySuspendAfter() {
        val input = """
            try {
                try { } finally {}
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(5))
    }

    @Test
    fun tryInTrySuspendBefore() {
        val input = """
            try {
                suspendingFun()
                try { } finally {}
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(5))
    }

    @Test
    fun tryInTrySuspendInInnerSuspend() {
        val input = """
            try {
                try {
                    suspendingFun()
                } finally {
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(7))
    }

    @Test
    fun tryInTrySuspendInInnerFinally() {
        val input = """
            try {
                try {
                } finally {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(7))
    }

    @Test
    fun failingTryOkTry() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
            try{
                view.foo()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(4)).expectErrorCount(1)
    }

    @Test
    fun failingTrySuspendFunOkTry() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
            suspendingFun()
            try{
                view.foo()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(4)).expectErrorCount(1)
    }
}