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

package androidx.work.impl.background.gcm;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.impl.model.WorkSpec;

import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;


/**
 * Converts a {@link androidx.work.impl.model.WorkSpec} to a {@link Task}.
 */
public class GcmTaskConverter {

    // windowEnd must be greater than windowStart. Therefore this is the size of the window.
    @VisibleForTesting
    public static final long WINDOW_SIZE = 10L;

    static Task convert(@NonNull WorkSpec workSpec) {

        // Handle backoff policies.
        // Right now we don't have any way to control that using the GcmNetworkManager API directly.

        if (workSpec.isPeriodic()) {
            return convertPeriodic(workSpec);
        } else {
            return convertOneTime(workSpec);
        }
    }

    private static OneoffTask convertOneTime(@NonNull WorkSpec workSpec) {
        OneoffTask.Builder builder = new OneoffTask.Builder();
        builder.setService(WorkManagerGcmService.class)
                .setTag(workSpec.id)
                .setPersisted(false)
                .setExecutionWindow(workSpec.initialDelay, workSpec.initialDelay + WINDOW_SIZE)
                .setUpdateCurrent(true);

        applyConstraints(builder, workSpec);

        return builder.build();
    }

    private static PeriodicTask convertPeriodic(@NonNull WorkSpec workSpec) {
        PeriodicTask.Builder builder = new PeriodicTask.Builder();

        builder.setService(WorkManagerGcmService.class)
                .setTag(workSpec.id)
                .setPersisted(false)
                .setUpdateCurrent(true)
                .setPeriod(workSpec.intervalDuration)
                .setFlex(workSpec.flexDuration);

        applyConstraints(builder, workSpec);

        return builder.build();
    }

    private static Task.Builder applyConstraints(
            @NonNull Task.Builder builder,
            @NonNull WorkSpec workSpec) {

        // Apply defaults
        builder.setRequiresCharging(false);
        builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);

        if (workSpec.hasConstraints()) {
            Constraints constraints = workSpec.constraints;

            // Network Constraints
            NetworkType networkType = constraints.getRequiredNetworkType();
            switch (networkType) {
                case METERED:
                case NOT_ROAMING:
                case CONNECTED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED);
                    break;
                case UNMETERED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_UNMETERED);
                    break;
                case NOT_REQUIRED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);
            }

            // Charging constraints
            if (constraints.requiresCharging()) {
                builder.setRequiresCharging(true);
            } else {
                builder.setRequiresCharging(false);
            }

            // No support for requires battery not low, and requires storage not low.
        }

        return builder;
    }
}
