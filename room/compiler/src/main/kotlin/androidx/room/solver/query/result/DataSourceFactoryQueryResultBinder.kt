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

package androidx.room.solver.query.result

import androidx.room.ext.PagingTypeNames
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class DataSourceFactoryQueryResultBinder(
        val positionalDataSourceQueryResultBinder: PositionalDataSourceQueryResultBinder)
    : QueryResultBinder(positionalDataSourceQueryResultBinder.listAdapter) {
    @Suppress("HasPlatformType")
    val typeName = positionalDataSourceQueryResultBinder.itemTypeName
    override fun convertAndReturn(
            roomSQLiteQueryVar: String,
            canReleaseQuery: Boolean,
            dbField: PropertySpec,
            inTransaction: Boolean,
            scope: CodeGenScope
    ) {
        scope.builder().apply {
            val pagedListProvider = TypeSpec
                    .anonymousClassBuilder().apply {
                superclass(
                    PagingTypeNames.DATA_SOURCE_FACTORY.parameterizedBy(INT, typeName))
                addFunction(createCreateMethod(
                        roomSQLiteQueryVar = roomSQLiteQueryVar,
                        dbField = dbField,
                        inTransaction = inTransaction,
                        scope = scope))
            }.build()
            addStatement("return %L", pagedListProvider)
        }
    }

    private fun createCreateMethod(
            roomSQLiteQueryVar: String,
            dbField: PropertySpec,
            inTransaction: Boolean,
            scope: CodeGenScope
    ): FunSpec = FunSpec.builder("create").apply {
        addModifiers(KModifier.OVERRIDE)
        returns(positionalDataSourceQueryResultBinder.typeName)
        val countedBinderScope = scope.fork()
        positionalDataSourceQueryResultBinder.convertAndReturn(
                roomSQLiteQueryVar = roomSQLiteQueryVar,
                canReleaseQuery = true,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = countedBinderScope)
        addCode(countedBinderScope.builder().build())
    }.build()
}
