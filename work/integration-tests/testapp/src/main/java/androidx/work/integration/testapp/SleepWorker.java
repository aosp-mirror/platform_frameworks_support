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

package androidx.work.integration.testapp;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;

/**
 * A test worker that sleeps.
 */
public class SleepWorker extends Worker {
    private static final String TAG = "SleepWorker";

    @Override
    public @NonNull Result doWork() {
        try {
            Log.i(TAG, "Sleeping");
            Thread.sleep(200);
            Log.i(TAG, "Completed");
        } catch (InterruptedException ignore) {
            Log.v(TAG, "Interrupted.");
        }
        return Result.SUCCESS;
    }
}
