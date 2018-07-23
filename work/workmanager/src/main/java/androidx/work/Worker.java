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

package androidx.work;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

/**
 * The basic object that performs work.  Worker classes are instantiated at runtime by
 * {@link WorkManager} and the {@link #doWork()} method is called on a background thread.  In case
 * the work is preempted for any reason, the same instance of Worker is not reused.  This means
 * that {@link #doWork()} is called exactly once per Worker instance.
 */
public abstract class Worker extends NonBlockingWorker {
    @WorkerThread
    public abstract @NonNull Result doWork();

    @Override
    public void onStartWork() {
        Result result = doWork();
        onWorkFinished(result);
    }
}
