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

import com.google.common.truth.Truth.assertThat
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.junit.After
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
        private const val GENERATED_SOURCE_DIR = BUILD_DIR +
                "/generated/source/apt/debug/androidx/lifecycle/incap"
        private const val CLASSES_DIR = BUILD_DIR +
                "/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
        private const val GENERATED_PROGUARD_DIR = "$CLASSES_DIR/META-INF/proguard"
        private const val APP_CLASS_DIR = "$CLASSES_DIR/androidx/lifecycle/incap"
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var projectRoot: File
    private lateinit var fooObserver: File
    private lateinit var barObserver: File
    private lateinit var genFooAdapter: File
    private lateinit var genBarAdapter: File
    private lateinit var genFooProguard: File
    private lateinit var genBarProguard: File
    private lateinit var fooObserverClass: File
    private lateinit var barObserverClass: File
    private lateinit var genFooAdapterClass: File
    private lateinit var genBarAdapterClass: File
    private lateinit var projectConnection: ProjectConnection
    private lateinit var prebuiltsRepo: String
    private lateinit var compileSdkVersion: String
    private lateinit var buildToolsVersion: String
    private lateinit var minSdkVersion: String
    private lateinit var debugKeystore: String
    private lateinit var agpVersion: String

    @Before
    fun setup() {
        projectRoot = testProjectDir.root
        fooObserver = File(projectRoot, "$SOURCE_DIR/FooObserver.java")
        barObserver = File(projectRoot, "$SOURCE_DIR/BarObserver.java")
        genFooAdapter = File(projectRoot, GENERATED_SOURCE_DIR +
                "/FooObserver_LifecycleAdapter.java")
        genBarAdapter = File(projectRoot, GENERATED_SOURCE_DIR +
                "/BarObserver_LifecycleAdapter.java")
        genFooProguard = File(projectRoot, GENERATED_PROGUARD_DIR +
                "/androidx.lifecycle.incap.FooObserver.pro")
        genBarProguard = File(projectRoot, GENERATED_PROGUARD_DIR +
                "/androidx.lifecycle.incap.BarObserver.pro")
        fooObserverClass = File(projectRoot, "$APP_CLASS_DIR/FooObserver.class")
        barObserverClass = File(projectRoot, "$APP_CLASS_DIR/BarObserver.class")
        genFooAdapterClass = File(projectRoot, APP_CLASS_DIR +
                "/FooObserver_LifecycleAdapter.class")
        genBarAdapterClass = File(projectRoot, APP_CLASS_DIR +
                "/BarObserver_LifecycleAdapter.class")
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
        projectConnection
            .newBuild()
            .forTasks("clean", "compileDebugJavaWithJavac")
            .withArguments("--info") // TODO
            .setStandardOutput(System.out) // TODO
            .run()

        val fooAdapterFirstBuild = Files.getLastModifiedTime(genFooAdapter.toPath())
        val barAdapterFirstBuild = Files.getLastModifiedTime(genBarAdapter.toPath())
        val fooProguardFirstBuild = Files.getLastModifiedTime(genFooProguard.toPath())
        val barProguardFirstBuild = Files.getLastModifiedTime(genBarProguard.toPath())
        val fooObserverClassFirstBuild = Files.getLastModifiedTime(fooObserverClass.toPath())
        val barObserverClassFirstBuild = Files.getLastModifiedTime(barObserverClass.toPath())
        val fooAdapterClassFirstBuild = Files.getLastModifiedTime(genFooAdapterClass.toPath())
        val barAdapterClassFirstBuild = Files.getLastModifiedTime(genBarAdapterClass.toPath())

        searchAndReplace(fooObserver.toPath(), "FooObserver_Log", "Modified_FooObserver_Log")

        projectConnection
            .newBuild()
            .forTasks("compileDebugJavaWithJavac")
            .run()

        val fooAdapterSecondBuild = Files.getLastModifiedTime(genFooAdapter.toPath())
        val barAdapterSecondBuild = Files.getLastModifiedTime(genBarAdapter.toPath())
        val fooProguardSecondBuild = Files.getLastModifiedTime(genFooProguard.toPath())
        val barProguardSecondBuild = Files.getLastModifiedTime(genBarProguard.toPath())
        val fooObserverClassSecondBuild = Files.getLastModifiedTime(fooObserverClass.toPath())
        val barObserverClassSecondBuild = Files.getLastModifiedTime(barObserverClass.toPath())
        val fooAdapterClassSecondBuild = Files.getLastModifiedTime(genFooAdapterClass.toPath())
        val barAdapterClassSecondBuild = Files.getLastModifiedTime(genBarAdapterClass.toPath())

        // FooObserver's adapter and its proguard file are regenerated
        // FooObserver and its regenerated adapter are recompiled
        assertThat(fooAdapterFirstBuild.compareTo(fooAdapterSecondBuild)).isEqualTo(-1)
        assertThat(fooProguardFirstBuild.compareTo(fooProguardSecondBuild)).isEqualTo(-1)
        assertThat(fooObserverClassFirstBuild.compareTo(fooObserverClassSecondBuild)).isEqualTo(-1)
        assertThat(fooAdapterClassFirstBuild.compareTo(fooAdapterClassSecondBuild)).isEqualTo(-1)
        // BarObserver's adapter and its proguard are not regenerated
        // BarObserver and its generated adapter are not recompiled
        assertThat(barAdapterFirstBuild.compareTo(barAdapterSecondBuild)).isEqualTo(0)
        assertThat(barProguardFirstBuild.compareTo(barProguardSecondBuild)).isEqualTo(0)
        assertThat(barObserverClassFirstBuild.compareTo(barObserverClassSecondBuild)).isEqualTo(0)
        assertThat(barAdapterClassFirstBuild.compareTo(barAdapterClassSecondBuild)).isEqualTo(0)
    }

    @Test
    fun checkDeleteOneSource() {
        projectConnection
            .newBuild()
            .forTasks("clean", "compileDebugJavaWithJavac")
            .run()

        val barAdapterFirstBuild = Files.getLastModifiedTime(genBarAdapter.toPath())
        val barProguardFirstBuild = Files.getLastModifiedTime(genBarProguard.toPath())
        val barObserverClassFirstBuild = Files.getLastModifiedTime(barObserverClass.toPath())
        val barAdapterClassFirstBuild = Files.getLastModifiedTime(genBarAdapterClass.toPath())

        assertThat(genFooAdapter.exists()).isTrue()
        assertThat(genFooProguard.exists()).isTrue()

        fooObserver.delete()

        projectConnection
            .newBuild()
            .forTasks("compileDebugJavaWithJavac")
            .run()

        val barAdapterSecondBuild = Files.getLastModifiedTime(genBarAdapter.toPath())
        val barProguardSecondBuild = Files.getLastModifiedTime(genBarProguard.toPath())
        val barObserverClassSecondBuild = Files.getLastModifiedTime(barObserverClass.toPath())
        val barAdapterClassSecondBuild = Files.getLastModifiedTime(genBarAdapterClass.toPath())

        // FooObserver's adapter and its proguard file are deleted since FooObserver is removed
        assertThat(genFooAdapter.exists()).isFalse()
        assertThat(genFooProguard.exists()).isFalse()
        // BarObserver's adapter and its proguard are not regenerated
        // BarObserver and its generated adapter are not recompiled
        assertThat(barAdapterFirstBuild.compareTo(barAdapterSecondBuild)).isEqualTo(0)
        assertThat(barProguardFirstBuild.compareTo(barProguardSecondBuild)).isEqualTo(0)
        assertThat(barObserverClassFirstBuild.compareTo(barObserverClassSecondBuild)).isEqualTo(0)
        assertThat(barAdapterClassFirstBuild.compareTo(barAdapterClassSecondBuild)).isEqualTo(0)
    }

    @After
    fun closeProjectConnection() {
        projectConnection.close()
    }

    private fun setupLocalProperties() {
        val commonProperties = File("../../../local.properties")
        commonProperties.copyTo(File(projectRoot, "local.properties"), overwrite = true)
    }

    private fun setupProjectBuildGradle() {
        // Provide a maven repo which contains necessary artifacts built from tip of tree
        val repo = File("../../../../../out/support/build/support_repo").absolutePath

        addFileWithContent("build.gradle", """
            ext.repo = "$repo"
            buildscript {
                repositories {
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven { url "$prebuiltsRepo/androidx/internal" }
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:$agpVersion"
                }
            }

            allprojects {
                repositories {
                    maven { url repo }
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven {
                        url "$prebuiltsRepo/androidx/internal"
                        // Get artifacts to be tested from provided repo instead of prebuiltsRepo
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
                // Use the latest lifecycle-runtime to keep up with lifecycle-compiler
                implementation "androidx.lifecycle:lifecycle-runtime:+"
                // Use the latest version to test lifecycle-compiler artifact built from tip of tree
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
        // copy sdk.prop (created by generateSdkResource task in build.gradle)
        IncrementalAnnotationProcessingTest::class.java.classLoader
            .getResourceAsStream("sdk.prop").use { input ->
            val properties = Properties().apply { load(input) }
            prebuiltsRepo = properties.getProperty("prebuiltsRepo")
            compileSdkVersion = properties.getProperty("compileSdkVersion")
            buildToolsVersion = properties.getProperty("buildToolsVersion")
            minSdkVersion = properties.getProperty("minSdkVersion")
                debugKeystore = properties.getProperty("debugKeystore")
                agpVersion = properties.getProperty("agpVersion")
        }
    }
}