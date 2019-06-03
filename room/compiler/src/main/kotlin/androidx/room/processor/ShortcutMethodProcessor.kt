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
package androidx.room.processor

import androidx.room.ext.toAnnotationBox
import androidx.room.vo.Entity
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.findFieldByColumnName
import asTypeElement
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Common functionality for shortcut method processors
 */
class ShortcutMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)
    private val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)

    fun <T : Annotation> extractAnnotation(klass: KClass<T>, errorMsg: String): T? {
        val annotation = executableElement.toAnnotationBox(klass)
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation?.value
    }

    fun extractReturnType() = delegate.extractReturnType()

    fun extractParams(
        requiresPrimaryKeys: Boolean,
        missingParamError: String
    ): Pair<Map<String, ShortcutEntity>, List<ShortcutQueryParameter>> {
        val params = delegate.extractParams()
                .map { ShortcutParameterProcessor(
                        baseContext = context,
                        containing = containing,
                        element = it).process() }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)
        val entityPojoPair = params
                .filter { it.entityType != null && it.pojoType != null }
                .associateBy({ it.name }, {
                    val typeUtils = context.processingEnv.typeUtils
                    val entity = EntityProcessor(
                        context = context,
                        element = it.entityType!!.asTypeElement()).process()
                    val pojo = if (!typeUtils.isSameType(it.entityType, it.pojoType)) {
                        // Param entity and pojo are not the same, process and validate partial
                        // entity.
                        PojoProcessor.createFor(
                            context = context,
                            element = it.pojoType!!.asTypeElement(),
                            bindingScope = FieldProcessor.BindingScope.BIND_TO_STMT,
                            parent = null
                        ).process().also { validatePartialEntity(it, entity, requiresPrimaryKeys) }
                    } else {
                        entity
                    }
                    ShortcutEntity(
                        entity = entity,
                        partialEntity = pojo)
                })
        return Pair(entityPojoPair, params)
    }

    private fun validatePartialEntity(pojo: Pojo, asEntity: Entity, requiresPrimaryKeys: Boolean) {
        // TODO: For insert verify all non null columns without a default value are in the pojo
        // ^ otherwise the INSERT will fail with a NOT NULL constraint
        pojo.fields.forEach { field ->
            if (asEntity.findFieldByColumnName(field.columnName) == null) {
                // TODO: Better message
                context.logger.e(field.element, "Can't find column in AsEntity class")
            }
        }

        if (requiresPrimaryKeys &&
            asEntity.primaryKey.fields.any { pojo.findFieldByColumnName(it.columnName) == null }) {
            // TODO: Better message
            context.logger.e(pojo.element, "Partial Entity requires fields with primary keys")
        }

        if (pojo.relations.isNotEmpty()) {
            // TODO: Support inserting relations.
            context.logger.e(pojo.element, "Partial Entity can't contain @Relation")
        }
    }

    fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = delegate.findInsertMethodBinder(returnType, params)

    fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        delegate.findDeleteOrUpdateMethodBinder(returnType)
}
