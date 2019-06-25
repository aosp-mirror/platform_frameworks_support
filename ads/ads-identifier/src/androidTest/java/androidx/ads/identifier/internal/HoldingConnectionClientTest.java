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

package androidx.ads.identifier.internal;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;
import static androidx.ads.identifier.MockAdvertisingIdService.TESTING_AD_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Pair;

import androidx.ads.identifier.MockAdvertisingIdService;
import androidx.ads.identifier.MockAdvertisingIdThrowsNpeService;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class HoldingConnectionClientTest {
    private static final String MOCK_SERVICE_PACKAGE_NAME = "androidx.ads.identifier.test";
    private static final String MOCK_SERVICE_NAME = MockAdvertisingIdService.class.getName();
    private static final String MOCK_THROWS_NPE_SERVICE_NAME =
            MockAdvertisingIdThrowsNpeService.class.getName();

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PackageManager mMockPackageManager;

    private Context mContext;

    private HoldingConnectionClient mClient;

    @Before
    public void setUp() {
        MockHoldingConnectionClient.sGetServiceConnectionThrowException = false;

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
                createResolveInfo(MOCK_SERVICE_PACKAGE_NAME, MOCK_SERVICE_NAME)));

        mClient = new HoldingConnectionClient(mContext);
    }

    @After
    public void tearDown() {
        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        serviceIntent.setClassName(MOCK_SERVICE_PACKAGE_NAME, MOCK_SERVICE_NAME);
        mContext.stopService(serviceIntent);

        Intent npeServiceIntent = new Intent(GET_AD_ID_ACTION);
        npeServiceIntent.setClassName(MOCK_SERVICE_PACKAGE_NAME, MOCK_THROWS_NPE_SERVICE_NAME);
        mContext.stopService(npeServiceIntent);
    }

    private void mockQueryIntentServices(List<ResolveInfo> resolveInfos) {
        when(mMockPackageManager.queryIntentServices(
                argThat(intent -> intent != null && GET_AD_ID_ACTION.equals(intent.getAction())),
                eq(0))).thenReturn(resolveInfos);
    }

    @Test
    public void notConnectedAtBeginning() {
        assertThat(mClient.isConnected()).isFalse();
    }

    @Test
    public void getServiceWithIntent() throws Exception {
        Pair<IAdvertisingIdService, Intent> serviceWithIntent = mClient.getServiceWithIntent();

        assertThat(serviceWithIntent.first.getId()).isEqualTo(TESTING_AD_ID);
        assertThat(serviceWithIntent.second.getComponent())
                .isEqualTo(new ComponentName(MOCK_SERVICE_PACKAGE_NAME, MOCK_SERVICE_NAME));
    }

    @Test
    public void scheduleAutoDisconnect() throws Exception {
        mClient.getServiceWithIntent();
        mClient.scheduleAutoDisconnect();
        assertThat(mClient.isConnected()).isTrue();

        Thread.sleep(11000);

        assertThat(mClient.isConnected()).isFalse();
    }

    @Test
    public void finish() throws Exception {
        mClient.getServiceWithIntent();
        mClient.finish();

        assertThat(mClient.isConnected()).isFalse();
    }

    @Test(expected = TimeoutException.class)
    public void getServiceWithIntent_connectionTimeout() throws Exception {
        new MockHoldingConnectionClient(mContext).getServiceWithIntent();
    }

    @Test(expected = IOException.class)
    public void getServiceWithIntent_connectionFailed() throws Exception {
        MockHoldingConnectionClient.sGetServiceConnectionThrowException = true;

        new MockHoldingConnectionClient(mContext).getServiceWithIntent();
    }

    private static class MockHoldingConnectionClient extends HoldingConnectionClient {

        static boolean sGetServiceConnectionThrowException = false;

        MockHoldingConnectionClient(Context context) {
            super(context);
        }

        @Override
        BlockingServiceConnection getServiceConnection() throws IOException {
            if (sGetServiceConnectionThrowException) {
                throw new IOException();
            }

            // This connection does not bind to any service, so it always timeout.
            return new BlockingServiceConnection();
        }
    }

    private ResolveInfo createResolveInfo(String packageName, String name) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.serviceInfo.name = name;
        return resolveInfo;
    }
}
