/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.dependencyTracker

import junit.framework.TestCase
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class DependencyTrackerTest {
    @Rule
    @JvmField
    val tmpDir = TemporaryFolder()
    @Test
    fun findDeps() {
        val parent = ProjectBuilder.builder()
            .withName("root")
            .build()
        val p1 = ProjectBuilder.builder()
            .withName("p1")
            .withParent(parent)
            .build()
        tmpDir.newFile("build.gradle").also {
            it.writeText(
                """
                    plugins {
                        id 'AndroidXBuildSrcTestPlugin'
                    }
                    task testIt {
                        doFirst {
                            project.depTracker.
                        }
                    }
                """.trimIndent()
            )
        }

        val result = GradleRunner
            .create()
            .withProjectDir(tmpDir.root)
            .withPluginClasspath()
            .build()
        result.tasks["a"]?.outcome == TaskOutcome.SUCCESS
        TestCase.assertEquals("Foo", result.output)
    }
}