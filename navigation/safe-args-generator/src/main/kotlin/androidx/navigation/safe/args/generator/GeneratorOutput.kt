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

package androidx.navigation.safe.args.generator

data class GeneratorOutput(val files: List<CodeFile>, val errors: List<ErrorMessage>) {
    val fileNames = files.map { it -> it.getFileName() }
}

data class ErrorMessage(val path: String, val line: Int, val column: Int, val message: String) {
    override fun toString() = "Error at $path:$line:$column $message"
}
