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

package androidx.room.writer.binder

import androidx.room.solver.CodeGenScope
import androidx.room.vo.InsertionMethod
import androidx.room.vo.ShortcutMethod
import androidx.room.writer.ShortcutMethodWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

abstract class ShortcutMethodBinder(val writer: ShortcutMethodWriter) {

    abstract fun writeInsertionMethod(
        method: InsertionMethod,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    )

    abstract fun writeUpdateOrDeleteMethod(
        method: ShortcutMethod,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    )
}