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

package androidx.work.testing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ListenableWorker;
import androidx.work.ListenableWorker.Result;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.testing.workers.TestListenableWorker;
import androidx.work.testing.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


@RunWith(AndroidJUnit4.class)
public class WorkerBuilderTest {

    private Context mContext;
    private Executor mExecutor;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mExecutor = new SynchronousExecutor();
    }

    @Test
    public void testListenableWorkerBuilder_buildsWorker()
            throws InterruptedException, ExecutionException {
        OneTimeWorkRequest request = OneTimeWorkRequest.from(TestWorker.class);
        ListenableWorker worker = ListenableWorkerBuilder.from(mContext, request).build();
        Result result = worker.startWork().get();
        assertThat(result, is(Result.success()));
    }

    @Test
    public void testWorkerBuilder_buildsWorker() {
        OneTimeWorkRequest request = OneTimeWorkRequest.from(TestWorker.class);
        Worker worker = WorkerBuilder.from(mContext, request, mExecutor).build();
        Result result = worker.doWork();
        assertThat(result, is(Result.success()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWorkerBuilder_invalidWorker() {
        OneTimeWorkRequest request = OneTimeWorkRequest.from(TestListenableWorker.class);
        WorkerBuilder.from(mContext, request, mExecutor).build();
    }

}
