/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.writer

import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Entity
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.columnNames
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

class EntityInsertionAdapterWriter(val entity: Entity, val onConflict: String) {
    fun createAnonymous(classWriter: ClassWriter, dbParam: String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder().apply {
            superclass(
                RoomTypeNames.INSERTION_ADAPTER.parameterizedBy(entity.typeName)
            )
            addSuperclassConstructorParameter("%L", dbParam)
            // If there is an auto-increment primary key with primitive type, we consider 0 as
            // not set. For such fields, we must generate a slightly different insertion SQL.
            val primitiveAutoGenerateField = if (entity.primaryKey.autoGenerateId) {
                entity.primaryKey.fields.firstOrNull()?.let { field ->
                    field.statementBinder?.typeMirror()?.let { binderType ->
                        if (binderType.kind.isPrimitive) {
                            field
                        } else {
                            null
                        }
                    }
                }
            } else {
                null
            }
            addFunction(FunSpec.builder("createQuery").apply {
                addModifiers(KModifier.OVERRIDE)
                returns(CommonTypeNames.STRING)
                val query =
                        "INSERT OR $onConflict INTO `${entity.tableName}`(" +
                                entity.columnNames.joinToString(",") { "`$it`" } +
                                ") VALUES (" +
                                entity.fields.joinToString(",") {
                                    if (primitiveAutoGenerateField == it) {
                                        "nullif(?, 0)"
                                    } else {
                                        "?"
                                    }
                                } + ")"
                addStatement("return %S", query)
            }.build())
            addFunction(FunSpec.builder("bind").apply {
                val bindScope = CodeGenScope(classWriter)
                addModifiers(KModifier.OVERRIDE)
                val stmtParam = "stmt"
                addParameter(
                    ParameterSpec.builder(
                        stmtParam, SupportDbTypeNames.SQLITE_STMT).build())
                val valueParam = "value"
                addParameter(ParameterSpec.builder(valueParam, entity.typeName).build())
                val mapped = FieldWithIndex.byOrder(entity.fields)
                FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mapped,
                        scope = bindScope
                )
                addCode(bindScope.builder().build())
            }.build())
        }.build()
    }
}
