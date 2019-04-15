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

package androidx.room.solver.query.result

import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.T
import androidx.room.ext.arrayTypeName
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec
import javax.lang.model.type.TypeMirror

class CoroutineChannelResultBinder(
    typeArg: TypeMirror,
    private val tableNames: Set<String>,
    private val continuationParamName: String,
    adapter: QueryResultAdapter?
) : CoroutineResultBinder(typeArg, continuationParamName, adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName()) {
            createRunQueryAndReturnStatements(
                builder = this,
                roomSQLiteQueryVar = roomSQLiteQueryVar,
                canReleaseQuery = canReleaseQuery,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope)
        }.build()

        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(",") { "\"$it\"" }
            addStatement(
                "return $T.createChannel($N, $L, new $T{$L}, $L, $N)",
                RoomCoroutinesTypeNames.COROUTINES_ROOM,
                dbField,
                if (inTransaction) "true" else "false",
                String::class.arrayTypeName(),
                tableNamesList,
                callableImpl,
                continuationParamName)
        }
    }
}