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
import androidx.room.ext.typeName
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import stripNonJava
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

class TableInfoValidationWriter(val entity: Entity) : ValidationWriter() {
    override fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize()
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder().apply {
            val columnListVar = scope.getTmpVar("_columns$suffix")
            val columnListType = HashMap::class.typeName().parameterizedBy(CommonTypeNames.STRING,
                RoomTypeNames.TABLE_INFO_COLUMN)

            addStatement("val %L = %T(%L)", columnListVar,
                    columnListType, entity.fields.size)
            entity.fields.forEach { field ->
                addStatement("%L.put(%S, %T(%S, %S, %L, %L))",
                        columnListVar, field.columnName, RoomTypeNames.TABLE_INFO_COLUMN,
                        /*name*/ field.columnName,
                        /*type*/ field.affinity?.name ?: SQLTypeAffinity.TEXT.name,
                        /*nonNull*/ field.nonNull,
                        /*pkeyPos*/ entity.primaryKey.fields.indexOf(field) + 1)
            }

            val foreignKeySetVar = scope.getTmpVar("_foreignKeys$suffix")
            val foreignKeySetType = HashSet::class.typeName().parameterizedBy(
                    RoomTypeNames.TABLE_INFO_FOREIGN_KEY)
            addStatement("val %L = new %T(%L)", foreignKeySetVar,
                    foreignKeySetType, entity.foreignKeys.size)
            entity.foreignKeys.forEach {
                val myColumnNames = it.childFields
                        .joinToString(",") { "\"${it.columnName}\"" }
                val refColumnNames = it.parentColumns
                        .joinToString(",") { "\"$it\"" }
                addStatement("%L.add(%T(%S, %S, %S," +
                        "%T.asList(%L), %T.asList(%L)))", foreignKeySetVar,
                        RoomTypeNames.TABLE_INFO_FOREIGN_KEY,
                        /*parent table*/ it.parentTable,
                        /*on delete*/ it.onDelete.sqlName,
                        /*on update*/ it.onUpdate.sqlName,
                        Arrays::class.typeName(),
                        /*parent names*/ myColumnNames,
                        Arrays::class.typeName(),
                        /*parent column names*/ refColumnNames)
            }

            val indicesSetVar = scope.getTmpVar("_indices$suffix")
            val indicesType = HashSet::class.typeName().parameterizedBy(
                    RoomTypeNames.TABLE_INFO_INDEX)
            addStatement("val %L = %T(%L)", indicesSetVar, indicesType, entity.indices.size)
            entity.indices.forEach { index ->
                val columnNames = index.columnNames.joinToString(",") { "\"$it\"" }
                addStatement("%L.add(%T(%S, %L, %T.asList(%L)))",
                        indicesSetVar,
                        RoomTypeNames.TABLE_INFO_INDEX,
                        index.name,
                        index.unique,
                        Arrays::class.typeName(),
                        columnNames)
            }

            addStatement("val %L = %T(%S, %L, %L, %L)",
                    expectedInfoVar, RoomTypeNames.TABLE_INFO,
                    entity.tableName, columnListVar, foreignKeySetVar, indicesSetVar)

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement("val %L = %T.read(%N, %S)",
                    existingVar, RoomTypeNames.TABLE_INFO, dbParam, entity.tableName)

            beginControlFlow("if (! %L.equals(%L))", expectedInfoVar, existingVar).apply {
                addStatement("throw %T(%S + %L + %S + %L)",
                        IllegalStateException::class.typeName(),
                        "Migration didn't properly handle ${entity.tableName}" +
                                "(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar, "\n Found:\n", existingVar)
            }
            endControlFlow()
        }
    }
}
