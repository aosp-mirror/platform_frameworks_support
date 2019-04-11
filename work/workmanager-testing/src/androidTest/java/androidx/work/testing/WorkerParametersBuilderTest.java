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
import android.net.Uri;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.Data;
import androidx.work.ListenableWorker.Result;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.testing.workers.TestExpectedParametersWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerParametersBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext);
    }

    @After
    public void tearDown() {
        // Clear delegates after every single test.
        WorkManagerImpl.setDelegate(null);
    }

    @Test
    public void testWorkerParameters() throws InterruptedException, ExecutionException {
        TestDriver driver = WorkManagerTestInitHelper.getTestDriver(mContext);
        if (driver != null) {
            WorkerParametersBuilder builder = driver.newWorkerParametersBuilder();

            Data inputData = new Data.Builder()
                    .put("Key", "Value")
                    .build();

            List<Uri> contentUris = Collections.singletonList(Uri.parse("abc://"));
            List<String> authorities = Collections.singletonList("abc");

            builder.setId(UUID.randomUUID())
                    .setInputData(inputData)
                    .setRunAttemptCount(2);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setTriggeredContentUris(contentUris);
                builder.setTriggeredContentAuthorities(authorities);
            }

            WorkerParameters parameters = builder.build();
            TestExpectedParametersWorker worker =
                    driver.newWorker(TestExpectedParametersWorker.class, parameters);

            worker.setExpected(parameters);

            Result result = worker.startWork().get();
            assertThat(result, is(Result.success()));
        }
    }

}
