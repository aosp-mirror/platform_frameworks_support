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

package androidx.room.solver.shortcut.binder

import androidx.room.ext.*
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType

/**
 * Binder for methods that return ListenableFuture<T>.
 *
 * The generic T may be any of Void, or List<V> where V is the input type in either single insert or
 * batch insert.
 *
 * The generation of the code in the call method is delegated to the [InstantInsertMethodBinder].
 *
 */
class GuavaListenableFutureInsertMethodBinder(
    val context: Context,
    private val listenableFutureTypeMirror: DeclaredType,
    adapter: InsertMethodAdapter?,
    val params: List<ShortcutQueryParameter>
) : InsertMethodBinder(adapter) {

    private val instantInsertMethodBinder = InstantInsertMethodBinder(adapter)

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {

        val callable = TypeSpec.anonymousClassBuilder("").apply {
            if (listenableFutureTypeMirror.typeArguments.first().typeName() == TypeName.VOID.box()) {
                superclass(java.util.concurrent.Callable::class.typeName())
            } else {
                superclass(ParameterizedTypeName.get(
                    java.util.concurrent.Callable::class.typeName(),
                    listenableFutureTypeMirror.typeArguments.first().typeName()
                ))
            }
            addMethod(createCallMethod(
                    parameters = parameters,
                    insertionAdapters = insertionAdapters,
                    scope = scope
            ))
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.createListenableFuture($N, $L, $L, $L)",
                RoomGuavaTypeNames.GUAVA_ROOM,
                DaoWriter.dbField,
                callable,
                null,
                /*canReleaseQuery*/ false)
        }
    }

    /**
     * Generate the implementation of the callable:
     * ```
     *  @Override
     *  public List<Long> call() throws Exception {
     *      __db.beginTransaction();
     *      try {
     *          List<Long> _result = __insertionAdapterOfPublisher.insertAndReturnIdsList(publishers);
     *          __db.setTransactionSuccessful();
     *          return _result;
     *      } finally {
     *         __db.endTransaction();
     *      }
     *  }
     * ```
     */
    private fun createCallMethod(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ): MethodSpec {
        val adapterScope = scope.fork()
        return MethodSpec.methodBuilder("call").apply {
            // Callable returns the LF's generic type argument.
            returns(listenableFutureTypeMirror.typeArguments.first().typeName())
            addException(Exception::class.typeName())
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            // delegate the generation of the code in the call method to the instant method binder
            instantInsertMethodBinder.convertAndReturn(
                    parameters = parameters,
                    insertionAdapters = insertionAdapters,
                    scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()
    }
}