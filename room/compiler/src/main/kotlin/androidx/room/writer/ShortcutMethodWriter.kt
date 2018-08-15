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
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.InsertionMethod
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec

/**
 * Class that knows how to write a shortcut method (insert/update/delete) body.
 */
class ShortcutMethodWriter {

    fun createInsertionMethodBody(
        insertionType: InsertionMethod.Type,
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ): CodeBlock {

        return scope.builder().apply {
            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", DaoWriter.dbField)
            val needsReturnType = insertionType != InsertionMethod.Type.INSERT_VOID
            val resultVar = if (needsReturnType) {
                scope.getTmpVar("_result")
            } else {
                null
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val insertionAdapter = insertionAdapters[param.name]?.first
                    if (needsReturnType) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addStatement("$T $L = $N.$L($L)",
                                insertionType.returnTypeName, resultVar,
                                insertionAdapter, insertionType.methodName,
                                param.name)
                    } else {
                        addStatement("$N.$L($L)", insertionAdapter, insertionType.methodName,
                                param.name)
                    }
                }
                addStatement("$N.setTransactionSuccessful()", DaoWriter.dbField)
                if (needsReturnType) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", DaoWriter.dbField)
            }
            endControlFlow()
        }.build()
    }

    fun createDeleteOrUpdateMethodBody(
        returnCount: Boolean,
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ): CodeBlock {
        val resultVar = if (returnCount) {
            scope.getTmpVar("_total")
        } else {
            null
        }
        return scope.builder().apply {
            if (resultVar != null) {
                addStatement("$T $L = 0", TypeName.INT, resultVar)
            }
            addStatement("$N.beginTransaction()", DaoWriter.dbField)
            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val adapter = adapters[param.name]?.first
                    addStatement("$L$N.$L($L)",
                            if (resultVar == null) "" else "$resultVar +=",
                            adapter, param.handleMethodName(), param.name)
                }
                addStatement("$N.setTransactionSuccessful()", DaoWriter.dbField)
                if (resultVar != null) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", DaoWriter.dbField)
            }
            endControlFlow()
        }.build()
    }
}