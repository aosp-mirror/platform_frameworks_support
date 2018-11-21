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

import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.RoomTypeNames.INVALIDATION_OBSERVER
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Converts the query into a LiveData and returns it. No query is run until necessary.
 */
class LiveDataQueryResultBinder(val typeArg: TypeMirror, val tableNames: Set<String>,
                                adapter: QueryResultAdapter?)
    : BaseObservableQueryResultBinder(adapter) {
    @Suppress("JoinDeclarationAndAssignment")
    override fun convertAndReturn(
            roomSQLiteQueryVar: String,
            canReleaseQuery: Boolean,
            dbField: PropertySpec,
            inTransaction: Boolean,
            scope: CodeGenScope
    ) {
        val typeName = typeArg.typeName()

        val liveDataImpl =
                TypeSpec.anonymousClassBuilder().apply {
            superclass(LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.parameterizedBy(typeName))

            addSuperclassConstructorParameter( // This passes the Executor as a parameter to the superclass' constructor
                // while declaring an anonymous class.
                CodeBlock.builder().apply {
                    add("%N.getQueryExecutor()", dbField)
                }.build()
            )
            val observerField = PropertySpec.builder(scope.getTmpVar("_observer"),
                RoomTypeNames.INVALIDATION_OBSERVER, KModifier.PRIVATE).build()
            addProperty(observerField)
            addFunction(createComputeMethod(
                    observerField = observerField,
                    typeName = typeName,
                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                    dbField = dbField,
                    inTransaction = inTransaction,
                    scope = scope
            ))
            if (canReleaseQuery) {
                addFunction(createFinalizeMethod(roomSQLiteQueryVar))
            }
        }.build()
        scope.builder().apply {
            addStatement("return %L.getLiveData()", liveDataImpl)
        }
    }

    private fun createComputeMethod(
        roomSQLiteQueryVar: String,
        typeName: TypeName,
        observerField: PropertySpec,
        dbField: PropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ): FunSpec {
        return FunSpec.builder("compute").apply {
            addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE)
            returns(typeName)

            beginControlFlow("if (%N == null)", observerField).apply {
                addStatement("%N = %L", observerField, createAnonymousObserver())
                addStatement("%N.getInvalidationTracker().addWeakObserver(%N)",
                        dbField, observerField)
            }
            endControlFlow()

            createRunQueryAndReturnStatements(builder = this,
                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                    dbField = dbField,
                    inTransaction = inTransaction,
                    scope = scope)
        }.build()
    }

    private fun createAnonymousObserver(): TypeSpec {
        val tableNamesList = tableNames.joinToString(",") { "\"$it\"" }
        return TypeSpec.anonymousClassBuilder().apply {
            superclass(INVALIDATION_OBSERVER)
            addSuperclassConstructorParameter(tableNamesList)
            addFunction(FunSpec.builder("onInvalidated").apply {
                addModifiers(KModifier.OVERRIDE)
                addParameter(
                    ParameterSpec.builder( "tables",
                        Set::class.java.parameterizedBy(String::class.java)).build())
                addStatement("invalidate()")
            }.build())
        }.build()
    }
}
