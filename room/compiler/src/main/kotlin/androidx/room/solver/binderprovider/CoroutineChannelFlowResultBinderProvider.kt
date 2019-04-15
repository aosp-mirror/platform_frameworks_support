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

package androidx.room.solver.binderprovider

import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.query.result.CoroutineChannelFlowResultBinder
import androidx.room.solver.query.result.QueryResultBinder
import com.squareup.javapoet.TypeName
import javax.lang.model.type.DeclaredType

sealed class CoroutineChannelFlowResultBinderProvider(
    val context: Context,
    private val channelFlowType: CoroutineChannelFlowResultBinder.Type
) : QueryResultBinderProvider {

    private val hasCoroutinesArtifact by lazy {
        context.processingEnv.elementUtils
            .getTypeElement(RoomCoroutinesTypeNames.COROUTINES_ROOM.toString()) != null
    }

    override fun provide(declared: DeclaredType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query)
        val tableNames = ((adapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }).toSet()
        if (tableNames.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        return CoroutineChannelFlowResultBinder(channelFlowType, typeArg, tableNames, adapter)
    }

    override fun matches(declared: DeclaredType): Boolean =
        declared.typeArguments.size == 1 &&
                matchesType(context.processingEnv.typeUtils.erasure(declared).typeName())

    protected open fun matchesType(typeName: TypeName): Boolean {
        val match = typeName == channelFlowType.className
        if (match && !hasCoroutinesArtifact) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_COROUTINE_ARTIFACT)
        }
        return match
    }
}

class CoroutineChannelResultBinderProvider(context: Context) :
    CoroutineChannelFlowResultBinderProvider(
        context, CoroutineChannelFlowResultBinder.Type.CHANNEL) {
    override fun matchesType(typeName: TypeName) = when (typeName) {
        KotlinTypeNames.CHANNEL, KotlinTypeNames.SEND_CHANNEL -> {
            context.logger.e(ProcessorErrors.invalidChannelType(typeName.toString()))
            false
        }
        else -> super.matchesType(typeName)
    }
}

class CoroutineFlowResultBinderProvider(context: Context) :
    CoroutineChannelFlowResultBinderProvider(context, CoroutineChannelFlowResultBinder.Type.FLOW)