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

package androidx.work.impl.foreground;

import static androidx.work.impl.foreground.SystemForegroundDispatcher.createExecuteIntent;
import static androidx.work.impl.foreground.SystemForegroundDispatcher.createStopIntent;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.work.impl.Scheduler;
import androidx.work.impl.model.WorkSpec;

/**
 * A foreground {@link Scheduler} that schedules jobs with foreground affinity. It delegates the
 * actual work to the {@link SystemForegroundService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundScheduler implements Scheduler {
    private final Context mContext;

    public SystemForegroundScheduler(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        if (workSpecs != null) {
            for (WorkSpec workSpec : workSpecs) {
                Intent intent = createExecuteIntent(mContext, workSpec.id);
                ContextCompat.startForegroundService(mContext, intent);
            }
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        Intent intent = createStopIntent(mContext, workSpecId);
        ContextCompat.startForegroundService(mContext, intent);
    }
}
