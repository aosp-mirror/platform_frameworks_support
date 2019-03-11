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

import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test

//@Ignore("ANDROID_HOME not available on CI")
class FinallyInWhenStartedTest {

    private fun check(body: String): TestLintResult {
        return TestLintTask.lint().files(viewStub, lifecycleStub, kt(template(body)))
            .allowMissingSdk(true)
            .issues(FinallyInWhenStarted.ISSUE)
            .run()
    }

    private val lifecycleStub = kt(
        "androidx/lifecycle/PausingDispatcher.kt", """
        package androidx.lifecycle;

        interface Lifecycle {}

        fun random() {}
        fun Lifecycle.whenLifecycleStarted(block: () -> Unit) {}
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

    private fun template(body: String) = """
            package foo

            import androidx.lifecycle.Lifecycle
            import androidx.lifecycle.whenLifecycleStarted
            import android.view.FooView

            suspend fun suspendingFun() {
            }

            fun foo(val lifecycle: Lifecycle, view: FooView) {
                lifecycle.whenLifecycleStarted {
                    $body
                }
            }
        """.trimIndent()

    @Test
    fun accessViewInFinally() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()

        val expected = """
            src/foo/test.kt:15: Error: View is accessed from finally block [whenLifecycleStarted.finally]
                view.foo()
                ~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        check(input.trimIndent()).expect(expected)
    }

    @Test
    fun suspendInScope() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()

        val expected = """
            src/foo/test.kt:15: Error: View is accessed from finally block [whenLifecycleStarted.finally]
                view.foo()
                ~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        check(input.trimIndent()).expect(expected)
    }

}