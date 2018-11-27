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

import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.ext.KotlinMetadataProcessor
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.findKotlinSuspendFunctionReturnType
import androidx.room.ext.hasAnnotation
import androidx.room.ext.isEntityElement
import androidx.room.ext.toAnnotationBox
import androidx.room.ext.typeName
import androidx.room.parser.SqlParser
import androidx.room.processor.ProcessorErrors.RAW_QUERY_STRING_PARAMETER_REMOVED
import androidx.room.vo.RawQueryMethod
import asTypeElement
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class RawQueryMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) : KotlinMetadataProcessor {
    val context = baseContext.fork(executableElement)

    // for kotlin metadata
    override val processingEnv: ProcessingEnvironment
        get() = context.processingEnv

    private val classMetadata =
        try {
            containing.asElement().kotlinMetadata
        } catch (throwable: Throwable) {
            context.logger.d(executableElement,
                "failed to read get kotlin metadata from %s", executableElement)
        } as? KotlinClassMetadata

    fun process(): RawQueryMethod {
        val types = context.processingEnv.typeUtils
        val asMember = types.asMemberOf(containing, executableElement)
        val isSuspendFunction = classMetadata?.isSuspendFunction(executableElement) ?: false
        val returnType = if (isSuspendFunction) {
            executableElement.findKotlinSuspendFunctionReturnType()
        } else {
            MoreTypes.asExecutable(asMember).returnType
        }

        context.checker.check(executableElement.hasAnnotation(RawQuery::class), executableElement,
                ProcessorErrors.MISSING_RAWQUERY_ANNOTATION)

        val returnTypeName = TypeName.get(returnType)
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)
        val observedTableNames = processObservedTables()
        val query = SqlParser.rawQueryForTables(observedTableNames)
        // build the query but don't calculate result info since we just guessed it.
        val resultBinder = context.typeAdapterStore
            .findQueryResultBinder(executableElement, isSuspendFunction, returnType, query)
        val runtimeQueryParam = findRuntimeQueryParameter()
        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        val rawQueryMethod = RawQueryMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                observedTableNames = observedTableNames,
                returnType = returnType,
                runtimeQueryParam = runtimeQueryParam,
                inTransaction = inTransaction,
                queryResultBinder = resultBinder
        )
        context.checker.check(rawQueryMethod.returnsValue, executableElement,
                ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE)
        return rawQueryMethod
    }

    private fun processObservedTables(): Set<String> {
        val annotation = executableElement.toAnnotationBox(RawQuery::class)
        return annotation?.getAsTypeMirrorList("observedEntities")
                ?.map {
                    it.asTypeElement()
                }
                ?.flatMap {
                    if (it.isEntityElement()) {
                        val entity = EntityProcessor(
                                context = context,
                                element = it
                        ).process()
                        arrayListOf(entity.tableName)
                    } else {
                        val pojo = PojoProcessor.createFor(
                                context = context,
                                element = it,
                                bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                                parent = null
                        ).process()
                        val tableNames = pojo.accessedTableNames()
                        // if it is empty, report error as it does not make sense
                        if (tableNames.isEmpty()) {
                            context.logger.e(executableElement,
                                    ProcessorErrors.rawQueryBadEntity(it.asType().typeName()))
                        }
                        tableNames
                    }
                }?.toSet() ?: emptySet()
    }

    private fun findRuntimeQueryParameter(): RawQueryMethod.RuntimeQueryParameter? {
        val types = context.processingEnv.typeUtils
        if (executableElement.parameters.size == 1 && !executableElement.isVarArgs) {
            val param = MoreTypes.asMemberOf(
                    types,
                    containing,
                    executableElement.parameters[0])
            val elementUtils = context.processingEnv.elementUtils
            val supportQueryType = elementUtils
                    .getTypeElement(SupportDbTypeNames.QUERY.toString()).asType()
            val isSupportSql = types.isAssignable(param, supportQueryType)
            if (isSupportSql) {
                return RawQueryMethod.RuntimeQueryParameter(
                        paramName = executableElement.parameters[0].simpleName.toString(),
                        type = supportQueryType.typeName())
            }
            val stringType = elementUtils.getTypeElement("java.lang.String").asType()
            val isString = types.isAssignable(param, stringType)
            if (isString) {
                // special error since this was initially allowed but removed in 1.1 beta1
                context.logger.e(executableElement, RAW_QUERY_STRING_PARAMETER_REMOVED)
                return null
            }
        }
        context.logger.e(executableElement, ProcessorErrors.RAW_QUERY_BAD_PARAMS)
        return null
    }
}