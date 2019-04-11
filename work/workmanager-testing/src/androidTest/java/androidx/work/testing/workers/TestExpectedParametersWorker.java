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

package androidx.work.testing.workers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A test {@link ListenableWorker} which checks input {@link WorkerParameters}.
 */
public class TestExpectedParametersWorker extends ListenableWorker {

    private WorkerParameters mExpected;

    public TestExpectedParametersWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    public void setExpected(@NonNull WorkerParameters expected) {
        mExpected = expected;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        if (mExpected != null) {
            assertThat(mExpected.getId(), is(getId()));
            assertThat(mExpected.getInputData(), is(getInputData()));
            assertThat(mExpected.getRunAttemptCount(), is(getRunAttemptCount()));
            assertThat(mExpected.getWorkerFactory(), is(getWorkerFactory()));
            assertThat(mExpected.getBackgroundExecutor(), is(getBackgroundExecutor()));
            assertThat(mExpected.getTaskExecutor(), is(getTaskExecutor()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertThat(mExpected.getTriggeredContentUris(), is(getTriggeredContentUris()));
                assertThat(
                        mExpected.getTriggeredContentAuthorities(),
                        is(getTriggeredContentAuthorities()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertThat(mExpected.getNetwork(), is(getNetwork()));
            }
        }

        final SettableFuture<Result> result = SettableFuture.create();

        getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                result.set(Result.success());
            }
        });

        return result;
    }
}
