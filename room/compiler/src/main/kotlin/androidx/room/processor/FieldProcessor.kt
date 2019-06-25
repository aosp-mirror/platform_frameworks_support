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

import androidx.room.ColumnInfo
import androidx.room.ext.toAnnotationBox
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Field
import com.squareup.javapoet.TypeName
import java.util.Locale
import javax.lang.model.element.Element
import javax.lang.model.type.DeclaredType

class FieldProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val element: Element,
    val bindingScope: BindingScope,
                     // pass only if this is processed as a child of Embedded field
    val fieldParent: EmbeddedField?
) {
    val context = baseContext.fork(element)
    fun process(): Field {
        val member = context.processingEnv.typeUtils.asMemberOf(containing, element)
        val type = TypeName.get(member)
        val columnInfo = element.toAnnotationBox(ColumnInfo::class)?.value
        val name = element.simpleName.toString()
        val rawCName = if (columnInfo != null && columnInfo.name != ColumnInfo.INHERIT_FIELD_NAME) {
            columnInfo.name
        } else {
            name
        }
        val columnName = (fieldParent?.prefix ?: "") + rawCName
        val affinity = try {
            SQLTypeAffinity.fromAnnotationValue(columnInfo?.typeAffinity)
        } catch (ex: NumberFormatException) {
            null
        }

        context.checker.notBlank(columnName, element,
                ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
        context.checker.notUnbound(type, element,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)

        val adapter = context.typeAdapterStore.findColumnTypeAdapter(member, affinity)
        val adapterAffinity = adapter?.typeAffinity ?: affinity
        val nonNull = Field.calcNonNull(element, fieldParent)

        val field = Field(
            name = name,
            type = member,
            element = element,
            columnName = columnName,
            affinity = affinity,
            collate = Collate.fromAnnotationValue(columnInfo?.collate),
            defaultValue = extractDefaultValue(
                columnInfo?.defaultValue, adapterAffinity, nonNull
            ),
            parent = fieldParent,
            indexed = columnInfo?.index ?: false,
            nonNull = nonNull
        )

        when (bindingScope) {
            BindingScope.TWO_WAY -> {
                field.statementBinder = adapter
                field.cursorValueReader = adapter
                field.affinity = adapterAffinity
                context.checker.check(adapter != null, field.element,
                        ProcessorErrors.CANNOT_FIND_COLUMN_TYPE_ADAPTER)
            }
            BindingScope.BIND_TO_STMT -> {
                field.statementBinder = context.typeAdapterStore
                        .findStatementValueBinder(field.type, field.affinity)
                context.checker.check(field.statementBinder != null, field.element,
                        ProcessorErrors.CANNOT_FIND_STMT_BINDER)
            }
            BindingScope.READ_FROM_CURSOR -> {
                field.cursorValueReader = context.typeAdapterStore
                        .findCursorValueReader(field.type, field.affinity)
                context.checker.check(field.cursorValueReader != null, field.element,
                        ProcessorErrors.CANNOT_FIND_CURSOR_READER)
            }
        }

        return field
    }

    private fun extractDefaultValue(
        value: String?,
        affinity: SQLTypeAffinity?,
        fieldNonNull: Boolean
    ): String? {
        if (value == null) {
            return null
        }
        val trimmed = value.trim().toLowerCase(Locale.ENGLISH)
        val defaultValue = if (affinity == SQLTypeAffinity.TEXT) {
            if (value == ColumnInfo.VALUE_UNSPECIFIED) {
                null
            } else if (trimmed.startsWith("(") || trimmed in SQLITE_VALUE_CONSTANTS) {
                value
            } else {
                "'${value.trim('\'')}'"
            }
        } else {
            if (value == ColumnInfo.VALUE_UNSPECIFIED || trimmed == "") {
                null
            } else {
                value
            }
        }
        if (trimmed == "null" && fieldNonNull) {
            context.logger.e(element, ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
        }
        return defaultValue
    }

    /**
     * Defines what we need to assign
     */
    enum class BindingScope {
        TWO_WAY, // both bind and read.
        BIND_TO_STMT, // just value to statement
        READ_FROM_CURSOR // just cursor to value
    }
}

internal val SQLITE_VALUE_CONSTANTS = listOf(
    "null",
    "current_time",
    "current_date",
    "current_timestamp",
    "true",
    "false"
)
