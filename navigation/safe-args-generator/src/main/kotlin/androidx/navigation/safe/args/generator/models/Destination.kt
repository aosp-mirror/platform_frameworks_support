/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.models

import androidx.navigation.safe.args.generator.ext.toCamelCase
import com.squareup.javapoet.ClassName

data class Destination(
    val id: ResReference?,
    val name: ClassName?,
    val type: String,
    val args: List<Argument>,
    val actions: List<Action>,
    val nested: List<Destination> = emptyList(),
    val skipFileGen: Boolean = false
) {

    companion object {
        fun createName(id: ResReference?, name: String, applicationId: String): ClassName? = when {
            name.isNotEmpty() -> {
                val specifiedPackage = name.substringBeforeLast('.', "")
                val classPackage = if (name.startsWith(".")) {
                    "$applicationId$specifiedPackage"
                } else {
                    specifiedPackage
                }
                ClassName.get(classPackage, name.substringAfterLast('.'))
            }
            id != null -> ClassName.get(id.packageName, id.javaIdentifier.toCamelCase())
            else -> null
        }
    }
}