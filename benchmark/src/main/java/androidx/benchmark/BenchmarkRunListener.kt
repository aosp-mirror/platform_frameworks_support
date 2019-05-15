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

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Result
import org.junit.runner.notification.RunListener
import java.io.File

/**
 * A JUnit RunListener which triggers benchmark reporting at the end of all tests.
 *
 * Declare this class in the metadata of an androidTest manifest to register it as a listener.
 */
@Suppress("unused")
class BenchmarkRunListener : RunListener() {
    override fun testRunFinished(result: Result?) {
        // Currently, we just overwrite the whole file
        // Ideally, append for efficiency
        val packageName =
            InstrumentationRegistry.getInstrumentation().targetContext!!.packageName
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$packageName-benchmarkData.json"
        )
        ResultWriter.writeReport(file, ResultWriter.reports)
    }
}