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

package androidx.room.vo

import androidx.room.parser.ParsedQuery
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class DatabaseView(
    element: TypeElement,
    val viewName: String,
    val query: ParsedQuery,
    type: DeclaredType,
    fields: List<Field>,
    embeddedFields: List<EmbeddedField>,
    constructor: Constructor?
)
    : Pojo(element, type, fields, embeddedFields, emptyList(), constructor) {

    val createViewQuery by lazy {
        // This query should match exactly like it is stored in sqlite_master. The query is
        // trimmed. "IF NOT EXISTS" will be omitted.
        "CREATE VIEW `$viewName` AS ${query.original.trim()}"
    }

    /**
     * List of all the underlying tables including those that are indirectly referenced.
     *
     * This is populated by DatabaseProcessor. This cannot be an immutable constructor parameter
     * as it can only be known after all the other views are initialized and parsed.
     */
    val tables = mutableSetOf<String>()

    /**
     * The number of steps it took to resolve [tables]. This is necessary to sort views by their
     * dependencies.
     */
    var stepsToResolve = Int.MAX_VALUE
}
