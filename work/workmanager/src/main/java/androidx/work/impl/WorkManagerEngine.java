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

package androidx.work.impl;

import android.annotation.TargetApi;
import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.R;
import androidx.work.State;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;
import androidx.work.WorkerParameters;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.ForceStopRunnable;
import androidx.work.impl.utils.LiveDataUtils;
import androidx.work.impl.utils.Preferences;
import androidx.work.impl.utils.PruneWorkRunnable;
import androidx.work.impl.utils.StartWorkRunnable;
import androidx.work.impl.utils.StatusRunnable;
import androidx.work.impl.utils.StopWorkRunnable;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerEngine {

    public static final int MAX_PRE_JOB_SCHEDULER_API_LEVEL = 22;
    public static final int MIN_JOB_SCHEDULER_API_LEVEL = 23;

    private Context mContext;
    private Configuration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private TaskExecutor mWorkTaskExecutor;
    private List<Scheduler> mSchedulers;
    private Processor mProcessor;
    private Preferences mPreferences;
    private boolean mForceStopRunnableCompleted;
    private BroadcastReceiver.PendingResult mRescheduleReceiverResult;

    private static WorkManagerEngine sDelegatedInstance = null;
    private static WorkManagerEngine sDefaultInstance = null;
    private static final Object sLock = new Object();


    /**
     * @param delegate The delegate for {@link WorkManagerEngine} for testing; {@code null} to use
     *                 the default instance
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void setDelegate(WorkManagerEngine delegate) {
        synchronized (sLock) {
            sDelegatedInstance = delegate;
        }
    }

    /**
     * Retrieves the singleton instance of {@link WorkManagerEngine}.
     *
     * @return The singleton instance of {@link WorkManagerEngine}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @Nullable WorkManagerEngine getInstance() {
        synchronized (sLock) {
            if (sDelegatedInstance != null) {
                return sDelegatedInstance;
            }

            return sDefaultInstance;
        }
    }

    /**
     * Initializes the singleton instance of {@link WorkManagerEngine}.
     *
     * @param context A {@link Context} object for configuration purposes. Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @param configuration The {@link Configuration} for used to set up WorkManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void initialize(@NonNull Context context, @NonNull Configuration configuration) {
        synchronized (sLock) {
            if (sDelegatedInstance == null) {
                context = context.getApplicationContext();
                if (sDefaultInstance == null) {
                    sDefaultInstance = new WorkManagerEngine(
                            context,
                            configuration,
                            new WorkManagerTaskExecutor());
                }
                sDelegatedInstance = sDefaultInstance;
            }
        }
    }

    /**
     * Create an instance of {@link WorkManagerEngine}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerEngine(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor) {
        this(context,
                configuration,
                workTaskExecutor,
                context.getResources().getBoolean(R.bool.workmanager_test_configuration));
    }

    /**
     * Create an instance of {@link WorkManagerEngine}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param useTestDatabase {@code true} If using an in-memory test database
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerEngine(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            boolean useTestDatabase) {

        Context applicationContext = context.getApplicationContext();
        WorkDatabase database = WorkDatabase.create(applicationContext, useTestDatabase);
        List<Scheduler> schedulers = createSchedulers(applicationContext, this);
        Processor processor = new Processor(
                context,
                configuration,
                workTaskExecutor,
                database,
                schedulers);
        internalInit(context, configuration, workTaskExecutor, database, schedulers, processor);
    }

    /**
     * Create an instance of {@link WorkManagerEngine}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param workDatabase The {@link WorkDatabase} instance
     * @param processor The {@link Processor} instance
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerEngine(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor) {
        internalInit(context, configuration, workTaskExecutor, workDatabase, schedulers, processor);
    }

    /**
     * @return The application {@link Context} associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Context getApplicationContext() {
        return mContext;
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Configuration} instance associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * @return The {@link Scheduler}s associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull List<Scheduler> getSchedulers() {
        return mSchedulers;
    }

    /**
     * @return The {@link Processor} used to process background work.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Processor getProcessor() {
        return mProcessor;
    }

    /**
     * @return the {@link TaskExecutor} used by the instance of {@link WorkManager}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull TaskExecutor getWorkTaskExecutor() {
        return mWorkTaskExecutor;
    }

    /**
     * @return the {@link Preferences} used by the instance of {@link WorkManager}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Preferences getPreferences() {
        return mPreferences;
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workRequests One or more {@link WorkRequest} to enqueue
     */
    public @NonNull ListenableFuture<Void> enqueue(
            @NonNull List<? extends WorkRequest> workRequests) {

        // This error is not being propagated as part of the ListenableFuture, as we want the
        // app to crash during development. Having no workRequests is always a developer error.
        if (workRequests.isEmpty()) {
            throw new IllegalArgumentException(
                    "enqueue needs at least one WorkRequest.");
        }
        return new WorkContinuationImpl(this, workRequests).enqueue();
    }

    /**
     * Begins a chain with one or more {@link OneTimeWorkRequest}s, which can be enqueued together
     * in the future using {@link WorkContinuation#enqueue()}.
     *
     * @param work One or more {@link OneTimeWorkRequest} to start a chain of work
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}
     */
    public @NonNull WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginWith needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, work);
    }

    /**
     * This method allows you to begin unique chains of work for situations where you only want one
     * chain with a given name to be active at a time.  For example, you may only want one sync
     * operation to be active.  If there is one pending, you can choose to let it run or replace it
     * with your new work.
     *
     * The {@code uniqueWorkName} uniquely identifies this set of work.
     *
     * If this method determines that new work should be enqueued and run, all records of previous
     * work with {@code uniqueWorkName} will be pruned.  If this method determines that new work
     * should NOT be run, then the entire chain will be considered a no-op.
     *
     * @param uniqueWorkName A unique name which for this chain of work
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}; see below for more information
     * @param work One or more {@link OneTimeWorkRequest} to enqueue. {@code REPLACE} ensures that
     *             if there is pending work labelled with {@code uniqueWorkName}, it will be
     *             cancelled and the new work will run. {@code KEEP} will run the new sequence of
     *             work only if there is no pending work labelled with {@code uniqueWorkName}.
     *             {@code APPEND} will create a new sequence of work if there is no
     *             existing work with {@code uniqueWorkName}; otherwise, {@code work} will be added
     *             as a child of all leaf nodes labelled with {@code uniqueWorkName}.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public @NonNull WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginUniqueWork needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, uniqueWorkName, existingWorkPolicy, work);
    }


    /**
     * This method allows you to enqueue {@code work} requests to a uniquely-named
     * {@link WorkContinuation}, where only one continuation of a particular name can be active at
     * a time. For example, you may only want one sync operation to be active. If there is one
     * pending, you can choose to let it run or replace it with your new work.
     *
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this {@link WorkContinuation}.
     * </p>
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingWorkPolicy An {@link ExistingWorkPolicy}
     * @param work {@link OneTimeWorkRequest}s to enqueue. {@code REPLACE} ensures
     *                     that if there is pending work labelled with {@code uniqueWorkName}, it
     *                     will be cancelled and the new work will run. {@code KEEP} will run the
     *                     new OneTimeWorkRequests only if there is no pending work labelled with
     *                     {@code uniqueWorkName}. {@code APPEND} will append the
     *                     OneTimeWorkRequests as leaf nodes labelled with {@code uniqueWorkName}.
     */
    public @NonNull ListenableFuture<Void> enqueueUniqueWork(@NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return new WorkContinuationImpl(this, uniqueWorkName, existingWorkPolicy, work).enqueue();
    }

    /**
     * This method allows you to enqueue a uniquely-named {@link PeriodicWorkRequest}, where only
     * one PeriodicWorkRequest of a particular name can be active at a time.  For example, you may
     * only want one sync operation to be active.  If there is one pending, you can choose to let it
     * run or replace it with your new work.
     *
     * <p>
     * The {@code uniqueWorkName} uniquely identifies this PeriodicWorkRequest.
     * </p>
     *
     * @param uniqueWorkName A unique name which for this operation
     * @param existingPeriodicWorkPolicy An {@link ExistingPeriodicWorkPolicy}
     * @param periodicWork A {@link PeriodicWorkRequest} to enqueue. {@code REPLACE} ensures that if
     *                     there is pending work labelled with {@code uniqueWorkName}, it will be
     *                     cancelled and the new work will run. {@code KEEP} will run the new
     *                     PeriodicWorkRequest only if there is no pending work labelled with
     *                     {@code uniqueWorkName}.
     */
    public @NonNull ListenableFuture<Void> enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {
        return createWorkContinuationForUniquePeriodicWork(
                uniqueWorkName,
                existingPeriodicWorkPolicy,
                periodicWork)
                .enqueue();
    }

    private WorkContinuationImpl createWorkContinuationForUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {
        ExistingWorkPolicy existingWorkPolicy;
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.KEEP) {
            existingWorkPolicy = ExistingWorkPolicy.KEEP;
        } else {
            existingWorkPolicy = ExistingWorkPolicy.REPLACE;
        }
        return new WorkContinuationImpl(
                this,
                uniqueWorkName,
                existingWorkPolicy,
                Collections.singletonList(periodicWork));
    }

    /**
     * Cancels work with the given id if it isn't finished.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param id The id of the work
     */
    public @NonNull ListenableFuture<Void> cancelWorkById(@NonNull UUID id) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forId(id, this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    /**
     * Cancels all unfinished work with the given tag.  Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param tag The tag used to identify the work
     */
    public @NonNull ListenableFuture<Void> cancelAllWorkByTag(@NonNull final String tag) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forTag(tag, this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    /**
     * Cancels all unfinished work in the work chain with the given name.  Note that cancellation is
     * a best-effort policy and work that is already executing may continue to run.
     *
     * @param uniqueWorkName The unique name used to identify the chain of wor
     */
    @NonNull
    public ListenableFuture<Void> cancelUniqueWork(@NonNull String uniqueWorkName) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forName(uniqueWorkName, this, true);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    /**
     * Cancels all unfinished work.  <b>Use this method with extreme caution!</b>  By invoking it,
     * you will potentially affect other modules or libraries in your codebase.  It is strongly
     * recommended that you use one of the other cancellation methods at your disposal.
     */
    public @NonNull ListenableFuture<Void> cancelAllWork() {
        CancelWorkRunnable runnable = CancelWorkRunnable.forAll(this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    /**
     * Gets a {@link LiveData} of the last time all work was cancelled.  This method is intended for
     * use by library and module developers who have dependent data in their own repository that
     * must be updated or deleted in case someone cancels their work without their prior knowledge.
     *
     * @return A {@link LiveData} of the timestamp in milliseconds when method that cancelled all
     *         work was last invoked; this timestamp may be {@code 0L} if this never occurred.
     */
    public @NonNull LiveData<Long> getLastCancelAllTimeMillisLiveData() {
        return mPreferences.getLastCancelAllTimeMillisLiveData();
    }

    /**
     * Gets a {@link ListenableFuture} of the last time all work was cancelled.  This method is
     * intended for use by library and module developers who have dependent data in their own
     * repository that must be updated or deleted in case someone cancels their work without
     * their prior knowledge.
     *
     * @return A {@link ListenableFuture} of the timestamp in milliseconds when method that
     * cancelled all work was last invoked; this timestamp may be {@code 0L} if this never occurred
     */
    public @NonNull ListenableFuture<Long> getLastCancelAllTimeMillis() {
        final SettableFuture<Long> future = SettableFuture.create();
        // Avoiding synthetic accessors.
        final Preferences preferences = mPreferences;
        mWorkTaskExecutor.executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(preferences.getLastCancelAllTimeMillis());
                } catch (Throwable throwable) {
                    future.setException(throwable);
                }
            }
        });
        return future;
    }

    /**
     * Prunes all eligible finished work from the internal database.  Eligible work must be finished
     * ({@link State#SUCCEEDED}, {@link State#FAILED}, or {@link State#CANCELLED}), with zero
     * unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkStatus} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     */
    public @NonNull ListenableFuture<Void> pruneWork() {
        PruneWorkRunnable runnable = new PruneWorkRunnable(this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link LiveData} of the {@link WorkStatus} associated with {@code id}; note that
     *         this {@link WorkStatus} may be {@code null} if {@code id} is not known to
     *         WorkManager.
     */
    public @NonNull LiveData<WorkStatus> getStatusByIdLiveData(@NonNull UUID id) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(Collections.singletonList(id.toString()));
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData,
                new Function<List<WorkSpec.WorkStatusPojo>, WorkStatus>() {
                    @Override
                    public WorkStatus apply(List<WorkSpec.WorkStatusPojo> input) {
                        WorkStatus workStatus = null;
                        if (input != null && input.size() > 0) {
                            workStatus = input.get(0).toWorkStatus();
                        }
                        return workStatus;
                    }
                },
                mWorkTaskExecutor);
    }

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkStatus} for a given work id.
     *
     * @param id The id of the work
     * @return A {@link ListenableFuture} of the {@link WorkStatus} associated with {@code id};
     * note that this {@link WorkStatus} may be {@code null} if {@code id} is not known to
     * WorkManager
     */
    public @NonNull ListenableFuture<WorkStatus> getStatusById(@NonNull UUID id) {
        StatusRunnable<WorkStatus> runnable = StatusRunnable.forUUID(this, id);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link LiveData} list of {@link WorkStatus} for work tagged with {@code tag}
     */
    public @NonNull LiveData<List<WorkStatus>> getStatusesByTagLiveData(@NonNull String tag) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForTag(tag);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_STATUS_MAPPER,
                mWorkTaskExecutor);
    }

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkStatus} for all work for a given tag.
     *
     * @param tag The tag of the work
     * @return A {@link ListenableFuture} list of {@link WorkStatus} for work tagged with
     * {@code tag}
     */
    public @NonNull ListenableFuture<List<WorkStatus>> getStatusesByTag(@NonNull String tag) {
        StatusRunnable<List<WorkStatus>> runnable = StatusRunnable.forTag(this, tag);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    /**
     * Gets a {@link LiveData} of the {@link WorkStatus} for all work in a work chain with a given
     * unique name.
     *
     * @param name The unique name used to identify the chain of work
     * @return A {@link LiveData} of the {@link WorkStatus} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public @NonNull LiveData<List<WorkStatus>> getStatusesForUniqueWorkLiveData(
            @NonNull String name) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForName(name);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_STATUS_MAPPER,
                mWorkTaskExecutor);
    }

    /**
     * Gets a {@link ListenableFuture} of the {@link WorkStatus} for all work in a work chain
     * with a given unique name.
     *
     * @param name The unique name used to identify the chain of work
     * @return A {@link ListenableFuture} of the {@link WorkStatus} for work in the chain named
     *         {@code uniqueWorkName}
     */
    public @NonNull ListenableFuture<List<WorkStatus>> getStatusesForUniqueWork(
            @NonNull String name) {
        StatusRunnable<List<WorkStatus>> runnable =
                StatusRunnable.forUniqueWork(this, name);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    LiveData<List<WorkStatus>> getStatusesById(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(workSpecIds);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_STATUS_MAPPER,
                mWorkTaskExecutor);
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(String workSpecId) {
        startWork(workSpecId, null);
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} associated with this work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(String workSpecId, WorkerParameters.RuntimeExtras runtimeExtras) {
        mWorkTaskExecutor
                .executeOnBackgroundThread(
                        new StartWorkRunnable(this, workSpecId, runtimeExtras));
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to stop
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void stopWork(String workSpecId) {
        mWorkTaskExecutor.executeOnBackgroundThread(new StopWorkRunnable(this, workSpecId));
    }

    /**
     * Reschedules all the eligible work. Useful for cases like, app was force stopped or
     * BOOT_COMPLETED, TIMEZONE_CHANGED and TIME_SET for AlarmManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @TargetApi(23) // https://issuetracker.google.com/issues/110576968
    public void rescheduleEligibleWork() {
        // TODO (rahulrav@) Make every scheduler do its own cancelAll().
        if (Build.VERSION.SDK_INT >= WorkManagerEngine.MIN_JOB_SCHEDULER_API_LEVEL) {
            SystemJobScheduler.jobSchedulerCancelAll(getApplicationContext());
        }

        // Reset scheduled state.
        getWorkDatabase().workSpecDao().resetScheduledState();

        // Delegate to the WorkManager's schedulers.
        // Using getters here so we can use from a mocked instance
        // of WorkManagerImpl.
        Schedulers.schedule(getConfiguration(), getWorkDatabase(), getSchedulers());
    }

    /**
     * A way for {@link ForceStopRunnable} to tell {@link WorkManagerEngine} that it has completed.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onForceStopRunnableCompleted() {
        synchronized (sLock) {
            mForceStopRunnableCompleted = true;
            if (mRescheduleReceiverResult != null) {
                mRescheduleReceiverResult.finish();
                mRescheduleReceiverResult = null;
            }
        }
    }

    /**
     * This method is invoked by
     * {@link androidx.work.impl.background.systemalarm.RescheduleReceiver}
     * after a call to {@link BroadcastReceiver#goAsync()}. Once {@link ForceStopRunnable} is done,
     * we can safely call {@link BroadcastReceiver.PendingResult#finish()}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setReschedulePendingResult(
            @NonNull BroadcastReceiver.PendingResult rescheduleReceiverResult) {
        synchronized (sLock) {
            mRescheduleReceiverResult = rescheduleReceiverResult;
            if (mForceStopRunnableCompleted) {
                mRescheduleReceiverResult.finish();
                mRescheduleReceiverResult = null;
            }
        }
    }

    /**
     * Initializes an instance of {@link WorkManagerEngine}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration.
     * @param workDatabase The {@link WorkDatabase} instance.
     * @param schedulers The {@link List} of {@link Scheduler}s to use.
     * @param processor The {@link Processor} instance.
     */
    private void internalInit(@NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor) {

        context = context.getApplicationContext();
        mContext = context;
        mConfiguration = configuration;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mSchedulers = schedulers;
        mProcessor = processor;
        mPreferences = new Preferences(mContext);
        mForceStopRunnableCompleted = false;

        Logger.setMinimumLoggingLevel(mConfiguration.getMinimumLoggingLevel());

        // Checks for app force stops.
        mWorkTaskExecutor.executeOnBackgroundThread(new ForceStopRunnable(context, this));
    }

    private static List<Scheduler> createSchedulers(
            Context context, WorkManagerEngine workManager) {
        return Arrays.asList(
                Schedulers.createBestAvailableBackgroundScheduler(context, workManager),
                new GreedyScheduler(context, workManager));
    }
}
