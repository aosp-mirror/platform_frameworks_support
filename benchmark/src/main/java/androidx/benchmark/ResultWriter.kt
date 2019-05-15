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

package androidx.benchmark

import android.util.JsonWriter
import androidx.annotation.VisibleForTesting
import java.io.File

internal object ResultWriter {
    @VisibleForTesting
    internal val reports = ArrayList<BenchmarkState.Report>()

    fun appendReport(report: BenchmarkState.Report) {
        reports.add(report)
    }

    internal fun writeReport(file: File, reports: List<BenchmarkState.Report>) {
        file.run {
            if (!exists()) {
                parentFile.mkdirs()
                createNewFile()
            }

            val writer = JsonWriter(bufferedWriter())
            writer.setIndent("    ")

            writer.beginArray()
            reports.forEach { it.write(writer) }
            writer.endArray()

            writer.flush()
            writer.close()
        }
    }

    private fun BenchmarkState.Report.write(writer: JsonWriter): JsonWriter {
        writer.beginObject()
            .name("name").value(testName)
            .name("className").value(className)
            .name("nanos").value(nanos)
            .name("warmupIterations").value(warmupIterations)
            .name("repeatIterations").value(repeatIterations)
            .name("runs").beginArray().also { data.forEach { writer.value(it) } }.endArray()
        writer.endObject()
        return writer
    }
}
