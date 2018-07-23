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

package androidx.work.impl.workers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import androidx.work.Logger;
import androidx.work.NonBlockingWorker;
import androidx.work.Worker;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Extras;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkerWrapper;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;

import java.util.Collections;
import java.util.List;

/**
 * Is an implementation of a {@link Worker} that can delegate to a different {@link Worker}
 * when the constraints are met.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConstraintTrackingWorker extends NonBlockingWorker implements WorkConstraintsCallback {

    private static final String TAG = "ConstraintTrkngWrkr";

    /**
     * The {@code className} of the {@link Worker} to delegate to.
     */
    public static final String ARGUMENT_CLASS_NAME =
            "androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME";

    @Nullable
    private NonBlockingWorker mDelegate;
    private ExecutionListener mOriginalListener;
    private ConstraintTrackingListener mListener;

    private final Object mLock;
    // Marking this volatile as the delegated workers could switch threads.
    private volatile boolean mAreConstraintsUnmet;

    public ConstraintTrackingWorker() {
        mLock = new Object();
        mAreConstraintsUnmet = false;
        mListener = new ConstraintTrackingListener(this);
    }

    @Override
    public void onStartWork() {

        String className = getInputData().getString(ARGUMENT_CLASS_NAME);
        if (TextUtils.isEmpty(className)) {
            Logger.debug(TAG, "No worker to delegate to.");
            onWorkFinished(Result.FAILURE);
        }
        // Instantiate the delegated worker. Use the same workSpecId, and the same Data
        // as this Worker's Data are a superset of the delegate's Worker's Data.
        Extras extras = getExtras();
        Extras.RuntimeExtras runtimeExtras = extras.getRuntimeExtras();
        // Swap listeners so we can intercept completions.
        mOriginalListener = runtimeExtras.mExecutionListener;
        runtimeExtras.mExecutionListener = mListener;

        mDelegate = WorkerWrapper.workerFromClassName(
                getApplicationContext(),
                className,
                getId(),
                getExtras());

        if (mDelegate == null) {
            Logger.debug(TAG, "No worker to delegate to.");
            onWorkFinished(Result.FAILURE);
        }

        WorkDatabase workDatabase = getWorkDatabase();

        // We need to know what the real constraints are for the delegate.
        WorkSpec workSpec = workDatabase.workSpecDao().getWorkSpec(getId().toString());
        if (workSpec == null) {
            onWorkFinished(Result.FAILURE);
        }
        WorkConstraintsTracker workConstraintsTracker =
                new WorkConstraintsTracker(getApplicationContext(), this);

        // Start tracking
        workConstraintsTracker.replace(Collections.singletonList(workSpec));

        if (workConstraintsTracker.areAllConstraintsMet(getId().toString())) {
            Logger.debug(TAG, String.format("Constraints met for delegate %s", className));

            // Wrapping the call to mDelegate#doWork() in a try catch, because
            // changes in constraints can cause the worker to throw RuntimeExceptions, and
            // that should cause a retry.
            try {
                mDelegate.onStartWork();
            } catch (Throwable exception) {
                Logger.debug(TAG, String.format(
                        "Delegated worker %s threw a runtime exception.", className), exception);
                synchronized (mLock) {
                    if (mAreConstraintsUnmet) {
                        Logger.debug(TAG, "Constraints were unmet, Retrying.");
                        onWorkFinished(Result.RETRY);
                    } else {
                        onWorkFinished(Result.FAILURE);
                    }
                }
            }
        } else {
            Logger.debug(TAG, String.format(
                    "Constraints not met for delegate %s. Requesting retry.", className));
            onWorkFinished(Result.RETRY);
        }
    }

    /**
     * @return The instance of {@link WorkDatabase}.
     */
    @VisibleForTesting
    public WorkDatabase getWorkDatabase() {
        return WorkManagerImpl.getInstance().getWorkDatabase();
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        // WorkConstraintTracker notifies on the main thread. So we don't want to trampoline
        // between the background thread and the main thread in this case.
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        // If at any point, constraints are not met mark it so we can retry the work.
        Logger.debug(TAG, String.format("Constraints changed for %s", workSpecIds));
        synchronized (mLock) {
            mAreConstraintsUnmet = true;
        }
    }

    /**
     * An implementation of a {@link ExecutionListener} for the delegated
     * {@link NonBlockingWorker} used by the {@link ConstraintTrackingWorker}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public static class ConstraintTrackingListener implements ExecutionListener {
        private final ConstraintTrackingWorker mWorker;

        @VisibleForTesting
        public ConstraintTrackingListener(@NonNull ConstraintTrackingWorker worker) {
            mWorker = worker;
        }

        @Override
        public void onExecuted(
                @NonNull String workSpecId,
                boolean isSuccessful,
                boolean needsReschedule) {

            synchronized (mWorker.mLock) {
                if (mWorker.mAreConstraintsUnmet) {
                    mWorker.setResult(Result.RETRY);
                } else {
                    if (mWorker.mDelegate != null) {
                        mWorker.setOutputData(mWorker.mDelegate.getOutputData());
                        mWorker.setResult(mWorker.mDelegate.getResult());
                    }
                }
                // Notify original listener that we have a result.
                mWorker.mOriginalListener.onExecuted(
                        workSpecId, isSuccessful, needsReschedule);
            }
        }
    }
}
