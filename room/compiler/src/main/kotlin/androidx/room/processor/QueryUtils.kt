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
import androidx.room.vo.EmbeddedField

object QueryUtils {

    private val SELECT_WILDCARD = Regex("""^\s*SELECT\s+\*\s+FROM.*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun normalizeQuery(query: ParsedQuery, embeddedFields: List<EmbeddedField>): String {
        if (query.original.matches(SELECT_WILDCARD) && embeddedFields.isNotEmpty()) {
            val aliases = query.tables.map { (name, alias) ->
                name to alias
            }.toMap()
            // Use the original if an @Embedded prefix does not match with the alias.
            for (embeddedField in embeddedFields) {
                val pojo = embeddedField.pojo
                if (aliases[pojo.name] !in listOf(embeddedField.prefix, pojo.name)) {
                    return query.original
                }
            }
            val index = query.original.indexOf('*')
            return query.original.substring(0, index) +
                    embeddedFields.flatMap { embeddedField ->
                        embeddedField.pojo.fields.map { field ->
                            val prefix = if (embeddedField.prefix.isEmpty()) {
                                embeddedField.pojo.name
                            } else {
                                embeddedField.prefix
                            }
                            buildString {
                                append("`$prefix`.`${field.columnName}`")
                                if (prefix != embeddedField.pojo.name) {
                                    append(" AS `$prefix${field.columnName}`")
                                }
                            }
                        }
                    }.joinToString(", ") +
                    query.original.substring(index + 1)
        }
        return query.original
    }
}
