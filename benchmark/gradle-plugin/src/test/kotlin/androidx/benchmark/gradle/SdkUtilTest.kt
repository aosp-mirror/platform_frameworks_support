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

import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SdkUtilTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    companion object {
        const val BENCHMARK_PLUGIN_ID = "androidx.benchmark"
        const val SYSTEM_PROPERTY_ANDROID_HOME = "android.home"
    }

    private var oldAndroidHomeValue: String? = null

    @Before
    fun setUp() {
        oldAndroidHomeValue = System.clearProperty(SYSTEM_PROPERTY_ANDROID_HOME)
    }

    @After
    fun tearDown() {
        if (oldAndroidHomeValue == null) {
            System.clearProperty(SYSTEM_PROPERTY_ANDROID_HOME)
        } else {
            System.setProperty(SYSTEM_PROPERTY_ANDROID_HOME, oldAndroidHomeValue)
        }
    }

    @Test
    fun getSdkPathFromLocalProps() {
        testProjectDir.root.mkdirs()

        val localPropsFile = testProjectDir.newFile("local.properties")
        localPropsFile.createNewFile()
        localPropsFile.writeText("sdk.dir=/usr/test/location")

        val project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir.root)
            .build()
        project.apply { it.plugin(BENCHMARK_PLUGIN_ID) }

        Assert.assertEquals("/usr/test/location", SdkUtil.getSdkPath(project).path)
    }

    @Test
    fun getSdkPathFromSystemProperty() {
        testProjectDir.root.mkdirs()

        val project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir.root)
            .build()

        System.setProperty(SYSTEM_PROPERTY_ANDROID_HOME, "/usr/system/prop/location")
        Assert.assertEquals("/usr/system/prop/location", SdkUtil.getSdkPath(project).path)
    }

    @Test(expected = Exception::class)
    fun getSdkPathThrowsWhenMissing() {
        val project = ProjectBuilder.builder().build()
        SdkUtil.getSdkPath(project).path
    }
}
