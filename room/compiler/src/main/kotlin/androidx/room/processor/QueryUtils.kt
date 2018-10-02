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

import androidx.room.ext.toAnnotationBox
import androidx.room.parser.ParsedQuery
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Pojo

object QueryUtils {

    private val SELECT_WILDCARD = Regex("""^\s*SELECT\s+\*\s+FROM.*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun inferProjection(
        query: ParsedQuery,
        embeddedFields: List<EmbeddedField>
    ): String {
        if (!query.original.matches(SELECT_WILDCARD) || embeddedFields.isEmpty()) {
            return query.original
        }

        // Only one embedded field can be non-prefixed
        if (embeddedFields.count { it.prefix.isEmpty() } > 1) {
            return query.original
        }

        // Makes sure that table aliases and field prefixes match
        if (embeddedFields.asSequence().filter { it.prefix.isNotEmpty() }.any { field ->
            query.tables.none { (name, alias) ->
                extractTableOrViewName(field.pojo) == name && field.prefix == alias
            }
        }) {
            // TODO: Warning?
            return query.original
        }

        // Rewrite the star.
        val index = query.original.indexOf('*')
        return query.original.substring(0, index) +
                embeddedFields.flatMap { embeddedField ->
                    embeddedField.pojo.fields.map { field ->
                        val pojoName = extractTableOrViewName(embeddedField.pojo)
                        val prefix = if (embeddedField.prefix.isEmpty()) {
                            pojoName
                        } else {
                            embeddedField.prefix
                        }
                        val columnName = field.columnName.removePrefix(prefix)
                        buildString {
                            append("`$prefix`.`$columnName`")
                            if (prefix != pojoName) {
                                append(" AS `$prefix$columnName`")
                            }
                        }
                    }
                }.joinToString(", ") +
                query.original.substring(index + 1)
    }

    private fun extractTableOrViewName(pojo: Pojo): String {
        val entity = pojo.element.toAnnotationBox(androidx.room.Entity::class)
        if (entity != null && entity.value.tableName.isNotEmpty()) {
            return entity.value.tableName
        }
        val view = pojo.element.toAnnotationBox(androidx.room.DatabaseView::class)
        if (view != null && view.value.viewName.isNotEmpty()) {
            return view.value.viewName
        }
        return pojo.element.simpleName.toString()
    }
}
