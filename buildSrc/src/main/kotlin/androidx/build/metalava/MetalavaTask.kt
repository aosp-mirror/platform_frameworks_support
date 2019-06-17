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

package androidx.build.metalava

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles

/** Base class for invoking Metalava. */
abstract class MetalavaTask : DefaultTask() {
    /** Configuration containing Metalava and its dependencies. */
    @get:Classpath
    var configuration: Configuration? = null

    /** Android's boot classpath. Obtained from [BaseExtension.getBootClasspath]. */
    @get:InputFiles
    var bootClasspath: Collection<File> = emptyList()

    /** Dependencies of [sourcePaths]. */
    @get:InputFiles
    var dependencyClasspath: FileCollection? = null

    /** Source files against which API signatures will be validated. */
    @get:InputFiles
    var sourcePaths: Collection<File> = emptyList()

    protected fun runWithArgs(vararg args: String) {
        runWithArgs(args.asList())
    }

    protected fun runWithArgs(args: List<String>) {
        project.javaexec {
            it.classpath = checkNotNull(configuration) { "Configuration not set." }
            it.main = "com.android.tools.metalava.Driver"
            it.args = listOf(
                "--no-banner",
                "--error",
                "DeprecationMismatch", // Enforce deprecation mismatch
                "--hide",
                listOf(
                    // The list of checks that are hidden as they are not useful in androidx
                    "Enum", // Enums are allowed to be use in androidx
                    "CallbackInterface", // With target Java 8, we have default methods
                    "HiddenSuperclass", // We allow having a hidden parent class

                    // List of checks that have bugs, but should be enabled once fixed.
                    "GetterSetterNames", // b/135498039

                    // The list of checks that are API lint warnings and are yet to be enabled
                    "MinMaxConstant",
                    "IntentBuilderName",
                    "OnNameExpected",
                    "TopLevelBuilder",
                    "MissingBuild",
                    "BuilderSetStyle",
                    "SetterReturnsThis",
                    "PackageLayering",
                    "OverlappingConstants",
                    "IllegalStateException",
                    "ListenerLast",
                    "ExecutorRegistration",
                    "StreamFiles",
                    "ParcelableList",
                    "AbstractInner",
                    "NotCloseable",
                    "ArrayReturn",
                    "UserHandle",
                    "UserHandleName",
                    "MethodNameTense",
                    "UseIcu",
                    "NoByteOrShort",
                    "CommonArgsFirst",
                    "SamShouldBeLast",
                    "MissingJvmStatic"
                ).joinToString()
            ) + args
        }
    }
}
