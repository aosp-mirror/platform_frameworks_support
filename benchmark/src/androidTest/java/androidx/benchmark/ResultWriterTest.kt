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

package androidx.benchmark

import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ResultWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val reportA = BenchmarkState.Report(
        testName = "MethodA",
        className = "package.Class1",
        nanos = 100,
        data = listOf(100, 101, 102),
        repeatIterations = 100000,
        warmupIterations = 8000
    )
    private val reportB = BenchmarkState.Report(
        testName = "MethodB",
        className = "package.Class2",
        nanos = 100,
        data = listOf(100, 101, 102),
        repeatIterations = 100000,
        warmupIterations = 8000
    )

    @Before
    fun setup() {
        ResultWriter.reports.clear()
    }

    @After
    fun tearDown() {
        ResultWriter.reports.clear()
    }

    @Test
    fun shouldClearExistingContent() {
        val tempFile = tempFolder.newFile()

        val fakeText = "This text should not be in the final output"
        tempFile.writeText(fakeText)

        ResultWriter.appendReport(reportA)
        ResultWriter.appendReport(reportB)

        ResultWriter.writeReport(tempFile)
        assert(!tempFile.readText().startsWith(fakeText))
    }

    @Test
    fun validateJson() {
        val tempFile = tempFolder.newFile()

        ResultWriter.appendReport(reportA)
        ResultWriter.appendReport(reportB)
        ResultWriter.writeReport(tempFile)

        assertEquals(
            """
            [
                {
                    "name": "MethodA",
                    "className": "package.Class1",
                    "nanos": 100,
                    "warmupIterations": 8000,
                    "repeatIterations": 100000,
                    "runs": [
                        100,
                        101,
                        102
                    ]
                },
                {
                    "name": "MethodB",
                    "className": "package.Class2",
                    "nanos": 100,
                    "warmupIterations": 8000,
                    "repeatIterations": 100000,
                    "runs": [
                        100,
                        101,
                        102
                    ]
                }
            ]
            """.trimIndent(),
            tempFile.readText()
        )
    }
}
