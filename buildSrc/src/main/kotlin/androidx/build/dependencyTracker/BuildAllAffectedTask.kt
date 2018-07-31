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

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * A task that runs all tests for all affected modules
 */
open class BuildAllAffectedTask : DefaultTask() {
    @Input
    lateinit var projectPaths : List<String>
    @TaskAction
    fun run() {
        println("completed running all affected stuff")
    }
    class ConfigAction() : Action<BuildAllAffectedTask> {
        override fun execute(task: BuildAllAffectedTask) {
//            val result = AffectedModuleDetector.get().affectedProjects
//            task.projectPaths = result.projects.map {
//                it.path
//            }
        }
    }

    companion object {
        @JvmStatic
        fun create(rootProject : Project) : Task {
            return rootProject.tasks.create("testAllAffected", BuildAllAffectedTask::class.java,
                    ConfigAction())
        }
    }
}