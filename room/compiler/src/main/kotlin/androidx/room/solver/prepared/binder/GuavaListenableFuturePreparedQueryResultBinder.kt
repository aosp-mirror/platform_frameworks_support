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

package androidx.room.solver.prepared.binder

import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import javax.lang.model.type.TypeMirror

/**
 * Binds a prepared query of a method that returns a ListenableFuture.
 */
class GuavaListenableFuturePreparedQueryResultBinder(
    val returnType: TypeMirror,
    adapter: PreparedQueryResultAdapter?
) : PreparedQueryResultBinder(adapter) {

    override fun executeAndReturn(
        stmtQueryVal: String,
        preparedStmtField: String?,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val callableImpl = CallableTypeSpecBuilder(returnType.typeName()) {
            val binderScope = scope.fork()
            adapter?.executeAndReturn(
                stmtQueryVal,
                preparedStmtField,
                dbField,
                binderScope)
            addCode(binderScope.generate())
        }.build()
        scope.builder().apply {
            addStatement(
                "return $T.createListenableFuture($N, $L)",
                RoomGuavaTypeNames.GUAVA_ROOM,
                DaoWriter.dbField,
                callableImpl)
        }
    }
}