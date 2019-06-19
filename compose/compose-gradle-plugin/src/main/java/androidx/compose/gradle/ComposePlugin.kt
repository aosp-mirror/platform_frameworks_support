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

package androidx.compose.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Create a configuration that is used to mark a project using the compose plugin.
 */
class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all {
            when (it) {
                is KotlinBasePluginWrapper -> {
                    // Create a configuration to reference the compose plugin
                    val conf = project.configurations.create("composePlugin")

                    // Ensure the configuration list only contains the compose plugin and none of
                    // its dependencies as they should already be present when the plugin is loaded
                    conf.isTransitive = false

                    // Configure all the Kotlin compiler tasks when a compose plugin was enabled.
                    project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
                        compile.dependsOn(conf)
                        compile.doFirst {
                            if (!conf.isEmpty) {
                                // Add the compose plugin as a kotlin compiler plugin
                                compile.kotlinOptions.freeCompilerArgs +=
                                    "-Xplugin=${conf.files.first()}"

                                // Ensure that we are using the IR back-end as is required by
                                // the compose plugin.
                                compile.kotlinOptions.useIR = true

                                // Compose is built with a "1.8" JVM target which then requires
                                // for the build target as well if inline methods are used.
                                compile.kotlinOptions.jvmTarget = "1.8"
                            }
                        }
                    }
                }
            }
        }
    }
}