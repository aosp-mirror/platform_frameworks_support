/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.solver.types

import androidx.room.solver.CodeGenScope
import androidx.room.vo.CustomTypeConverter
import androidx.room.writer.ClassWriter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

/**
 * Wraps a type converter specified by the developer and forwards calls to it.
 */
class CustomTypeConverterWrapper(val custom: CustomTypeConverter)
    : TypeConverter(custom.from, custom.to) {

    override fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            if (custom.isStatic) {
                addStatement("%L = %T.%L(%L)",
                        outputVarName, custom.typeName,
                        custom.methodName, inputVarName)
            } else {
                addStatement("%L = %N.%L(%L)",
                        outputVarName, typeConverter(scope),
                        custom.methodName, inputVarName)
            }
        }
    }

    fun typeConverter(scope: CodeGenScope): PropertySpec {
        val baseName = (custom.typeName as ClassName).simpleName.decapitalize()
        return scope.writer.getOrCreateField(object : ClassWriter.SharedPropertySpec(
                baseName, custom.typeName) {
            override fun getUniqueKey(): String {
                return "converter_${custom.typeName}"
            }

            override fun prepare(writer: ClassWriter, builder: PropertySpec.Builder) {
                builder.addModifiers(KModifier.PRIVATE)
                builder.initializer("new %T()", custom.typeName)
            }
        })
    }
}
