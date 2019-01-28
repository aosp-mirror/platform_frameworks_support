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
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.background.gcm.GcmTaskConverter.EXECUTION_WINDOW_SIZE
import com.google.android.gms.gcm.Task
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class GcmTaskConverterTest {

    @Test
    fun testOneTimeRequest_noInitialDelay() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        val task = GcmTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, 0L)
        assertEquals(task.windowEnd, 0L + EXECUTION_WINDOW_SIZE)
    }

    @Test
    fun testOneTimeRequest_noInitialDelay_withConstraints() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.METERED)
            .setRequiresCharging(true)
            .build()

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setConstraints(constraints)
            .build()

        val task = GcmTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_CONNECTED)
        assertEquals(task.requiresCharging, true)
        assertEquals(task.windowStart, 0L)
        assertEquals(task.windowEnd, 0L + EXECUTION_WINDOW_SIZE)
    }

    @Test
    fun testOneTimeRequest_noInitialDelay_withConstraints2() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setConstraints(constraints)
            .build()

        val task = GcmTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_UNMETERED)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, 0L)
        assertEquals(task.windowEnd, 0L + EXECUTION_WINDOW_SIZE)
    }

    @Test
    fun testOneTimeRequest_hasInitialDelay() {
        val initialDelay = 10L
        val initialDelayInMillis = TimeUnit.SECONDS.toMillis(initialDelay)

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .build()

        val task = GcmTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, initialDelayInMillis)
        assertEquals(task.windowEnd, initialDelayInMillis + EXECUTION_WINDOW_SIZE)
    }

    @Test
    fun testOneTimeWorkRequest_backedOff() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        val workSpec = request.workSpec
        workSpec.runAttemptCount = 1
        val expectedWindowStart = workSpec.calculateNextRunTime()

        val task = GcmTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, expectedWindowStart)
        assertEquals(task.windowEnd, expectedWindowStart + EXECUTION_WINDOW_SIZE)
    }

    @Test
    @SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
    fun testPeriodicWorkRequest_firstRun() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(15L, TimeUnit.MINUTES)
            .build()

        val expected = TimeUnit.MINUTES.toMillis(15L)
        val task = GcmTaskConverter.convert(request.workSpec)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, expected) // should be in the past
        assertEquals(task.windowEnd, expected + EXECUTION_WINDOW_SIZE)
    }

    @Test
    @SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
    fun testPeriodicWorkRequest_withFlex_firstRun() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(
            15L, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).build()

        val now = System.currentTimeMillis()
        val task = GcmTaskConverter.convert(request.workSpec)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertThat(task.windowStart, greaterThan(now)) // should be in the future
    }

    @Test
    @SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
    fun testPeriodicWorkRequest_withFlex_nextRun() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(
            15L, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).build()

        val now = System.currentTimeMillis()
        request.workSpec.periodStartTime = now
        val expected = now + TimeUnit.MINUTES.toMillis(15)

        val task = GcmTaskConverter.convert(request.workSpec)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, expected)
        assertEquals(task.windowEnd, expected + EXECUTION_WINDOW_SIZE)
    }
}
