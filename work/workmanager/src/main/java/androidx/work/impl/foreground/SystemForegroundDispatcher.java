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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles requests for executing {@link androidx.work.WorkRequest}s on behalf of
 * {@link SystemForegroundService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundDispatcher implements ExecutionListener, WorkConstraintsCallback {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("SystemFgDispatcher");

    // actions
    private static final String ACTION_EXECUTE = "ACTION_EXECUTE";
    private static final String ACTION_STOP = "ACTION_STOP";
    // keys
    private static final String KEY_WORKSPEC_ID = "KEY_WORKSPEC_ID";

    // Synthetic access
    static final Object sLock = new Object();

    // Synthetic access
    WorkManagerImpl mWorkManagerImpl;
    // Synthetic access
    WorkConstraintsTracker mTracker;
    // Synthetic access
    List<String> mWorkSpecIds;
    // Synthetic access
    Map<String, WorkSpec> mWorkSpecsMap;
    // WorkSpecs that we are tracking constraints for
    List<WorkSpec> mTrackingWorkSpecs;

    @Nullable
    private SystemForegroundCallbacks mCallback;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private NotificationChannel mChannel;

    SystemForegroundDispatcher(@NonNull Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mWorkManagerImpl = WorkManagerImpl.getInstance(context);
        mTracker = new WorkConstraintsTracker(context, this);
        mWorkSpecIds = new ArrayList<>();
        mWorkSpecsMap = new HashMap<>();
        mTrackingWorkSpecs = new ArrayList<>();
        mWorkManagerImpl.getProcessor().addExecutionListener(this);
    }

    void onStartCommand(Intent intent) {
        if (!intent.hasExtra(KEY_WORKSPEC_ID)) {
            return;
        }

        final String workSpecId = intent.getStringExtra(KEY_WORKSPEC_ID);
        final String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            Logger.get().debug(TAG, String.format("Handling %s (%s)", action, workSpecId));
            // Technically we should stop tracking this WorkSpec
            // That will happen automatically once the Worker is stopped and onExecuted() is
            // complete
            mWorkManagerImpl.stopWork(workSpecId);
        } else if (ACTION_EXECUTE.equals(action)) {
            Logger.get().debug(TAG, String.format("Handling %s (%s)", action, workSpecId));
            synchronized (sLock) {
                mWorkSpecIds.add(workSpecId);
            }
            mWorkManagerImpl.getWorkTaskExecutor().executeOnBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    WorkSpecDao workSpecDao = mWorkManagerImpl.getWorkDatabase().workSpecDao();
                    WorkSpec workSpec = workSpecDao.getWorkSpec(workSpecId);
                    synchronized (sLock) {
                        mWorkSpecsMap.put(workSpecId, workSpec);
                        if (workSpec.hasConstraints()) {
                            Logger.get().debug(TAG, String.format("Tracking %s", workSpec));
                            mTrackingWorkSpecs.add(workSpec);
                            mTracker.replace(mTrackingWorkSpecs);
                        } else {
                            Logger.get().debug(TAG,
                                    String.format("No constraints for %s", workSpec));
                            onAllConstraintsMet(Collections.singletonList(workSpecId));
                        }
                    }
                }
            });
        } else {
            Logger.get().warning(TAG, String.format("Ignoring intent %s", intent));
        }
    }

    void setCallback(@NonNull SystemForegroundCallbacks callback) {
        if (mCallback != null) {
            Logger.get().error(TAG, "A callback for SystemForegroundDispatcher already exists.");
            return;
        }
        mCallback = callback;
    }

    void onDestroy() {
        mCallback = null;
        mWorkManagerImpl.getProcessor().removeExecutionListener(this);
        mTracker.reset();
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Logger.get().debug(TAG, String.format("Completed executing %s", workSpecId));
        synchronized (sLock) {
            mWorkSpecIds.remove(workSpecId);
            WorkSpec workSpec = mWorkSpecsMap.remove(workSpecId);
            // Update constraint trackers
            mTrackingWorkSpecs.remove(workSpec);
            mTracker.replace(mTrackingWorkSpecs);
            if (!mWorkManagerImpl.getProcessor().hasWork() && mCallback != null) {
                mCallback.onCompleted();
            }
        }
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        Logger.get().debug(TAG, String.format("onConstraintsMet (%s)", workSpecIds));
        if (!workSpecIds.isEmpty()) {
            for (String id : workSpecIds) {
                if (!mWorkManagerImpl.getProcessor().isEnqueued(id)) {
                    Logger.get().debug(TAG, String.format("Starting work for %s", id));
                    mWorkManagerImpl.startWork(id);
                }
            }
        }
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        Logger.get().debug(TAG, String.format("onConstraintsMet (%s)", workSpecIds));
        if (!workSpecIds.isEmpty()) {
            for (String id : workSpecIds) {
                Logger.get().debug(TAG, String.format("Stopping work for %s", id));
                mWorkManagerImpl.stopWork(id);
            }
        }
    }

    static Intent createExecuteIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_EXECUTE);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    static Intent createStopIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_STOP);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    /**
     * Used to notify that all pending commands are now completed.
     */
    interface SystemForegroundCallbacks {
        void onCompleted();
    }
}
