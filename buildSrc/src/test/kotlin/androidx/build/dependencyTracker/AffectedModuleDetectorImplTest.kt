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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class AffectedModuleDetectorImplTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private lateinit var root: Project
    private lateinit var p1: Project
    private lateinit var p2: Project
    private lateinit var p3: Project
    private lateinit var p4: Project
    private lateinit var p5: Project
    private lateinit var p6: Project
    private lateinit var p7: Project

    @Before
    fun init() {
        val tmpDir = tmpFolder.root

        /*

        Dummy project tree:

               root
              / |  \
            p1  p7  p2
           /         \
          p3          p5
         /  \
       p4   p6


         */

        root = ProjectBuilder.builder()
                .withProjectDir(tmpDir)
                .withName("root")
                .build()
        p1 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p1"))
                .withName("p1")
                .withParent(root)
                .build()
        p2 = ProjectBuilder.builder()
                .withProjectDir(tmpDir.resolve("p2"))
                .withName("p2")
                .withParent(root)
                .build()
        p3 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3"))
            .withName("p3")
            .withParent(p1)
            .build()
        p4 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3:p4"))
            .withName("p4")
            .withParent(p3)
            .build()
        p5 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p2:p5"))
            .withName("p5")
            .withParent(p2)
            .build()
        p6 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3:p6"))
            .withName("p6")
            .withParent(p3)
            .build()
        p7 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p7"))
            .withName("p7")
            .withParent(root)
            .build()
    }

    @Test
    fun noChangeCLs() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                onlyDependant = false,
                onlyDirectlyAffected = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = emptyList())
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    @Test
    fun noChangeCLsOnlyDependant() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                onlyDependant = true,
                onlyDirectlyAffected = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = emptyList())
                )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    @Test
    fun noChangeCLsOnlyDirectlyAffected() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = true,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = emptyList())
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    @Test
    fun changeInOne() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = listOf(convertToFilePath("p1", "foo.java")))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p3, p4, p6)
        ))
    }

//    @Test
//    fun changeInOneOnlyDependant() {
//        val detector = AffectedModuleDetectorImpl(
//            rootProject = root,
//            logger = logger,
//            ignoreUnknownProjects = false,
//            onlyDependant = true,
//            onlyDirectlyAffected = false,
//            injectedGitClient = MockGitClient(
//                lastMergeSha = "foo",
//                changedFiles = listOf(convertToFilePath("p1", "foo.java")))
//        )
//        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
//            setOf(p3, p4, p6)
//        ))
//    }

    @Test
    fun changeInOneOnlyDirectlyAffected() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = true,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p1)
        ))
    }

//    @Test
//    fun changeInTwo() {
//        val detector = AffectedModuleDetectorImpl(
//                rootProject = root,
//                logger = logger,
//                ignoreUnknownProjects = false,
//                onlyDependant = false,
//                onlyDirectlyAffected = false,
//                injectedGitClient = MockGitClient(
//                        lastMergeSha = "foo",
//                        changedFiles = listOf(
//                                convertToFilePath("p1", "foo.java"),
//                                convertToFilePath("p2", "bar.java")))
//        )
//        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
//                setOf(p1, p2, p3, p4, p5, p6)
//        ))
//    }

//    @Test
//    fun changeInTwoOnlyDependant() {
//        val detector = AffectedModuleDetectorImpl(
//            rootProject = root,
//            logger = logger,
//            ignoreUnknownProjects = false,
//            onlyDependant = true,
//            onlyDirectlyAffected = false,
//            injectedGitClient = MockGitClient(
//                lastMergeSha = "foo",
//                changedFiles = listOf(
//                    convertToFilePath("p1", "foo.java"),
//                    convertToFilePath("p2", "bar.java")))
//        )
//        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
//            setOf(p3, p4, p5, p6)
//        ))
//    }

    @Test
    fun changeInTwoOnlyDirectlyAffected() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = true,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p1", "foo.java"),
                    convertToFilePath("p2", "bar.java")))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p1, p2)
        ))
    }

    @Test
    fun changeInRoot() {
        val detector = AffectedModuleDetectorImpl(
                rootProject = root,
                logger = logger,
                ignoreUnknownProjects = false,
                onlyDependant = false,
                onlyDirectlyAffected = false,
                injectedGitClient = MockGitClient(
                        lastMergeSha = "foo",
                        changedFiles = listOf("foo.java"))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    @Test
    fun changeInRootOnlyDependant() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = true,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf("foo.java"))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    @Test
    fun changeInRootOnlyDirectlyAffected() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            onlyDependant = false,
            onlyDirectlyAffected = true,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf("foo.java"))
        )
        MatcherAssert.assertThat(detector.affectedProjects, CoreMatchers.`is`(
            setOf(p1, p2, p3, p4, p5, p6, p7)
        ))
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockGitClient(
        val lastMergeSha: String?,
        val changedFiles: List<String>
    ) : GitClient {
        override fun findChangedFilesSince(
            sha: String,
            top: String,
            includeUncommitted: Boolean
        ) = changedFiles

        override fun findPreviousMergeCL() = lastMergeSha
    }
}
