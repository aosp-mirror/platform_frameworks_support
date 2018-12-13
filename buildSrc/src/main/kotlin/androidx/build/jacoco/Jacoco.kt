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

package androidx.build.jacoco

import androidx.build.getDistributionDirectory
import androidx.build.lazyDependsOn
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create

object Jacoco {
    private const val VERSION = "0.7.8"
    const val CORE_DEPENDENCY = "org.jacoco:org.jacoco.core:$VERSION"
    private const val ANT_DEPENDENCY = "org.jacoco:org.jacoco.ant:$VERSION"

    fun createUberJarTask(project: Project): Task {
        // This "uber" jacoco jar is used by the build server. Instrumentation tests are executed
        // outside of Gradle and this is needed to process the coverage files.

        val config = project.configurations.create("myJacoco")
        config.dependencies.add(project.dependencies.create(ANT_DEPENDENCY))

        val task = project.tasks.create<Jar>("jacocoAntUberJar")
        task.apply {
            inputs.files(config)
            from(config.resolvedConfiguration.resolvedArtifacts.map { project.zipTree(it.file) }) {
                it.exclude("META-INF/*.SF")
                it.exclude("META-INF/*.DSA")
                it.exclude("META-INF/*.RSA")
            }
            destinationDir = project.getDistributionDirectory()
            archiveName = "jacocoant.jar"
        }
        return task
    }

    @JvmStatic
    fun registerSubTask(project : Project, provider : TaskProvider<Task>) {
        getRootCoverageTask(project).lazyDependsOn(provider)
    }

    fun getRootCoverageTask(project : Project) : TaskProvider<Jar> {
        return project.rootProject.tasks.named("packageAllClassFilesForCoverageReport") as TaskProvider<Jar>
    }

    fun createCoverageJarTask(project: Project): TaskProvider<Jar> {
        // Package the individual *-allclasses.jar files together to generate code coverage reports
        val packageAllClassFiles = project.tasks.register("packageAllClassFilesForCoverageReport",
                Jar::class.java) { packageAllTask ->
            packageAllTask.destinationDir = project.getDistributionDirectory()
            packageAllTask.archiveName = "jacoco-report-classes-all.jar"
        }
        return packageAllClassFiles
    }
}
