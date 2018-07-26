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
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec

/**
 * Instantly runs and returns the query.
 */
class InstantQueryResultBinder(adapter: QueryResultAdapter?) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val transactionWrapper = if (inTransaction) {
            scope.builder().transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        scope.builder().apply {
            val shouldCopyCursor = adapter?.shouldCopyCursor() == true
            val outVar = scope.getTmpVar("_result")
            val cursorVar = scope.getTmpVar("_cursor")
            val cursorCopyVar = scope.getTmpVar("_cursorCopy")
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
                if (canReleaseQuery) {
                    addStatement("$L.release()", roomSQLiteQueryVar)
                }
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

    private fun CodeBlock.Builder.writeConvert(
        outVar: String,
        cursorVar: String,
        transactionWrapper: TransactionWrapper?,
        scope: CodeGenScope
    ) {
        adapter?.convert(outVar, cursorVar, scope)
        transactionWrapper?.commitTransaction()
        addStatement("return $L", outVar)
    }
}
