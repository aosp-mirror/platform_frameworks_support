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

import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write delete and update methods that return Completable/Single/Maybe.
 *
 * For example, the generated code for the DAO function:
 * ```
 * @Delete
 * fun deletePublishers(vararg publishers: Publisher): Single<Int>
 * ```
 * Will be:
 * ```
 * public override fun deletePublishers(vararg publishers: Publisher): Single<Int> {
 *  return Single.fromCallable(Callable {
 *      var _total = 0
 *      __db.beginTransaction()
 *      try {
 *          _total += __deletionAdapterOfPublisher.handleMultiple(publishers)
 *          __db.setTransactionSuccessful()
 *          return@Callable _total
 *      } finally {
 *      __db.endTransaction()
 *      }
 *  })
 * }
 * ```
 * The generation of the code in the call method is delegated to
 * [InstantDeleteOrUpdateMethodBinder].
 */
class RxCallableDeleteOrUpdateMethodBinder(
    private val rxType: RxType,
    private val typeMirror: TypeMirror,
    adapter: DeleteOrUpdateMethodAdapter?
) : DeleteOrUpdateMethodBinder(adapter) {

    private val instantDeleteOrUpdateMethodBinder = InstantDeleteOrUpdateMethodBinder(adapter)

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<PropertySpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val callable = TypeSpec.anonymousClassBuilder().apply {
            val typeName = typeMirror.typeName()
            if (rxType == RxType.COMPLETABLE) {
                // Since Completable is not parameterized and the Callable should return Void
                // We can just create a Callable without type
                superclass(java.util.concurrent.Callable::class.typeName())
            } else {
                // Create a parameterized Callable object
                superclass(
                        java.util.concurrent.Callable::class.typeName().parameterizedBy(
                        typeName
                ))
            }
            addFunction(createCallMethod(
                    parameters = parameters,
                    insertionAdapters = adapters,
                    scope = scope
            ))
        }.build()
        scope.builder().apply {
            addStatement("return %T.fromCallable(%L)", rxType.className, callable)
        }
    }

    /**
     * Generate the implementation of the callable:
     * ```
     *  @Override
     *  public Integer call() throws Exception {
     *      int _total = 0;
     *      __db.beginTransaction();
     *      try {
     *          __deletionAdapterOfPublisher.handleMultiple(publishers);
     *          __db.setTransactionSuccessful();
     *          return _total;
     *      } finally {
     *      __db.endTransaction();
     *  }
     * ```
     */
    private fun createCallMethod(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<PropertySpec, TypeSpec>>,
        scope: CodeGenScope
    ): FunSpec {
        val adapterScope = scope.fork()
        return FunSpec.builder("call").apply {
            // For completable, we just return Void, instead of the type
            if (rxType == RxType.COMPLETABLE) {
                returns(Void::class.typeName())
            } else {
                returns(typeMirror.typeName())
            }
            addModifiers(KModifier.OVERRIDE)
            // delegate the generation of the code in the call method to the instant method binder
            instantDeleteOrUpdateMethodBinder.convertAndReturn(
                    parameters = parameters,
                    adapters = insertionAdapters,
                    scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()
    }

    /**
     * Supported types for delete and update
     */
    enum class RxType(val className: ClassName) {
        SINGLE(RxJava2TypeNames.SINGLE),
        MAYBE(RxJava2TypeNames.MAYBE),
        COMPLETABLE(RxJava2TypeNames.COMPLETABLE)
    }
}