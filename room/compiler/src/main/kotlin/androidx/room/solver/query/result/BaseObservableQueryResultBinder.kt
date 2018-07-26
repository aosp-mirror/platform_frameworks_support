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
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

/**
 * Base class for query result binders that observe the database. It includes common functionality
 * like creating a finalizer to release the query or creating the actual adapter call code.
 */
abstract class BaseObservableQueryResultBinder(adapter: QueryResultAdapter?)
    : QueryResultBinder(adapter) {

    protected fun createFinalizeMethod(roomSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            addStatement("$L.release()", roomSQLiteQueryVar)
        }.build()
    }

    protected fun createRunQueryAndReturnStatements(
        builder: MethodSpec.Builder,
        roomSQLiteQueryVar: String,
        dbField: FieldSpec,
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
        val cursorCopyVar = scope.getTmpVar("_cursorCopy")
        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            if (shouldCopyCursor) {
                addStatement("final $T $L", AndroidTypeNames.CURSOR, cursorCopyVar)
            }
            addStatement("final $T $L = $N.query($L)", AndroidTypeNames.CURSOR, cursorVar,
                    DaoWriter.dbField, roomSQLiteQueryVar)
            beginControlFlow("try").apply {
                if (shouldCopyCursor) {
                    addStatement("$L = $T.copy($L)",
                            cursorCopyVar, RoomTypeNames.CURSOR_UTIL, cursorVar)
                } else {
                    writeConvert(outVar, cursorVar, transactionWrapper, scope)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
            }
            endControlFlow()
            if (shouldCopyCursor) {
                beginControlFlow("try").apply {
                    writeConvert(outVar, cursorCopyVar, transactionWrapper, scope)
                }
                nextControlFlow("finally").apply {
                    addStatement("$L.close()", cursorCopyVar)
                }
                endControlFlow()
            }
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }

    private fun MethodSpec.Builder.writeConvert(
        outVar: String,
        cursorVar: String,
        transactionWrapper: TransactionWrapper?,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        adapter?.convert(outVar, cursorVar, adapterScope)
        addCode(adapterScope.builder().build())
        transactionWrapper?.commitTransaction()
        addStatement("return $L", outVar)
    }
}
