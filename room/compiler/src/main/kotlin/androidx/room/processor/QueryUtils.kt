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

import androidx.room.parser.ParsedQuery
import androidx.room.parser.Section
import androidx.room.vo.EmbeddedField
import androidx.room.vo.EntityOrView
import androidx.room.vo.Pojo

object QueryUtils {

    fun expandProjection(
        query: ParsedQuery,
        pojo: Pojo,
        tables: List<EntityOrView>
    ): String {
        if (query.sections.none {
                it is Section.Projection.All ||
                        it is Section.Projection.Table }) {
            return query.original
        }
        val specifiedNames = query.sections.mapNotNull { section ->
            when (section) {
                is Section.Projection.Specific -> section.columnAlias
                else -> null
            }
        }
        return buildString {
            for (section in query.sections) {
                append(
                    when (section) {
                        is Section.Text -> section.text
                        is Section.BindVar -> section.text
                        is Section.Newline -> "\n"
                        is Section.Projection -> when (section) {
                            is Section.Projection.All -> expand(pojo, tables, specifiedNames)
                            is Section.Projection.Table -> expand(
                                pojo,
                                tables,
                                specifiedNames,
                                section.tableAlias
                            )
                            is Section.Projection.Specific -> section.text
                        }
                    }
                )
            }
        }
    }

    private fun expand(
        pojo: Pojo,
        tables: List<EntityOrView>,
        specifiedNames: List<String>,
        tableAlias: String? = null
    ): String {
        var embeddedFields = pojo.embeddedFields.map { embedded ->
            embedded to tables.find { it.typeName == embedded.pojo.typeName }
        }
        if (tableAlias != null) {
            val prefixed = embeddedFields.find { (embedded, _) -> embedded.prefix == tableAlias }
            if (prefixed != null) {
                embeddedFields = listOf(prefixed)
            } else {
                val table = embeddedFields.find { (_, table) -> table?.tableName == tableAlias }
                if (table != null) {
                    embeddedFields = listOf(table)
                }
            }
        }
        return (embeddedFields.flatMap { (embedded, table) ->
            expandEmbeddedField(embedded, table)
        } + pojo.fields.filter { field ->
            field.parent == null && field.columnName !in specifiedNames
        }.map { field ->
            "`${field.columnName}`"
        }).joinToString(", ")
    }

    private fun expandEmbeddedField(
        embedded: EmbeddedField,
        table: EntityOrView?
    ): List<String> {
        val pojo = embedded.pojo
        return if (table != null) {
            if (embedded.prefix.isNotEmpty()) {
                table.fields.map { field ->
                    "`${embedded.prefix}`.`${field.columnName}` " +
                            "AS `${embedded.prefix}${field.columnName}`"
                }
            } else {
                table.fields.map { field ->
                    "`${table.tableName}`.`${field.columnName}`"
                }
            }
        } else {
            if (embedded.prefix.isNotEmpty()) {
                pojo.fields.map { field ->
                    // TODO: Probably wrong
                    "`${embedded.prefix}`.`${field.columnName}` AS `${field.columnName}`"
                }
            } else {
                pojo.fields.map { field ->
                    "`${field.columnName}`"
                }
            }
        }
    }
}
