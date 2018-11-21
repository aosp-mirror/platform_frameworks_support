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

package androidx.room.solver.query.result

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class PositionalDataSourceQueryResultBinder(
        val listAdapter: ListQueryResultAdapter?,
        val tableNames: Set<String>) : QueryResultBinder(listAdapter) {
    val itemTypeName: TypeName = listAdapter?.rowAdapter?.out?.typeName() ?: ANY
    val typeName: ParameterizedTypeName =
            RoomTypeNames.LIMIT_OFFSET_DATA_SOURCE.parameterizedBy(itemTypeName)
    override fun convertAndReturn(roomSQLiteQueryVar: String,
                                  canReleaseQuery: Boolean,
                                  dbField: PropertySpec,
                                  inTransaction: Boolean,
                                  scope: CodeGenScope) {
        // first comma for table names comes from the string since it might be empty in which case
        // we don't need a comma. If list is empty, this prevents generating bad code (it is still
        // an error to have empty list but that is already reported while item is processed)
        val tableNamesList = tableNames.joinToString("") { ", \"$it\"" }
        val spec = TypeSpec.anonymousClassBuilder().apply {
            superclass(typeName)
            addSuperclassConstructorParameter("%N, %L, %L %L",
                dbField, roomSQLiteQueryVar, inTransaction, tableNamesList)
            addFunction(createConvertRowsMethod(scope))
        }.build()
        scope.builder().apply {
            addStatement("return %L", spec)
        }
    }

    private fun createConvertRowsMethod(scope: CodeGenScope): FunSpec =
            FunSpec.builder("convertRows").apply {
                addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE)
                returns(CommonTypeNames.LIST.parameterizedBy(itemTypeName))
                val cursorParam = ParameterSpec.builder("cursor", AndroidTypeNames.CURSOR)
                        .build()
                addParameter(cursorParam)
                val resultVar = scope.getTmpVar("_res")
                val rowsScope = scope.fork()
                listAdapter?.convert(resultVar, cursorParam.name, rowsScope)
                addCode(rowsScope.builder().build())
                addStatement("return %L", resultVar)
            }.build()
}
