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

package androidx.work

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.workers.TestWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OneTimeWorkRequestTest {
    @Test
    fun testOneTimeWorkRequestBuilder() {
        val builder = OneTimeWorkRequestBuilder<TestWorker>()
        builder.setInputMerger(ArrayCreatingInputMerger::class)
        val request = builder.build()
        assertEquals(request.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(request.workSpec.inputMergerClassName,
                ArrayCreatingInputMerger::class.java.name)
    }

    @Test
    fun testOneTimeWorkRequestBuilderDefaults() {
        val builder = OneTimeWorkRequestBuilder<TestWorker>()
        val request = builder.build()
        assertEquals(request.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(request.workSpec.inputMergerClassName,
                OverwritingInputMerger::class.java.name)
    }

    @Test
    fun testOneTimeWorkRequestFrom() {
        val request = OneTimeWorkRequestFrom<TestWorker>()
        assertEquals(request.workSpec.workerClassName, TestWorker::class.java.name)
    }
}
