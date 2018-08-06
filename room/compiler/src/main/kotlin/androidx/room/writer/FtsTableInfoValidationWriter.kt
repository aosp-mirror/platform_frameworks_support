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

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.FtsEntity
import com.squareup.javapoet.ParameterSpec
import stripNonJava

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter {
    override fun write(dbParam: ParameterSpec, scope: CodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize()
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder().apply {
            addStatement("final $T $L = new $T($S, $L)",
                    RoomTypeNames.TABLE_CREATE_INFO, expectedInfoVar,
                    RoomTypeNames.TABLE_CREATE_INFO, entity.tableName,
                    "\"${entity.createTableQuery}\"")

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement("final $T $L = $T.read($N, $S)",
                    RoomTypeNames.TABLE_CREATE_INFO, existingVar, RoomTypeNames.TABLE_CREATE_INFO,
                    dbParam, entity.tableName)

            beginControlFlow("if (!$L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement("throw new $T($S + $L + $S + $L)",
                        IllegalStateException::class.typeName(),
                        "Migration didn't properly handle ${entity.tableName}" +
                                "(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar, "\n Found:\n", existingVar)
            }
            endControlFlow()
        }
    }

    override fun statementCount() = 4
}