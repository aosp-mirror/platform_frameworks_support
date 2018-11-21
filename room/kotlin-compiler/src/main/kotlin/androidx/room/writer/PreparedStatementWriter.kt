/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Creates anonymous classes for RoomTypeNames#SHARED_SQLITE_STMT.
 */
class PreparedStatementWriter(val queryWriter: QueryWriter) {
    fun createAnonymous(classWriter: ClassWriter, dbParam: PropertySpec): TypeSpec {
        val scope = CodeGenScope(classWriter)
        @Suppress("RemoveSingleExpressionStringTemplate")
//        return TypeSpec.objectBuilder("%N", dbParam).apply {
        // TODO : not sure
        return TypeSpec.anonymousClassBuilder().apply {
            superclass(RoomTypeNames.SHARED_SQLITE_STMT)
            addFunction(FunSpec.builder("createQuery").apply {
                addModifiers(KModifier.OVERRIDE)
                returns(CommonTypeNames.STRING)
                val queryName = scope.getTmpVar("_query")
                val queryGenScope = scope.fork()
                queryWriter.prepareQuery(queryName, queryGenScope)
                addCode(queryGenScope.builder().build())
                addStatement("return %L", queryName)
            }.build())
        }.build()
    }
}

class C {

}