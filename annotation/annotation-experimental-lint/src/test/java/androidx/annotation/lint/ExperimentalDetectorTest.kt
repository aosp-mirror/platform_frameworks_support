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

import androidx.annotation.lint.stubs.EXPERIMENTAL_STUB
import androidx.annotation.lint.stubs.USE_EXPERIMENTAL_STUB
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
        return lint()
            .files(
                EXPERIMENTAL_STUB,
                USE_EXPERIMENTAL_STUB,
                *code.map(TestFile::indented).toTypedArray()
            )
            .allowMissingSdk(true)
            .issues(ExperimentalDetector.ISSUE)
            .run()
    }

    @Test
    fun useExperimentalClassUnchecked() {
        val input = java("""
            package sample;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassUnchecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {
                    Date getDate() { return null; }
                }

                Date getDate() {
                    DateProvider provider = new DateProvider();
                    return provider.getDate();
                }
            }
        """)

        val expected = """
src/sample/UseExperimentalClassUnchecked.java:20: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalClassUnchecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalClassUnchecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsage]
        DateProvider provider = new DateProvider();
                                ~~~~~~~~~~~~~~~~~~
src/sample/UseExperimentalClassUnchecked.java:21: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalClassUnchecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalClassUnchecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsage]
        return provider.getDate();
                        ~~~~~~~
2 errors, 0 warnings
        """

        checkJava(input)
            .expect(expected.trimIndent())
    }

    @Test
    fun useExperimentalClassChecked() {
        val input = java("""
            package sample;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassChecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {
                    Date getDate() { return null; }
                }

                @ExperimentalDateTime
                Date getDate() {
                    DateProvider provider = new DateProvider();
                    return provider.getDate();
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
            package sample;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassChecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {
                    Date getDate() { return null; }
                }

                @ExperimentalDateTime
                Date getDate() {
                    DateProvider provider = new DateProvider();
                    return provider.getDate();
                }

                void displayDate() {
                    System.out.println(getDate());
                }
            }
        """)

        val expected = """
src/sample/UseExperimentalClassChecked.java:26: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalClassChecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalClassChecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsage]
        System.out.println(getDate());
                           ~~~~~~~
1 errors, 0 warnings
        """.trimIndent()

        checkJava(input).expect(expected)
    }

    @Test
    fun useExperimentalClassCheckedUses() {
        val input = java("""
            package sample;

            import static androidx.annotation.Experimental.Level.ERROR;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import androidx.annotation.Experimental;
            import androidx.annotation.UseExperimental;
            import java.lang.annotation.Retention;

            public class UseExperimentalClassChecked {
                @Retention(SOURCE)
                @Experimental(ERROR)
                public @interface ExperimentalDateTime {}

                @ExperimentalDateTime
                class DateProvider {
                    Date getDate() { return null; }
                }

                @UseExperimental(ExperimentalDateTime.class)
                Date getDate() {
                    DateProvider provider = new DateProvider();
                    return provider.getDate();
                }
            }
        """)

        val expected = "No warnings."

        checkJava(input)
            .expect(expected.trimIndent())
    }
}
