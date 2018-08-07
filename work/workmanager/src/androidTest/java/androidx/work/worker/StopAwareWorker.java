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

package androidx.work.worker;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;

/**
 * A Worker that loops until it has been stopped.
 */
public class StopAwareWorker extends Worker {

    private static final String TAG = "StopAwareWorker";

    @Override
    public @NonNull Result doWork() {
        while (!isStopped()) {
            // Adding a log message to work around a check for empty while loops.
            Log.d(TAG, "Working");
        }
        return Result.SUCCESS;
    }
}
