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

package androidx.room.processor

import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.T
import androidx.room.ext.extendsBoundOrSelf
import androidx.room.ext.getSuspendFunctionReturnType
import androidx.room.kotlin.KotlinMetadataElement
import androidx.room.parser.ParsedQuery
import androidx.room.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder
import androidx.room.solver.query.result.CoroutineChannelResultBinder
import androidx.room.solver.query.result.CoroutineFlowResultBinder
import androidx.room.solver.query.result.CoroutineResultBinder
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.shortcut.binder.CallableDeleteOrUpdateMethodBinder.Companion.createDeleteOrUpdateBinder
import androidx.room.solver.shortcut.binder.CallableInsertMethodBinder.Companion.createInsertBinder
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.solver.transaction.binder.InstantTransactionMethodBinder
import androidx.room.solver.transaction.binder.CoroutineTransactionMethodBinder
import androidx.room.solver.transaction.binder.TransactionMethodBinder
import androidx.room.solver.transaction.result.TransactionMethodAdapter
import androidx.room.vo.QueryParameter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.TransactionMethod
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 *  Delegate class with common functionality for DAO method processors.
 */
abstract class MethodProcessorDelegate(
    val context: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement,
    protected val classMetadata: KotlinMetadataElement?
) {

    abstract fun extractReturnType(): TypeMirror

    abstract fun extractParams(): List<VariableElement>

    fun extractQueryParams(): List<QueryParameter> {
        val kotlinParameterNames = classMetadata?.getParameterNames(executableElement)
        return extractParams().mapIndexed { index, variableElement ->
            QueryParameterProcessor(
                baseContext = context,
                containing = containing,
                element = variableElement,
                sqlName = kotlinParameterNames?.getOrNull(index)
            ).process()
        }
    }

    abstract fun findResultBinder(returnType: TypeMirror, query: ParsedQuery): QueryResultBinder

    abstract fun findPreparedResultBinder(
        returnType: TypeMirror,
        query: ParsedQuery
    ): PreparedQueryResultBinder

    abstract fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder

    abstract fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror): DeleteOrUpdateMethodBinder

    abstract fun findTransactionMethodBinder(
        callType: TransactionMethod.CallType
    ): TransactionMethodBinder

    companion object {
        fun createFor(
            context: Context,
            containing: DeclaredType,
            executableElement: ExecutableElement
        ): MethodProcessorDelegate {
            val kotlinMetadata =
                KotlinMetadataElement.createFor(context, executableElement.enclosingElement)
            return if (kotlinMetadata?.isSuspendFunction(executableElement) == true) {
                val hasCoroutineArtifact = context.processingEnv.elementUtils
                    .getTypeElement(RoomCoroutinesTypeNames.COROUTINES_ROOM.toString()) != null
                if (!hasCoroutineArtifact) {
                    context.logger.e(ProcessorErrors.MISSING_ROOM_COROUTINE_ARTIFACT)
                }
                SuspendMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    kotlinMetadata
                )
            } else {
                DefaultMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    kotlinMetadata
                )
            }
        }
    }
}

/**
 * Default delegate for DAO methods.
 */
class DefaultMethodProcessorDelegate(
    context: Context,
    containing: DeclaredType,
    executableElement: ExecutableElement,
    classMetadata: KotlinMetadataElement?
) : MethodProcessorDelegate(context, containing, executableElement, classMetadata) {

    override fun extractReturnType(): TypeMirror {
        val asMember = context.processingEnv.typeUtils.asMemberOf(containing, executableElement)
        return MoreTypes.asExecutable(asMember).returnType
    }

    override fun extractParams() = executableElement.parameters

    override fun findResultBinder(returnType: TypeMirror, query: ParsedQuery) =
        context.typeAdapterStore.findQueryResultBinder(returnType, query)

    override fun findPreparedResultBinder(
        returnType: TypeMirror,
        query: ParsedQuery
    ) = context.typeAdapterStore.findPreparedQueryResultBinder(returnType, query)

    override fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = context.typeAdapterStore.findInsertMethodBinder(returnType, params)

    override fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        context.typeAdapterStore.findDeleteOrUpdateMethodBinder(returnType)

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        InstantTransactionMethodBinder(
            TransactionMethodAdapter(executableElement.simpleName.toString(), callType))
}

/**
 * Delegate for DAO methods that are a suspend function.
 */
