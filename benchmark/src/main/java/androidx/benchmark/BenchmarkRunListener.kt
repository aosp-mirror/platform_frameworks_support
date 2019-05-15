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

import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Result
import org.junit.runner.notification.RunListener
import java.io.File

/**
 * A JUnit RunListener which triggers benchmark reporting at the end of all tests.
 *
 * Declare this class in the metadata of an androidTest manifest to register it as a listener.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("unused")
class BenchmarkRunListener : RunListener() {
    override fun testRunFinished(result: Result?) {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext!!.packageName
        val filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        val file = File(filePath, "$packageName-benchmarkData.json")
        ResultWriter.writeReport(file, ResultWriter.reports)
    }
}