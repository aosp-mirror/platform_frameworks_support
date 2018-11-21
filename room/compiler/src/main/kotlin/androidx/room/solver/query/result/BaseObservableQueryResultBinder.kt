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
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.writer.DaoWriter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

/**
 * Base class for query result binders that observe the database. It includes common functionality
 * like creating a finalizer to release the query or creating the actual adapter call code.
 */
abstract class BaseObservableQueryResultBinder(adapter: QueryResultAdapter?)
    : QueryResultBinder(adapter) {

    protected fun createFinalizeMethod(roomSQLiteQueryVar: String): FunSpec {
        return FunSpec.builder("finalize").apply {
            addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE)
            addStatement("%L.release()", roomSQLiteQueryVar)
        }.build()
    }

    protected fun createRunQueryAndReturnStatements(
        builder: FunSpec.Builder,
        roomSQLiteQueryVar: String,
        dbField: PropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            addStatement("final %T %L = %T.query(%N, %L, %L)",
                    AndroidTypeNames.CURSOR,
                    cursorVar,
                    RoomTypeNames.DB_UTIL,
                    DaoWriter.dbField,
                    roomSQLiteQueryVar,
                    if (shouldCopyCursor) "true" else "false")
            beginControlFlow("try").apply {
                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.builder().build())
                transactionWrapper?.commitTransaction()
                addStatement("return %L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("%L.close()", cursorVar)
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}
