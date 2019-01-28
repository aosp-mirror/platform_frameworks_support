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


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The {@link GcmTaskService} responsible for handling requests for executing
 * {@link androidx.work.WorkRequest}s.
 */
public class WorkManagerGcmService extends GcmTaskService {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WorkManagerGcmService");

    private static final long AWAIT_TIME_IN_MINUTES = 10;

    // Synthetic access
    WorkManagerImpl mWorkManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkManager = WorkManagerImpl.getInstance();
    }

    @Override
    @MainThread
    public void onInitializeTasks() {
        // Reschedule all eligible work, as all tasks have been cleared in GCMNetworkManager.
        // This typically happens after an upgrade.
        mWorkManager.getWorkTaskExecutor().executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                Logger.get().debug(TAG, "onInitializeTasks(): Rescheduling work");
                mWorkManager.rescheduleEligibleWork();
            }
        });
    }

    @Override
    public int onRunTask(@NonNull TaskParams taskParams) {
        Logger.get().debug(TAG, String.format("Handing task %s", taskParams));

        String tag = taskParams.getTag();
        if (tag == null || tag.isEmpty()) {
            // Bad request. No WorkSpec id.
            Logger.get().debug(TAG, "Bad request. No tag.");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        WorkSpecExecutionListener listener = new WorkSpecExecutionListener(tag);
        mWorkManager.getProcessor().addExecutionListener(listener);
        mWorkManager.startWork(tag);

        try {
            listener.getLatch().await(AWAIT_TIME_IN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Logger.get().debug(TAG, String.format("%s being rescheduled", tag), exception);
            return reschedule(tag);
        } finally {
            mWorkManager.getProcessor().removeExecutionListener(listener);
        }

        if (listener.needsReschedule()) {
            Logger.get().debug(TAG, String.format("%s being rescheduled", tag));
            return reschedule(tag);
        }

        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(tag);
        switch (workSpec.state) {
            case SUCCEEDED:
            case CANCELLED:
                Logger.get().debug(TAG, String.format("%s is being treated as successful", tag));
                return GcmNetworkManager.RESULT_SUCCESS;
            case FAILED:
                Logger.get().debug(TAG, String.format("%s is being treated as failed", tag));
                return GcmNetworkManager.RESULT_FAILURE;
            default:
                Logger.get().debug(TAG, "Rescheduling eligible work.");
                return reschedule(tag);
        }
    }

    private int reschedule(@NonNull String tag) {
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        try {
            workDatabase.beginTransaction();
            // Mark the workSpec as unscheduled. We are doing this explicitly here because
            // there are many cases where WorkerWrapper may not have had a chance to update this
            // flag. For e.g. this will happen if the Worker took longer than 10 minutes.
            workDatabase.workSpecDao()
                    .markWorkSpecScheduled(tag, WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
            // We reschedule on our own to apply our own backoff policy.
            Schedulers.schedule(
                    mWorkManager.getConfiguration(),
                    mWorkManager.getWorkDatabase(),
                    mWorkManager.getSchedulers());
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        Logger.get().debug(TAG, String.format("%s being treated as successful", tag));
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    static class WorkSpecExecutionListener implements ExecutionListener {
        private static final String TAG = Logger.tagWithPrefix("WorkSpecExecutionListener");
        private final String mTag;
        private final CountDownLatch mLatch;
        private boolean mNeedsReschedule;

        WorkSpecExecutionListener(@NonNull String tag) {
            mTag = tag;
            mLatch = new CountDownLatch(1);
            mNeedsReschedule = false;
        }

        boolean needsReschedule() {
            return mNeedsReschedule;
        }

        CountDownLatch getLatch() {
            return mLatch;
        }

        @Override
        public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
            if (!mTag.equals(workSpecId)) {
                Logger.get().warning(TAG,
                        String.format("Notified for %s, but was looking for %s", workSpecId, mTag));
            } else {
                mNeedsReschedule = needsReschedule;
                mLatch.countDown();
            }
        }
    }
}
