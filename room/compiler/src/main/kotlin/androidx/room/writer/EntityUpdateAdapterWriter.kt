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

class EntityUpdateAdapterWriter(val entity: Entity, val onConflict: String) {
    fun createAnonymous(classWriter: ClassWriter, dbParam: String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        //TODO: shit
        return TypeSpec.anonymousClassBuilder().apply {
            superclass(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER.parameterizedBy(entity.typeName))
            addSuperclassConstructorParameter("%L", dbParam)
            addFunction(FunSpec.builder("createQuery").apply {
                addModifiers(KModifier.OVERRIDE)
                returns(CommonTypeNames.STRING)
                val query = "UPDATE OR $onConflict `${entity.tableName}` SET " +
                        entity.columnNames.joinToString(",") { "`$it` = ?" } + " WHERE " +
                        entity.primaryKey.columnNames.joinToString(" AND ") { "`$it` = ?" }
                addStatement("return %S", query)
            }.build())
            addFunction(FunSpec.builder("bind").apply {
                val bindScope = CodeGenScope(classWriter)
                addModifiers(KModifier.OVERRIDE)
                val stmtParam = "stmt"
                addParameter(ParameterSpec.builder(
                    stmtParam, SupportDbTypeNames.SQLITE_STMT).build())
                val valueParam = "value"
                addParameter(ParameterSpec.builder(valueParam, entity.typeName).build())
                val mappedField = FieldWithIndex.byOrder(entity.fields)
                FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedField,
                        scope = bindScope
                )
                val pkeyStart = entity.fields.size
                val mappedPrimaryKeys = entity.primaryKey.fields.mapIndexed { index, field ->
                    FieldWithIndex(field = field,
                            indexVar = "${pkeyStart + index + 1}",
                            alwaysExists = true)
                }
                FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedPrimaryKeys,
                        scope = bindScope
                )
                addCode(bindScope.builder().build())
            }.build())
        }.build()
    }
}
