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

package androidx.room.solver.shortcutbinder

import androidx.room.solver.CodeGenScope
import androidx.room.vo.InsertionMethod
import javax.lang.model.type.DeclaredType

abstract class ShortcutMethodBinder(val writer : ShortcutWriter) {
    abstract fun write()
}

class InstantBinder(writer: ShortcutWriter) : ShortcutMethodBinder(writer = writer) {
    override fun write(scope : CodeGenScope) {
        if (needwrite) {
            scope.builder().addStatement("$L fdas // declare types")
        }
        writer.createInsertionMethodBody(memt, insertadap)
    }
}

class RxBinder(writer: ShortcutWriter) : ShortcutMethodBinder(writer = writer) {
    override fun write() {
        addState(Rx.fromCalb )
        addBlock(
                InstantBInder(writer).write(scopeb)
        )
    }
}