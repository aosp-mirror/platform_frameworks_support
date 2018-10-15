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

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.ext.hasAnnotation
import androidx.room.ext.typeName
import androidx.room.vo.TableOrView
import androidx.room.vo.Field
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

interface TableOrViewProcessor {
    fun process(): TableOrView
}

/**
 * A dummy implementation of [TableOrViewProcessor] that just prints a processor error for use of
 * an invalid class as [TableOrView].
 */
private class NotTableOrViewProcessor(
    val context: Context,
    val element: TypeElement,
    private val referenceStack: LinkedHashSet<Name>
) : TableOrViewProcessor {

    override fun process(): TableOrView {
        context.logger.e(element, ProcessorErrors.NOT_ENTITY_OR_VIEW)
        // Parse this as a Pojo in case there are more errors.
        PojoProcessor.createFor(
                context = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                parent = null,
                referenceStack = referenceStack).process()
        return object : TableOrView {
            override val fields: List<Field>
                get() = emptyList()
            override val tableName: String
                get() = typeName.toString()
            override val typeName: TypeName
                get() = element.asType().typeName()
        }
    }
}

@Suppress("FunctionName")
fun DatabaseItemProcessor(
    context: Context,
    element: TypeElement,
    referenceStack: LinkedHashSet<Name> = LinkedHashSet()
): TableOrViewProcessor {
    return when {
        element.hasAnnotation(Entity::class) ->
            EntityProcessor(context, element, referenceStack)
        element.hasAnnotation(DatabaseView::class) ->
            DatabaseViewProcessor(context, element)
        else ->
            NotTableOrViewProcessor(context, element, referenceStack)
    }
}
