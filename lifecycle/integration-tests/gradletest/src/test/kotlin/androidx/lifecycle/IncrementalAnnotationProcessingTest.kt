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

package androidx.lifecycle

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

@RunWith(JUnit4::class)
class IncrementalAnnotationProcessingTest {

    companion object {
        private const val MAIN_DIR = "app/src/main"
        private const val BUILD_DIR = "app/build"
        private const val SOURCE_DIR = "$MAIN_DIR/java/androidx/lifecycle/incap"
        private const val GENERATED_SOURCE_DIR = "$BUILD_DIR" +
                "/generated/source/apt/debug/androidx/lifecycle/incap"
        private const val GENERATED_PROGUARD_DIR = "$BUILD_DIR" +
                "/intermediates/javac/debug/compileDebugJavaWithJavac/classes/META-INF/proguard"
        private const val SEC = 1000L
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var projectRoot: File
    private lateinit var fooObserver: File
    private lateinit var barObserver: File
    private lateinit var fooAdapter: File
    private lateinit var barAdapter: File
    private lateinit var fooProguard: File
    private lateinit var barProguard: File
    private lateinit var projectConnection: ProjectConnection
    private lateinit var prebuiltsRepo: String
    private lateinit var compileSdkVersion: String
    private lateinit var buildToolsVersion: String
    private lateinit var minSdkVersion: String
    private lateinit var debugKeystore: String

    @Before
    fun setup() {
        projectRoot = testProjectDir.root
        fooObserver = File(projectRoot, "$SOURCE_DIR/FooObserver.java")
        barObserver = File(projectRoot, "$SOURCE_DIR/BarObserver.java")
        fooAdapter = File(projectRoot, "$GENERATED_SOURCE_DIR" +
                "/FooObserver_LifecycleAdapter.java")
        barAdapter = File(projectRoot, "$GENERATED_SOURCE_DIR" +
                "/BarObserver_LifecycleAdapter.java")
        fooProguard = File(projectRoot, "$GENERATED_PROGUARD_DIR" +
                "/androidx.lifecycle.incap.FooObserver.pro")
        barProguard = File(projectRoot, "$GENERATED_PROGUARD_DIR" +
                "/androidx.lifecycle.incap.BarObserver.pro")
        projectRoot.mkdirs()
        setProperties()
        setupProjectBuildGradle()
        setupAppBuildGradle()
        setupLocalProperties()
        setupSettingsGradle()
        setupAndroidManifest()
        addSource()

        projectConnection = GradleConnector.newConnector().useGradleVersion("5.4-rc-1")
            .forProjectDirectory(projectRoot).connect()
    }

    @Test
    fun checkModifySource() {
        val search = "FooObserver_Log"
        val replace = "Modified_FooObserver_Log"

        projectConnection
            .newBuild()
            .forTasks("clean", "compileDebugJavaWithJavac")
            .withArguments("--info")
            .setStandardOutput(System.out)
            .run()

        val fooAdapterFirstBuild = Files.getLastModifiedTime(fooAdapter.toPath())
        val barAdapterFirstBuild = Files.getLastModifiedTime(barAdapter.toPath())
        val fooProguardFirstBuild = Files.getLastModifiedTime(fooProguard.toPath())
        val barProguardFirstBuild = Files.getLastModifiedTime(barProguard.toPath())

        searchAndReplace(fooObserver.toPath(), search, replace)
        Thread.sleep(SEC)

        projectConnection
            .newBuild()
            .forTasks("compileDebugJavaWithJavac")
            .run()

        val fooAdapterSecondBuild = Files.getLastModifiedTime(fooAdapter.toPath())
        val barAdapterSecondBuild = Files.getLastModifiedTime(barAdapter.toPath())
        val fooProguardSecondBuild = Files.getLastModifiedTime(fooProguard.toPath())
        val barProguardSecondBuild = Files.getLastModifiedTime(barProguard.toPath())

        // FooObserver is recompiled and its proguard file is regenerated
        assertEquals(-1, fooAdapterFirstBuild.compareTo(fooAdapterSecondBuild).toLong())
        assertEquals(-1, fooProguardFirstBuild.compareTo(fooProguardSecondBuild).toLong())
        // BarObserver is not recompiled
        assertEquals(0, barAdapterFirstBuild.compareTo(barAdapterSecondBuild).toLong())
        assertEquals(0, barProguardFirstBuild.compareTo(barProguardSecondBuild).toLong())

        projectConnection.close()
    }

    @Test
    fun checkDeleteOneSource() {
        projectConnection
            .newBuild()
            .forTasks("clean", "compileDebugJavaWithJavac")
            .run()

        val barAdapterFirstBuild = Files.getLastModifiedTime(barAdapter.toPath())
        val barProguardFirstBuild = Files.getLastModifiedTime(barProguard.toPath())

        fooObserver.delete()
        Thread.sleep(SEC)

        projectConnection
            .newBuild()
            .forTasks("compileDebugJavaWithJavac")
            .run()

        val barAdapterSecondBuild = Files.getLastModifiedTime(barAdapter.toPath())
        val barProguardSecondBuild = Files.getLastModifiedTime(barProguard.toPath())

        // FooAdapter and FooProguard is deleted since FooObserver is removed
        assertFalse(fooAdapter.exists())
        assertFalse(fooProguard.exists())
        // BarObserver is not recompiled
        assertEquals(0, barAdapterFirstBuild.compareTo(barAdapterSecondBuild).toLong())
        assertEquals(0, barProguardFirstBuild.compareTo(barProguardSecondBuild).toLong())

        projectConnection.close()
    }

    private fun setupLocalProperties() {
        val commonProperties = File("../../../local.properties")
        commonProperties.copyTo(File(projectRoot, "local.properties"), overwrite = true)
    }

    private fun setupProjectBuildGradle() {
        val outDir = File("../../../../../out")
        val runningInBuildServer =
            System.getenv("DIST_DIR") != null && System.getenv("OUT_DIR") != null
        var repo: String
        if (runningInBuildServer) {
            repo = File(outDir, "gradle/frameworks/support/build/support_repo").absolutePath
        } else {
            repo = File(outDir, "host/gradle/frameworks/support/build/support_repo").absolutePath
        }
        addFileWithContent("build.gradle", """
            ext.repo = "$repo"
            buildscript {
                repositories {
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven { url "$prebuiltsRepo/androidx/internal" }
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:3.4.0-rc02'
                }
            }

            allprojects {
                repositories {
                    maven { url repo }
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven {
                        url "$prebuiltsRepo/androidx/internal"
                        content {
                            excludeModule("androidx.lifecycle", "lifecycle-compiler")
                        }
                    }
                }
            }

            task clean(type: Delete) {
                delete rootProject.buildDir
            }
        """.trimIndent())
    }

    private fun setupAppBuildGradle() {
        addFileWithContent("app/build.gradle", """
            apply plugin: 'com.android.application'

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
                implementation "androidx.lifecycle:lifecycle-runtime:+"
                annotationProcessor "androidx.lifecycle:lifecycle-compiler:+"
            }
        """.trimIndent())
    }

    private fun setupSettingsGradle() {
        addFileWithContent("settings.gradle", """
            include ':app'
        """.trimIndent())
    }

    private fun setupAndroidManifest() {
        addFileWithContent("$MAIN_DIR/AndroidManifest.xml", """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="androidx.lifecycle.incap">
            </manifest>
        """.trimIndent())
    }

    private fun addSource() {
        addFileWithContent("$SOURCE_DIR/FooObserver.java", """
            package androidx.lifecycle.incap;

            import android.util.Log;

            import androidx.lifecycle.Lifecycle;
            import androidx.lifecycle.LifecycleObserver;
            import androidx.lifecycle.OnLifecycleEvent;

            class FooObserver implements LifecycleObserver {
            private String mLog = "FooObserver_Log";

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    Log.i(mLog, "onResume");
                }
            }
        """.trimIndent())

        addFileWithContent("$SOURCE_DIR/BarObserver.java", """
            package androidx.lifecycle.incap;

            import android.util.Log;

            import androidx.lifecycle.Lifecycle;
            import androidx.lifecycle.LifecycleObserver;
            import androidx.lifecycle.OnLifecycleEvent;

            class BarObserver implements LifecycleObserver {
                private String mLog = "BarObserver_Log";

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    Log.i(mLog, "onResume");
                }
            }
        """.trimIndent())
    }

    private fun addFileWithContent(relativePath: String, content: String) {
        val file = File(projectRoot, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun searchAndReplace(file: Path, search: String, replace: String) {
        val content = String(Files.readAllBytes(file))
        val newContent = content.replace(search, replace)
        Files.write(file, newContent.toByteArray())
    }

    private fun setProperties() {
        // copy sdk.prop (created by module's build.gradle)
        IncrementalAnnotationProcessingTest::class.java.classLoader
            .getResourceAsStream("sdk.prop").use { input ->
            val properties = Properties().apply { load(input) }
            prebuiltsRepo = properties.getProperty("prebuiltsRepo")
            compileSdkVersion = properties.getProperty("compileSdkVersion")
            buildToolsVersion = properties.getProperty("buildToolsVersion")
            minSdkVersion = properties.getProperty("minSdkVersion")
            debugKeystore = properties.getProperty("debugKeystore")
        }
    }
}