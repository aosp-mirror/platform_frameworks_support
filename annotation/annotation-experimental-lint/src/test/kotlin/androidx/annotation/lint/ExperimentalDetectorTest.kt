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

    private fun checkJava(testFile: TestFile): TestLintResult {
        return lint()
            .files(
                javaSample("androidx.annotation.Experimental"),
                javaSample("androidx.annotation.UseExperimental"),
                testFile
            )
            .allowMissingSdk(true)
            .issues(*ExperimentalDetector.ISSUES.toTypedArray())
            .run()
    }

    /**
     * Loads a [TestFile] from Java source code included in the JAR resources.
     */
    private fun javaSample(className: String): TestFile {
        return java(javaClass.getResource("/${className.replace('.','/')}.java").readText())
    }

    @Test
    fun useExperimentalClassUnchecked() {
        val input = javaSample("sample.UseExperimentalClassUnchecked")

        val expected = """
src/sample/UseExperimentalClassUnchecked.java:41: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalClassUnchecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalClassUnchecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        DateProvider provider = new DateProvider();
                                ~~~~~~~~~~~~~~~~~~
src/sample/UseExperimentalClassUnchecked.java:42: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalClassUnchecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalClassUnchecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        return provider.getDate();
                        ~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        checkJava(input).expect(expected)
    }

    @Test
    fun useExperimentalClassChecked() {
        val input = javaSample("sample.UseExperimentalClassChecked")

        val expected = "No warnings."

        checkJava(input).expect(expected)
    }

    @Test
    fun useExperimentalMethodUnchecked() {
        val input = javaSample("sample.UseExperimentalMethodUnchecked")

        val expected = """
src/sample/UseExperimentalMethodUnchecked.java:47: Error: This declaration is experimental and its usage should be marked with
'@sample.UseExperimentalMethodUnchecked.ExperimentalDateTime' or '@UseExperimental(sample.UseExperimentalMethodUnchecked.ExperimentalDateTime.class)' [UnsafeExperimentalUsageError]
        System.out.println(getDate());
                           ~~~~~~~
1 errors, 0 warnings
        """.trimIndent()

        checkJava(input).expect(expected)
    }

    @Test
    fun useExperimentalClassCheckedUses() {
        val input = javaSample("sample.UseExperimentalClassCheckedUses")

        val expected = "No warnings."

        checkJava(input).expect(expected)
    }
}
