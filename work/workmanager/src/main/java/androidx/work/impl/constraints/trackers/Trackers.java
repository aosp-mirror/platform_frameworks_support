/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers;

import android.content.Context;
<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
=======

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)

/**
 * A singleton class to hold an instance of each {@link ConstraintTracker}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Trackers {

    private static Trackers sInstance;

    /**
     * Gets the singleton instance of {@link Trackers}.
     *
     * @param context The initializing context (we only use the application context)
     * @return The singleton instance of {@link Trackers}.
     */
    @NonNull
    public static synchronized Trackers getInstance(Context context, TaskExecutor taskExecutor) {
        if (sInstance == null) {
            sInstance = new Trackers(context, taskExecutor);
        }
        return sInstance;
    }

    /**
     * Sets an instance of the {@link Trackers} for testing.
     */
    @VisibleForTesting
    public static synchronized void setInstance(@NonNull Trackers trackers) {
        sInstance = trackers;
    }

    private BatteryChargingTracker mBatteryChargingTracker;
    private BatteryNotLowTracker mBatteryNotLowTracker;
    private NetworkStateTracker mNetworkStateTracker;
    private StorageNotLowTracker mStorageNotLowTracker;

    private Trackers(@NonNull Context context, @NonNull TaskExecutor taskExecutor) {
        Context appContext = context.getApplicationContext();
        mBatteryChargingTracker = new BatteryChargingTracker(appContext, taskExecutor);
        mBatteryNotLowTracker = new BatteryNotLowTracker(appContext, taskExecutor);
        mNetworkStateTracker = new NetworkStateTracker(appContext, taskExecutor);
        mStorageNotLowTracker = new StorageNotLowTracker(appContext, taskExecutor);
    }

    /**
     * Gets the tracker used to track the battery charging status.
     *
     * @return The tracker used to track battery charging status
     */
    @NonNull
    public BatteryChargingTracker getBatteryChargingTracker() {
        return mBatteryChargingTracker;
    }

    /**
     * Gets the tracker used to track if the battery is okay or low.
     *
     * @return The tracker used to track if the battery is okay or low
     */
    @NonNull
    public BatteryNotLowTracker getBatteryNotLowTracker() {
        return mBatteryNotLowTracker;
    }

    /**
     * Gets the tracker used to track network state changes.
     *
     * @return The tracker used to track state of the network
     */
    @NonNull
    public NetworkStateTracker getNetworkStateTracker() {
        return mNetworkStateTracker;
    }

    /**
     * Gets the tracker used to track if device storage is okay or low.
     *
     * @return The tracker used to track if device storage is okay or low.
     */
    @NonNull
    public StorageNotLowTracker getStorageNotLowTracker() {
        return mStorageNotLowTracker;
    }
}
