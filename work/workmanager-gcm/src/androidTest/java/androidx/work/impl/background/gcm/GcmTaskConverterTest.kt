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

package androidx.work.impl.background.gcm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.impl.background.gcm.GcmTaskConverter.WINDOW_SIZE
import com.google.android.gms.gcm.OneoffTask
import com.google.android.gms.gcm.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GcmTaskConverterTest {
    @Test
    fun testOneTimeRequest_noInitialDelay() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        val task = GcmTaskConverter.convert(request.workSpec) as OneoffTask
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.windowStart, 0L)
        assertEquals(task.windowEnd, 0L + WINDOW_SIZE)
    }
}
