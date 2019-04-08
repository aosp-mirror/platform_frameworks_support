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

package androidx.benchmark.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var project: Project
    private lateinit var configuration: Configuration

    private val benchmarkDependency = object : Dependency {
        override fun getGroup(): String? = "androidx"

        override fun getName(): String = "benchmark"

        override fun getVersion(): String? = "1.0.0-alpha01"

        override fun contentEquals(p0: Dependency): Boolean = false

        override fun copy(): Dependency = this

        override fun because(p0: String?) = Unit

        override fun getReason(): String? = null
    }

    @Before
    fun setUp() {
        testProjectDir.root.mkdirs()

        val localPropFile = File("../../local.properties")
        localPropFile.copyTo(File(testProjectDir.root, "local.properties"), overwrite = true)

        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir.root)
            .build()

        configuration = project.configurations.create("implementation")
        project.configurations.add(configuration)
        configuration.dependencies.add(benchmarkDependency)
    }

    @After
    fun tearDown() {
        project.configurations.remove(configuration)
    }

    @Test
    fun applyPluginAppProject() {
        project.apply { it.plugin("com.android.application") }
        project.apply { it.plugin("androidx.benchmark") }

        assertNotNull(project.tasks.findByPath("lockClocks"))
        assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test
    fun applyPluginAndroidLibProject() {
        project.apply { it.plugin("com.android.library") }
        project.apply { it.plugin("androidx.benchmark") }

        assertNotNull(project.tasks.findByPath("lockClocks"))
        assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test
    fun applyPluginNonAndroidProject() {
        project.apply { it.plugin("java-library") }
        assertFailsWith(PluginApplicationException::class) {
            project.apply { it.plugin("androidx.benchmark") }
        }
    }

    @Test
    fun applyPluginNonBenchmarkProject() {
        configuration.dependencies.remove(benchmarkDependency)
        project.apply { it.plugin("com.android.library") }

        assertFailsWith(PluginApplicationException::class) {
            project.apply { it.plugin("androidx.benchmark") }
        }
    }

    @Test
    fun applyPluginBeforeAgp() {
        assertFailsWith(PluginApplicationException::class) {
            project.apply { it.plugin("androidx.benchmark") }
        }
    }
}
