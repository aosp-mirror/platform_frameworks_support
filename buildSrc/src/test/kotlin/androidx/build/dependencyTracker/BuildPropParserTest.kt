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

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BuildPropParserTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun parseProps() {
        val repoProps = tmpFolder.newFile("repo.prop")
        repoProps.writeText(
                """
                    platform/external/doclava 798edee4d01239248ed33585a42b19ca93c31f56
                    platform/external/flatbuffers b5c9b8e12b47d0597cd29b217e5765d325957d8a
                    platform/external/jdiff fe19ba361e4a1230108378b4634784281dd9d250
                    platform/external/noto-fonts d20a289eaebf2e371064dacc306b57d37b733f7c
                    platform/external/webview_support_interfaces 023152ac4ec8293b0496ab11ab0280c3ef5e1f02
                    platform/frameworks/support fc9030edfe7e0a85a5545a342c7efbf93283f62f
                    platform/manifest 446c4307abb8d676b308af55b06e1d1a01c858c7
                    platform/prebuilts/androidx/external 28219bd26d4ef59e133a85189404db8e73ec0f70
                    platform/prebuilts/androidx/internal dbbb264e785643cb8089c3a5ed7f8b25c4207738
                    platform/prebuilts/checkstyle c52010acac9b638f45f66747762cc0ad187c1e39
                    platform/prebuilts/fullsdk-darwin/build-tools/27.0.3 7cd5865ddc02204dea19256b159cdd4f06756c34
                    platform/prebuilts/fullsdk-darwin/platform-tools ff82338964036bea88e26a748fa0e1109d2f4cd3
                    platform/prebuilts/fullsdk-darwin/tools 204b8813ffedef10214a4afe04c843f1ee9e4fe7
                    platform/prebuilts/fullsdk-linux/build-tools/27.0.3 e300d12969bca21bdf53bd03b1d0367b5317138a
                    platform/prebuilts/fullsdk-linux/platform-tools 83a183b4bced4377eb5817074db82885cfcae393
                    platform/prebuilts/fullsdk-linux/tools 2d5e66a40bd1e6d43ee458efea6f1dd5d5166a59
                    platform/prebuilts/fullsdk/platforms/android-28 7dd7e37b7d60cf8291f267168760327998a96f6b
                    platform/prebuilts/ktlint 9a3378cb74a0ab30433181a60371673cb6eb5643
                    platform/tools/external/gradle 29b86bd23797fe0873ffe18f12907cd5eea5d427
                    platform/tools/repohooks bbc97c1419402c3d0297189c2d34228cbb60c2e6
                """.trimIndent(),
                Charsets.UTF_8
        )
        val appliedProp = tmpFolder.newFile("applied.prop")
        appliedProp.writeText(
                """
                    frameworks/support e31b2fa85ae9cb414023346684060d1009bf4c11
                """.trimIndent(),
                Charsets.UTF_8
        )
        val info = BuildPropParser.getShaForThisBuild(
                appliedPropsFile = appliedProp,
                repoPropsFile = repoProps
        )
        MatcherAssert.assertThat(info, CoreMatchers.`is`(
                GitClient.BuildRange(
                        repoSha = "fc9030edfe7e0a85a5545a342c7efbf93283f62f",
                        buildSha = "e31b2fa85ae9cb414023346684060d1009bf4c11"
                )
        ))
    }
}