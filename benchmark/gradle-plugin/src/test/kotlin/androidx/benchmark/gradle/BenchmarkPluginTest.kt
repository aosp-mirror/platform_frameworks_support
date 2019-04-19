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
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties
import kotlin.streams.toList

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private val pluginClasspathStream =
        BenchmarkPluginTest::class.java.classLoader.getResourceAsStream("plugin-classpath.txt")
            ?: throw IllegalStateException(
                "Did not find plugin classpath resource, run `testClasses` build task."
            )
    private val pluginClasspath = pluginClasspathStream.bufferedReader().lines().map { File(it) }

    private lateinit var gradlePropFile: File
    private lateinit var localPropFile: File
    private lateinit var project: Project

    private lateinit var compileSdkVersion: String
    private lateinit var buildToolsVersion: String
    private lateinit var prebuiltsRepo: String
    private lateinit var minSdkVersion: String

    @Before
    fun setUp() {
        testProjectDir.root.mkdirs()

        // TODO: Consolidate mock gradle project logic with safeargs plugin test logic.
        val stream = BenchmarkPluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)

        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
        prebuiltsRepo = properties.getProperty("prebuiltsRepo")
        minSdkVersion = properties.getProperty("minSdkVersion")

        // copy local.properties
        localPropFile = File("../../local.properties")
        localPropFile.copyTo(
            File(testProjectDir.root, "local.properties"),
            overwrite = true
        )

        gradlePropFile = File(testProjectDir.root, "gradle.properties")
        gradlePropFile.createNewFile()
        gradlePropFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.setProperty("android.enableJetifier", "true")
            props.store(it, null)
        }

        File("src/test/test-data", "app-project").copyRecursively(testProjectDir.root)
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir.root)
            .build()
    }

    @Test
    fun applyPluginAppProject() {
        val buildFile = File(testProjectDir.root, "build.gradle")
        buildFile.createNewFile()
        buildFile.writeText(
            """
            plugins {
                id 'com.android.application'
                id 'androidx.benchmark'
            }

            repositories {
                maven { url "$prebuiltsRepo/androidx/external" }
                maven { url "$prebuiltsRepo/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                }
            }
        """.trimIndent()
        )

        val project: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath(pluginClasspath.toList())
            .withArguments("tasks")
            .build()

        Assert.assertTrue(project.output.contains("lockClocks"))
        Assert.assertTrue(project.output.contains("unlockClocks"))
    }

    @Test
    fun applyPluginAndroidLibProject() {
        project.apply { it.plugin("com.android.library") }
        project.apply { it.plugin("androidx.benchmark") }

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test(expected = PluginApplicationException::class)
    fun applyPluginNonAndroidProject() {
        project.apply { it.plugin("java-library") }
        project.apply { it.plugin("androidx.benchmark") }

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }

    @Test
    fun applyPluginNonAndroidXProject() {
        project.apply { it.plugin("com.android.library") }
        project.apply { it.plugin("androidx.benchmark") }

        project.setProperty("android.useAndroidX", "false")
        project.setProperty("android.enableJetifier", "false")

        Assert.assertNotNull(project.tasks.findByPath("lockClocks"))
        Assert.assertNotNull(project.tasks.findByPath("unlockClocks"))
    }
}
