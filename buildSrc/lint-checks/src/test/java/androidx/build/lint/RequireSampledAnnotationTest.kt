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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RequireSampledAnnotationTest {
    // IMPORTANT - this must start with src/ or else lint won't run
    private val projectDir = "src/foo/bar"
    private val sourceDir = "$projectDir/src"
    private val samplesDir = "$projectDir/integration-tests/samples"

    private val sampledStub = kotlin("""
        package androidx.annotation;

        annotation class Sampled
    """.trimIndent())

    private fun checkKotlin(vararg code: TestFile): TestLintResult {
        return lint()
            .files(
                sampledStub,
                *code.map { it.indented() }.toTypedArray()
            )
            .allowMissingSdk(true)
            .issues(
                RequireSampledAnnotation.OBSOLETE_ANNOTATION,
                RequireSampledAnnotation.MISSING_ANNOTATION
            )
            .run()
    }

    @Test
    fun orphanedSampleFunction() {
        val sample = kotlin("""
            package samplepackage

            @Sampled
            fun sampleFun() {}
        """).within(samplesDir)

        val expected = """
src/foo/bar/integration-tests/samples/samplepackage/test.kt:4: Error: sampleFun is annotated""" +
""" with @Sampled, but is not linked to from a @sample tag. [RequireSampledAnnotation]
fun sampleFun() {}
    ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(sample)
            .expect(expected.trimIndent())
    }

    @Test
    fun unannotatedSampleFunction() {
        val source = kotlin("""
            package sourcepackage

            class Source {
              /**
               * @sample samplepackage.sampleFun
               */
              fun sampleFun() {}
            }
        """).within(sourceDir)

        val sample = kotlin("""
            package samplepackage

            fun sampleFun() {}
        """).within(samplesDir)

        val expected = """
src/foo/bar/src/sourcepackage/Source.kt:5: Error: sampleFun is not annotated with @Sampled, but""" +
""" is linked to from the KDoc of sampleFun [RequireSampledAnnotation]
   * @sample samplepackage.sampleFun
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(source, sample)
            .expect(expected.trimIndent())
    }

    @Test
    fun correctlyAnnotatedSampleFunction() {
        val source = kotlin("""
            package sourcepackage

            class Source {
              /**
               * @sample samplepackage.sampleFun
               */
              fun sampleFun() {}
            }
        """).within(sourceDir)

        val sample = kotlin("""
            package samplepackage

            @Sampled
            fun sampleFun() {}
        """).within(samplesDir)

        checkKotlin(source, sample)
            .expectClean()
    }
}
