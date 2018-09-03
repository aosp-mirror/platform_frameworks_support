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

package androidx.room.solver.shortcut.binderprovider

import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.solver.shortcut.binder.RxCallableInsertMethodBinder
import androidx.room.vo.ShortcutQueryParameter
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

sealed class RxCallableInsertMethodBinderProvider(
    val context: Context,
    val rxType: RxCallableInsertMethodBinder.RxType
) : InsertMethodBinderProvider {

    private val hasRxJava2Artifact by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(RoomRxJava2TypeNames.RX_ROOM.toString()) != null
    }

    abstract fun extractTypeArg(declared: DeclaredType): TypeMirror

    override fun matches(declared: DeclaredType): Boolean =
            declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: DeclaredType): Boolean {
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        val match = erasure.typeName() == rxType.className
        if (match && !hasRxJava2Artifact) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
        }
        return match
    }

    override fun provide(
        declared: DeclaredType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder {
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findInsertAdapter(typeArg, params)
        return RxCallableInsertMethodBinder(rxType, typeArg, adapter)
    }
}

class RxSingleInsertMethodBinderProvider(context: Context)
    : RxCallableInsertMethodBinderProvider(context, RxCallableInsertMethodBinder.RxType.SINGLE) {
    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxMaybeInsertMethodBinderProvider(context: Context)
    : RxCallableInsertMethodBinderProvider(context, RxCallableInsertMethodBinder.RxType.MAYBE) {
    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()
}

class RxCompletableInsertMethodBinderProvider(context: Context)
    : RxCallableInsertMethodBinderProvider(context,
        RxCallableInsertMethodBinder.RxType.COMPLETABLE) {

    private val completableTypeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(RxJava2TypeNames.COMPLETABLE.toString())?.asType()
    }

    override fun extractTypeArg(declared: DeclaredType): TypeMirror =
            context.processingEnv.elementUtils.getTypeElement("java.lang.Void").asType()

    override fun matches(declared: DeclaredType): Boolean = isCompletable(declared)

    private fun isCompletable(declared: DeclaredType): Boolean {
        if (completableTypeMirror == null) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return context.processingEnv.typeUtils.isAssignable(completableTypeMirror, erasure)
    }
}
