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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.worker.StopAwareWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class CancellationTest {

    private Context mContext;
    private Configuration mConfiguration;
    private WorkDatabase mDatabase;
    private WorkManagerImpl mWorkManager;

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
        mContext = InstrumentationRegistry.getTargetContext();
        mConfiguration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .build();
        mWorkManager = new WorkManagerImpl(
                mContext,
                mConfiguration,
                new InstantWorkTaskExecutor());
        WorkManagerImpl.setDelegate(mWorkManager);
        mDatabase = mWorkManager.getWorkDatabase();
        Logger.setMinimumLoggingLevel(Log.DEBUG);
    }

    @Test
    @SmallTest
    public void testCancelledWorkState() throws InterruptedException, ExecutionException {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CancellationWorker.class)
                .build();

        mWorkManager.enqueue(request);
        // Wait for cancellation to be done.
        mWorkManager.cancelWorkById(request.getId()).getResult().get();
    }

    public static class CancellationWorker extends StopAwareWorker {

        private static final String TAG = "CancellationWorker";

        public CancellationWorker(Context context, WorkerParameters workerParameters) {
            super(context, workerParameters);
        }

        @Override
        public void onStopped() {
            super.onStopped();
            try {
                WorkInfo workInfo = WorkManager.getInstance().getWorkInfoById(getId()).get();
                Log.d(TAG, String.format("WorkInfo %s", workInfo));
                assertThat(workInfo.getState(), is(WorkInfo.State.CANCELLED));
            } catch (Throwable throwable) {
                fail(String.format("Asserting failed due to %s", throwable.getMessage()));
            }
        }
    }
}
