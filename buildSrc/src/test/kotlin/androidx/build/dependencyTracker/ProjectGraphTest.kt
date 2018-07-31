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

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.gradle.api.Project
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import java.io.File

@RunWith(JUnit4::class)
class ProjectGraphTest {
    @Test
    fun testSimple() {
        val tmpDir = File("/tmp")
        val root = createProject(
                tmpDir,
                "root",
                ":root"
        )
        val p1 = createProject(
                projectDir = tmpDir.resolve("p1"),
                name = "p1",
                path = ":p1"
        )
        val p2 = createProject(
                projectDir = tmpDir.resolve("p2"),
                name = "p2",
                path = ":p2"
        )
        val p1p3 = createProject(
                projectDir = tmpDir.resolve("p1/p3"),
                name = "p1_p3",
                path = ":p1:p3"
        )
        Mockito.`when`(root.subprojects)
                .thenReturn(setOf(p1, p2, p1p3))
        val graph = ProjectGraph(root)
        assertNull(graph.findContainingProject("nowhere"))
        assertNull(graph.findContainingProject("rootfile.java"))
        assertEquals(
                p1,
                graph.findContainingProject("p1/px/x.java".toLocalPath()))
        assertEquals(
                p1,
                graph.findContainingProject("p1/a.java".toLocalPath()))
        assertEquals(
                p1p3,
                graph.findContainingProject("p1/p3/a.java".toLocalPath()))
        assertEquals(
                p2,
                graph.findContainingProject("p2/a/b/c/d/e/f/a.java".toLocalPath()))
    }

    private fun String.toLocalPath() =
        this.split("/").joinToString(File.separator)

    companion object {
        fun createProject(
            projectDir: File,
            name: String,
            path: String
        ): Project {
            val project = Mockito.mock(Project::class.java)
            Mockito.`when`(project.name).thenReturn(name)
            Mockito.`when`(project.path).thenReturn(path)
            Mockito.`when`(project.projectDir).thenReturn(projectDir)
            return project
        }
    }
}