class SuspendMethodProcessorDelegate(
    context: Context,
    containing: DeclaredType,
    executableElement: ExecutableElement,
    kotlinMetadata: KotlinMetadataElement
) : MethodProcessorDelegate(context, containing, executableElement, kotlinMetadata) {

    private val continuationParam: VariableElement by lazy {
        val typesUtil = context.processingEnv.typeUtils
        val continuationType = typesUtil.erasure(
            context.processingEnv.elementUtils
                .getTypeElement(KotlinTypeNames.CONTINUATION.toString())
                .asType()
        )
        executableElement.parameters.last {
            typesUtil.isSameType(typesUtil.erasure(it.asType()), continuationType)
        }
    }

    private val channelTypes: Array<TypeMirror> by lazy {
        val elementUtils = context.processingEnv.elementUtils
        arrayOf(
            elementUtils.getTypeElement(KotlinTypeNames.CHANNEL.toString()).asType(),
            elementUtils.getTypeElement(KotlinTypeNames.RECEIVE_CHANNEL.toString()).asType())
    }

    private val flowType: TypeMirror by lazy {
        context.processingEnv.elementUtils.getTypeElement(KotlinTypeNames.FLOW.toString()).asType()
    }

    override fun extractReturnType() = executableElement.getSuspendFunctionReturnType()

    override fun extractParams() =
        executableElement.parameters.filterNot { it == continuationParam }

    override fun findResultBinder(returnType: TypeMirror, query: ParsedQuery): QueryResultBinder {
        val typeUtils = context.processingEnv.typeUtils
        val declared = MoreTypes.asDeclared(returnType)
        val erasure = typeUtils.erasure(declared)

        return if (channelTypes.any { typeUtils.isAssignable(erasure, it) }) {
            // TODO: Complain that type must be ReceiveChannel and not Channel (nor SendChannel)
            val extractedReturnType = declared.typeArguments.first().extendsBoundOrSelf()
            val adapter =
                context.typeAdapterStore.findQueryResultAdapter(extractedReturnType, query)
            val tableNames = ((adapter?.accessedTableNames() ?: emptyList()) +
                    query.tables.map { it.name }).toSet()
            if (tableNames.isEmpty()) {
                context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
            }
            CoroutineChannelResultBinder(
                typeArg = extractedReturnType,
                tableNames = tableNames,
                adapter = adapter,
                continuationParamName = continuationParam.simpleName.toString()
            )
        } else if (typeUtils.isAssignable(erasure, flowType)) {
            val extractedReturnType = declared.typeArguments.first().extendsBoundOrSelf()
            val adapter =
                context.typeAdapterStore.findQueryResultAdapter(extractedReturnType, query)
            CoroutineFlowResultBinder(
                typeArg = extractedReturnType,
                adapter = adapter,
                continuationParamName = continuationParam.simpleName.toString()
            )
        } else {
            CoroutineResultBinder(
                typeArg = returnType,
                adapter = context.typeAdapterStore.findQueryResultAdapter(returnType, query),
                continuationParamName = continuationParam.simpleName.toString()
            )
        }
    }

    override fun findPreparedResultBinder(
        returnType: TypeMirror,
        query: ParsedQuery
    ) = createPreparedBinder(
        returnType = returnType,
        adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(returnType, query)
    ) { callableImpl, dbField ->
        addStatement(
            "return $T.execute($N, $L, $L, $N)",
            RoomCoroutinesTypeNames.COROUTINES_ROOM,
            dbField,
            "true", // inTransaction
            callableImpl,
            continuationParam.simpleName.toString()
        )
    }

    override fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = createInsertBinder(
        typeArg = returnType,
        adapter = context.typeAdapterStore.findInsertAdapter(returnType, params)
    ) { callableImpl, dbField ->
        addStatement(
            "return $T.execute($N, $L, $L, $N)",
            RoomCoroutinesTypeNames.COROUTINES_ROOM,
            dbField,
            "true", // inTransaction
            callableImpl,
            continuationParam.simpleName.toString()
        )
    }

    override fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        createDeleteOrUpdateBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(returnType)
        ) { callableImpl, dbField ->
            addStatement(
                "return $T.execute($N, $L, $L, $N)",
                RoomCoroutinesTypeNames.COROUTINES_ROOM,
                dbField,
                "true", // inTransaction
                callableImpl,
                continuationParam.simpleName.toString()
            )
        }

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        CoroutineTransactionMethodBinder(
            adapter = TransactionMethodAdapter(executableElement.simpleName.toString(), callType),
            continuationParamName = continuationParam.simpleName.toString()
        )
}