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

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExperimentalDetectorTest {

    private fun checkJava(vararg code: TestFile): TestLintResult {
        return lint().files(*code)
            .allowMissingSdk(true)
            .issues(ExperimentalDetector.ISSUE)
            .run()
    }

    @Test
    fun useExperimentalClassUnchecked() {
        val input = java("""
            package foo;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassUnchecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                public class DateProvider {}

                public int getYear() {
                    DateProvider provider;
                    return -1;
                }
            }
        """)

        val expected = """

        """

        checkJava(input)
            .expect(expected.trimIndent())
    }

    @Test
    fun useExperimentalClassChecked() {
        val input = java("""
            package foo;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassChecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {}

                @ExperimentalDateTime
                Date getDate() {
                    DateProvider provider;
                    return null;
                }
            }
        """)

        val expected = "No warnings."

        checkJava(input)
            .expect(expected.trimIndent())
    }

    @Test
    fun useExperimentalMethodUnchecked() {
        val input = java("""
            package foo;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassChecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {}

                @ExperimentalDateTime
                Date getDate() {
                    DateProvider provider;
                    return null;
                }

                void displayDate() {
                    System.out.println(getDate());
                }
            }
        """)

        val expected = """

        """

        checkJava(input)
            .expect(expected.trimIndent())
    }
}
