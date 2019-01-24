/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Task for a json file of all dependencies for each artifactId
 */
open class CombineDependencyListTask : DefaultTask() {

    init {
        group = "Help"
        description = "Creates a json file of the dependency graph in developer/dependencyGraph"
    }


    /**
     * Iterate through each configuration of the project and build the set of all dependencies.
     * Then add each dependency to the Artifact class as a project or prebuilt dependency.  Finally,
     * write these dependencies to a json file.
     */
    @TaskAction
    fun combineDependencyList() {
        println("WOOO WE\'RE COLLECTION DATA ")
        println("\n\n\n\n\n\n\n================================\n\n\n\n\n")

    }
}
