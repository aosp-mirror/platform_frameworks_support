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

package androidx.room.writer

import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.FtsEntity
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import stripNonJava
import java.util.HashMap
import java.util.HashSet

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter {
    override fun write(dbParam: ParameterSpec, scope: CodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize()
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder().apply {
            val columnListVar = scope.getTmpVar("_columns$suffix")
            val columnListType = ParameterizedTypeName.get(HashMap::class.typeName(),
                    CommonTypeNames.STRING, RoomTypeNames.TABLE_INFO_COLUMN)

            addStatement("final $T $L = new $T($L)", columnListType, columnListVar,
                    columnListType, entity.fields.size)

            // TODO: Need more real validation.
            // docid goes first
            // then cN<columnName>,  N start at 0
            // the first few cN are those defined by the user
            // the last few cN are the prefixes, when more than one is defined
            // last column in langid if enabled

            entity.fields.forEachIndexed { index, field ->
                addStatement("$L.put($S, new $T($S, $S, $L, $L))",
                        columnListVar, field.columnName, RoomTypeNames.TABLE_INFO_COLUMN,
                        /*name*/ field.columnName,
                        /*type*/ field.affinity?.name ?: SQLTypeAffinity.TEXT.name,
                        /*nonNull*/ field.nonNull,
                        /*pkeyPos*/ entity.primaryKey.fields.indexOf(field) + 1)
            }

            val foreignKeySetVar = scope.getTmpVar("_foreignKeys$suffix")
            val foreignKeySetType = ParameterizedTypeName.get(HashSet::class.typeName(),
                    RoomTypeNames.TABLE_INFO_FOREIGN_KEY)
            addStatement("final $T $L = new $T($L)", foreignKeySetType, foreignKeySetVar,
                    foreignKeySetType, entity.foreignKeys.size)
            val indicesSetVar = scope.getTmpVar("_indices$suffix")
            val indicesType = ParameterizedTypeName.get(HashSet::class.typeName(),
                    RoomTypeNames.TABLE_INFO_INDEX)
            addStatement("final $T $L = new $T($L)", indicesType, indicesSetVar,
                    indicesType, entity.indices.size)

            addStatement("final $T $L = new $T($S, $L, $L, $L)",
                    RoomTypeNames.TABLE_INFO, expectedInfoVar, RoomTypeNames.TABLE_INFO,
                    entity.tableName, columnListVar, foreignKeySetVar, indicesSetVar)

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement("final $T $L = $T.read($N, $S)",
                    RoomTypeNames.TABLE_INFO, existingVar, RoomTypeNames.TABLE_INFO,
                    dbParam, entity.shadowTableName)

            beginControlFlow("if (! $L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement("throw new $T($S + $L + $S + $L)",
                        IllegalStateException::class.typeName(),
                        "Migration didn't properly handle ${entity.tableName}" +
                                "(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar, "\n Found:\n", existingVar)
            }
            endControlFlow()
        }
    }

    override fun statementCount() = entity.fields.size
}