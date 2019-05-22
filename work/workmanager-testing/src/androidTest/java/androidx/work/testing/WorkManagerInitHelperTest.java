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

package androidx.work.testing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.impl.WorkManagerImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class WorkManagerInitHelperTest {

    private Context mContext;
    private Executor mExecutor;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        // Clear delegates after every single test.
        WorkManagerImpl.setDelegate(null);
    }

    @Test
    public void testWorkManagerIsInitialized() {
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext);
        assertThat(WorkManager.getInstance(), is(notNullValue()));
=======
        Configuration configuration = new Configuration.Builder()
                .setExecutor(mExecutor)
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        WorkManagerImpl workManager = (WorkManagerImpl) WorkManager.getInstance(mContext);
        assertThat(workManager, is(notNullValue()));
        assertThat(workManager.getWorkTaskExecutor().getBackgroundExecutor(), is(mExecutor));
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
    }
}
