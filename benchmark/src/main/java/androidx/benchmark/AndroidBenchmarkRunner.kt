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

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class AndroidBenchmarkRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        println("creating runner")
        // Keep at least one core busy. Together with a single threaded benchmark, this makes the
        // process get multi-threaded setSustainedPerformanceMode.
        //
        // We want to keep to the 50% clocks of the multi-threaded benchmark mode to avoid
        // any benchmarks running at higher clocks than any others.
        object : Thread() {
            override fun run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
                while (true);
            }
        }.start()

    }

    override fun onDestroy() {
        println("destroying runner")
    }

    override fun callActivityOnStart(activity: Activity) {
        super.callActivityOnStart(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            println("starting activity!! $activity")
            activity.window.setSustainedPerformanceMode(true)
        }
    }
}
