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

package androidx.ads.identifier.benchmark;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;

import androidx.ads.identifier.AdvertisingIdClient;
import androidx.ads.identifier.provider.internal.AdvertisingIdService;
import androidx.annotation.NonNull;
import androidx.benchmark.BenchmarkRule;
import androidx.benchmark.BenchmarkState;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AdvertisingIdBenchmark {

    private static final String SERVICE_PACKAGE_NAME = "androidx.ads.identifier.benchmark.test";
    private static final String SERVICE_NAME = AdvertisingIdService.class.getName();

    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PackageManager mMockPackageManager;

    private Context mContext;

    @Before
    public void setUp() {
        Context applicationContext = ApplicationProvider.getApplicationContext();

        mContext = new ContextWrapper(applicationContext) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public PackageManager getPackageManager() {
                return mMockPackageManager;
            }
        };

        mockQueryIntentServices(Lists.newArrayList(
                createResolveInfo(SERVICE_PACKAGE_NAME, SERVICE_NAME)));
    }

    @After
    public void tearDown() {
        stopAdvertisingIdService();
    }

    private void stopAdvertisingIdService() {
        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        serviceIntent.setClassName(SERVICE_PACKAGE_NAME, SERVICE_NAME);
        mContext.stopService(serviceIntent);
    }

    private void mockQueryIntentServices(List<ResolveInfo> resolveInfos) {
        when(mMockPackageManager.queryIntentServices(
                argThat(intent -> intent != null && GET_AD_ID_ACTION.equals(intent.getAction())),
                eq(0))).thenReturn(resolveInfos);
    }

    private ResolveInfo createResolveInfo(String packageName, String name) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.serviceInfo.name = name;
        return resolveInfo;
    }

    @Test
    public void getAdvertisingIdInfo_withConnectionCache() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.getAdvertisingIdInfo(mContext);
        }
    }

    @Test
    public void getAdvertisingIdInfo_withServiceStarted() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            AdvertisingIdClient.getAdvertisingIdInfo(mContext);
        }
    }

    @Test
    public void getAdvertisingIdInfo_worker() throws Exception {
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.cancelAllWork();
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            OneTimeWorkRequest work =
                    new OneTimeWorkRequest.Builder(GetAdInfoWorker.class).build();
            workManager.enqueue(work).getResult().get();
        }
    }

    /** Get the Advertising ID on a worker thread. */
    private class GetAdInfoWorker extends Worker {

        GetAdInfoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            } catch (Exception e) {
                return Result.failure();
            }
            return Result.success();
        }
    }

    @Test
    public void getAdvertisingIdInfo_asyncTask() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            new AdInfoAsyncTask().execute(mContext).get();
        }
    }
    private static class AdInfoAsyncTask extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            try {
                AdvertisingIdClient.getAdvertisingIdInfo(contexts[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }

    }

    @Test
    public void getAdvertisingIdInfo_thread() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            Thread thread = createAdInfoThread();
            thread.start();
            thread.join();
        }
    }

    private Thread createAdInfoThread() {
        return new Thread(() -> {
            try {
                AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void benchmark2() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Thread thread = createAdInfoThread();
                thread.start();
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.join();
            }
        }
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay1Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(1);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay10Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(10);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay100Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(100);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay1000Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(1000);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay9000Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(9000);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay10000Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(10000);
    }

    @Test
    public void getAdvertisingIdInfo_10threadWithDelay11000Millis() throws Exception {
        getAdvertisingIdInfo_10threadWithDelay(11000);
    }

    public void getAdvertisingIdInfo_10threadWithDelay(long millis) throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            AdvertisingIdClient.clearForTesting();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Thread thread = createAdInfoThread();
                thread.start();

                if (millis != 0) {
                    Thread.sleep(millis);
                }

                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.join(1000);
            }
        }
    }
}
