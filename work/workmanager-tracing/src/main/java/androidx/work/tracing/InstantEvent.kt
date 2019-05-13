/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.tracing

import android.util.JsonWriter

data class InstantEvent(
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scope: Scope = Scope.Thread
) : Traceable

fun InstantEvent.write(writer: JsonWriter) {
    writer.beginObject()
    writer.name("name").value(name)
        .name("ts").value(timestamp)
        .name("cname").value("grey")
    scope.write(writer)
    writer.endObject()
}
