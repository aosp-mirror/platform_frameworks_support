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

package androidx.room.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

@RunWith(JUnit4::class)
class CompileTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    internal var buildFile: File = File("")
    internal var prebuiltsRepo = ""
    internal var compileSdkVersion = ""
    internal var buildToolsVersion = ""
    internal var minSdkVersion = ""
    internal var debugKeystore = ""

    @Before
    fun setup() {
        projectRoot().mkdirs()
        buildFile = File(projectRoot(), "build.gradle")
        buildFile.createNewFile()
        // copy local.properties
        val appToolkitProperties = File("../../local.properties")
        if (appToolkitProperties.exists()) {
            appToolkitProperties.copyTo(
                File(projectRoot(), "local.properties"), overwrite = true)
        } else {
            File("../../local.properties").copyTo(
                File(projectRoot(), "local.properties"), overwrite = true)
        }
        val stream = CompileTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        prebuiltsRepo = properties.getProperty("prebuiltsRepo")
        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
        minSdkVersion = properties.getProperty("minSdkVersion")
        debugKeystore = properties.getProperty("debugKeystore")

        val propertiesFile = File(projectRoot(), "gradle.properties")
        propertiesFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.store(it, null)
        }
    }

    @Test
    fun simpleProject() {
        testData("simple-project").copyRecursively(projectRoot())
        buildFile.writeText("""
            buildscript {
                repositories {
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven { url "$prebuiltsRepo/androidx/internal" }
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:3.4.0-rc02'
                }
            }

            apply plugin: 'com.android.application'

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

                signingConfigs {
                    debug {
                        storeFile file("$debugKeystore")
                    }
                }
            }

            dependencies {
                implementation "androidx.room:room-runtime:+"
                annotationProcessor "androidx.room:room-compiler:+"
            }
        """.trimIndent())

        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")

        assertThat(generatedData("testapp/TestDatabase_Impl.java").exists()).isTrue()
        assertThat(generatedData("testapp/TestDao_Impl.java").exists()).isTrue()
    }

    private fun testData(name: String) = File("src/test/data/gradle", name)

    private fun generatedData(name: String) =
        File("${projectRoot()}/build/generated/source/apt/debug", name)

    val folder = Files.createTempDir()
    private fun projectRoot(): File = folder

    private fun gradleBuilder(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectRoot())
        .withArguments(*args)

    private fun runGradle(vararg args: String) = gradleBuilder(*args).build()

    private fun BuildResult.assertSuccessfulTask(name: String): BuildResult {
        MatcherAssert.assertThat(task(":$name")!!.outcome, CoreMatchers.`is`(TaskOutcome.SUCCESS))
        return this
    }
}