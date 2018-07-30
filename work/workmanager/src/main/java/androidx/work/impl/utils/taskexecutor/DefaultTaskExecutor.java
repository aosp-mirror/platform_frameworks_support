/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.utils.taskexecutor;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default Task Executor for executing common tasks in WorkManager
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultTaskExecutor implements TaskExecutor {
    private final ScheduledExecutorService mBackgroundExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void postToMainThread(Runnable r) {
        mMainThreadHandler.post(r);
    }

    @Override
    public void executeOnBackgroundThread(Runnable r) {
        mBackgroundExecutor.execute(r);
    }

    @Override
    public void executeOnBackgroundThread(Runnable r, long delay, TimeUnit timeUnit) {
        mBackgroundExecutor.schedule(r, delay, timeUnit);
    }
}
