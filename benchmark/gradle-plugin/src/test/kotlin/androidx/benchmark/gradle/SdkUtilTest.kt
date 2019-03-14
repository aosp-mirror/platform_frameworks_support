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
import org.junit.Assert
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
    }

    @Test
    fun getSdkPathFromLocalProps() {
        val project = ProjectBuilder.builder().build()
        project.apply { it.plugin(BENCHMARK_PLUGIN_ID) }

        val localPropsFile = project.rootProject.file("local.properties")
        localPropsFile.createNewFile()
        localPropsFile.writeText(
            """
            sdk.dir=/usr/test/location
            """.trimIndent()
        )
        Assert.assertEquals("/usr/test/location", SdkUtil().getSdkPath(project).path)
    }

    @Test
    fun getSdkPathFromSystemProperty() {
        val project = ProjectBuilder.builder().build()

        System.setProperty("android.home", "/usr/system/prop/location")
        Assert.assertEquals("/usr/system/prop/location", SdkUtil().getSdkPath(project).path)
    }

    @Test(expected = Exception::class)
    fun getSdkPathThrowsWhenMissing() {
        val project = ProjectBuilder.builder().build()
        project.apply { it.plugin(BENCHMARK_PLUGIN_ID) }

        SdkUtil().getSdkPath(project).path
    }
}
