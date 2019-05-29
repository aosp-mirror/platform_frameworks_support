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

package androidx.ads.identifier;

import static androidx.ads.identifier.AdvertisingIdClient.GET_AD_ID_ACTION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import androidx.ads.identifier.internal.BlockingServiceClient;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvertisingIdClientTest {
    private static final String TESTING_AD_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private static final String MOCK_SERVICE_PACKAGE_NAME = "androidx.ads.identifier.test";
    private static final String MOCK_SERVICE_NAME = MockAdvertisingIdService.class.getName();

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PackageManager mMockPackageManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockAdvertisingIdService.sGetIdResultType =
                MockAdvertisingIdService.ResultType.NORMAL;
        MockAdvertisingIdClient.sGetServiceConnectionThrowException = false;

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
    }

    @After
    public void tearDown() {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        intent.setClassName(MOCK_SERVICE_PACKAGE_NAME, MOCK_SERVICE_NAME);
        mContext.stopService(intent);
    }

    private void mockQueryIntentServices(List<ResolveInfo> resolveInfos) {
        when(mMockPackageManager.queryIntentServices(argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Intent intent) {
                return intent != null && GET_AD_ID_ACTION.equals(intent.getAction());
            }
        }), eq(0))).thenReturn(resolveInfos);
    }

    @Test
    public void getAdvertisingIdInfo() throws Exception {
        MockAdvertisingIdService.sId = TESTING_AD_ID;
        MockAdvertisingIdService.sLimitAdTrackingEnabled = true;

        AdvertisingIdInfo info = AdvertisingIdClient.getAdvertisingIdInfo(mContext);

        assertThat(info).isEqualTo(AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(MOCK_SERVICE_PACKAGE_NAME)
                .build());
    }

    @Test
    public void getAdvertisingIdInfo_idNotInUuidFormat() throws Exception {
        MockAdvertisingIdService.sId = "abc";
        MockAdvertisingIdService.sLimitAdTrackingEnabled = true;

        AdvertisingIdInfo info = AdvertisingIdClient.getAdvertisingIdInfo(mContext);

        assertThat(info).isEqualTo(AdvertisingIdInfo.builder()
                .setId("90015098-3cd2-3fb0-9696-3f7d28e17f72") // UUID version 3 of "abc"
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(MOCK_SERVICE_PACKAGE_NAME)
                .build());
    }

    @Test
    public void getAdvertisingIdInfo_runOnMainThread() throws Exception {
        MockAdvertisingIdService.sId = TESTING_AD_ID;
        MockAdvertisingIdService.sLimitAdTrackingEnabled = true;

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    AdvertisingIdClient.getAdvertisingIdInfo(mContext);
                } catch (IllegalStateException expected) {
                    // Expected exception.
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                fail("IllegalStateException expected");
            }
        });
    }

    @Test(expected = AdvertisingIdNotAvailableException.class)
    public void getAdvertisingIdInfo_noProvider() throws Exception {
        mockQueryIntentServices(Collections.<ResolveInfo>emptyList());

        AdvertisingIdClient.getAdvertisingIdInfo(mContext);
    }

    @Test(expected = AdvertisingIdNotAvailableException.class)
    public void getAdvertisingIdInfo_serviceReturnsNull() throws Exception {
        MockAdvertisingIdService.sId = null;

        AdvertisingIdClient.getAdvertisingIdInfo(mContext);
    }

    @Test
    public void getAdvertisingIdInfo_serviceThrowsRemoteException() throws Exception {
        MockAdvertisingIdService.sGetIdResultType =
                MockAdvertisingIdService.ResultType.REMOTE_EXCEPTION;

        try {
            AdvertisingIdClient.getAdvertisingIdInfo(mContext);
        } catch (IOException expected) {
            assertThat(expected).hasMessageThat().isEqualTo("Remote exception");
            return;
        }
        fail("IOException expected.");
    }

    @Test(expected = RuntimeException.class)
    public void getAdvertisingIdInfo_serviceThrowsRuntimeException() throws Exception {
        MockAdvertisingIdService.sGetIdResultType =
                MockAdvertisingIdService.ResultType.RUNTIME_EXCEPTION;

        AdvertisingIdClient.getAdvertisingIdInfo(mContext);
    }

    @Test(expected = IOException.class)
    public void getAdvertisingIdInfo_serviceTimeout() throws Exception {
        MockAdvertisingIdService.sGetIdResultType =
                MockAdvertisingIdService.ResultType.TIMEOUT;

        AdvertisingIdClient.getAdvertisingIdInfo(mContext);
    }

    @Test
    public void getInfo_twoClients() throws Exception {
        MockAdvertisingIdService.sId = TESTING_AD_ID;
        MockAdvertisingIdService.sLimitAdTrackingEnabled = true;

        AdvertisingIdClient client1 = new AdvertisingIdClient(mContext);
        AdvertisingIdClient client2 = new AdvertisingIdClient(mContext);
        AdvertisingIdInfo info1 = client1.getInfo();
        AdvertisingIdInfo info2 = client2.getInfo();
        client1.finish();
        client2.finish();

        AdvertisingIdInfo expected = AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(MOCK_SERVICE_PACKAGE_NAME)
                .build();
        assertThat(info1).isEqualTo(expected);
        assertThat(info2).isEqualTo(expected);
    }

    @Test
    public void getAdvertisingIdInfo_connectionTimeout() throws Exception {
        try {
            MockAdvertisingIdClient.getAdvertisingIdInfo(mContext);
        } catch (IOException expected) {
            assertThat(expected).hasCauseThat().isInstanceOf(TimeoutException.class);
            return;
        }
        fail("IOException expected.");
    }

    @Test(expected = IOException.class)
    public void getAdvertisingIdInfo_connectionFailed() throws Exception {
        MockAdvertisingIdClient.sGetServiceConnectionThrowException = true;

        MockAdvertisingIdClient.getAdvertisingIdInfo(mContext);
    }

    private static class MockAdvertisingIdClient extends AdvertisingIdClient {
        static boolean sGetServiceConnectionThrowException = false;

        MockAdvertisingIdClient(Context context) {
            super(context);
        }

        @Override
        BlockingServiceClient<AdvertisingIdInfo> getServiceClient() throws IOException {
            if (sGetServiceConnectionThrowException) {
                throw new IOException();
            }

            return new BlockingServiceClient<AdvertisingIdInfo>(null, null, null) {
                @Override
                public void bindService(Context context, Intent intent) throws IOException {
                    // This connection does not bind to any service, so it always timeout.
                }
            };
        }

        public static AdvertisingIdInfo getAdvertisingIdInfo(Context context)
                throws IOException, IllegalStateException, AdvertisingIdNotAvailableException {
            MockAdvertisingIdClient client = new MockAdvertisingIdClient(context);
            try {
                return client.getInfo();
            } finally {
                client.finish();
            }
        }
    }

    @Test
    public void selectServiceByPriority() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("c.system.normal.1", true, false, 1, packageManager),
                createPackageInfo("a.user.noEffectHigh.1", false, true, 1, packageManager),
                createPackageInfo("b.user.normal.0", false, false, 0, packageManager),
                createPackageInfo("y.system.normal.0", true, false, 0, packageManager),
                createPackageInfo("x.system.normal.0", true, false, 0, packageManager),
                createPackageInfo("z.system.high.2", true, true, 2, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "z.system.high.2",
                "x.system.normal.0",
                "y.system.normal.0",
                "c.system.normal.1",
                "b.user.normal.0",
                "a.user.noEffectHigh.1"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_firstInstallTime() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("com.a", true, false, 2, packageManager),
                createPackageInfo("com.b", true, false, 9, packageManager),
                createPackageInfo("com.c", true, false, 7, packageManager),
                createPackageInfo("com.d", true, false, 10, packageManager),
                createPackageInfo("com.e", true, false, 0, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "com.e",
                "com.a",
                "com.c",
                "com.b",
                "com.d"
        ).inOrder();
    }

    @Test
    public void selectServiceByPriority_packageName() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = Lists.newArrayList(
                createPackageInfo("com.abc.id", true, false, 0, packageManager),
                createPackageInfo("com.abc", true, false, 0, packageManager),
                createPackageInfo("org.example", true, false, 0, packageManager),
                createPackageInfo("com.abcde", true, false, 0, packageManager),
                createPackageInfo("com.abcde_id", true, false, 0, packageManager));

        List<String> priorityList = getPriorityList(resolveInfos, packageManager);

        assertThat(priorityList).containsExactly(
                "com.abc",
                "com.abc.id",
                "com.abcde",
                "com.abcde_id",
                "org.example"
        ).inOrder();
    }

    private List<String> getPriorityList(List<ResolveInfo> resolveInfos,
            PackageManager packageManager) {
        List<String> result = new ArrayList<>();
        while (resolveInfos.size() > 0) {
            final ServiceInfo serviceInfo = AdvertisingIdClient.selectServiceByPriority(
                    resolveInfos,
                    packageManager);

            result.add(serviceInfo.packageName);

            resolveInfos.removeIf(new Predicate<ResolveInfo>() {
                @Override
                public boolean test(ResolveInfo resolveInfo) {
                    return resolveInfo.serviceInfo.packageName.equals(serviceInfo.packageName);
                }
            });
        }
        return result;
    }

    @Test
    public void selectServiceByPriority_inputNull() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        ServiceInfo serviceInfo =
                AdvertisingIdClient.selectServiceByPriority(null, packageManager);

        assertThat(serviceInfo).isNull();
    }

    @Test
    public void selectServiceByPriority_inputEmpty() throws Exception {
        PackageManager packageManager = mock(PackageManager.class);

        ServiceInfo serviceInfo =
                AdvertisingIdClient.selectServiceByPriority(Collections.<ResolveInfo>emptyList(),
                        packageManager);

        assertThat(serviceInfo).isNull();
    }

    private ResolveInfo createPackageInfo(String packageName, boolean isSystem,
            boolean requestHighPriority, long firstInstallTime, PackageManager packageManager)
            throws Exception {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        if (isSystem) {
            packageInfo.applicationInfo = new ApplicationInfo();
            packageInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        }
        if (requestHighPriority) {
            packageInfo.requestedPermissions =
                    new String[]{AdvertisingIdClient.HIGH_PRIORITY_PERMISSION};
        }
        packageInfo.firstInstallTime = firstInstallTime;

        mockGetPackageInfo(packageInfo, packageManager);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        return resolveInfo;
    }

    private void mockGetPackageInfo(PackageInfo packageInfo, PackageManager packageManager)
            throws Exception {
        when(packageManager.getPackageInfo(eq(packageInfo.packageName),
                eq(PackageManager.GET_PERMISSIONS))).thenReturn(packageInfo);
    }

    @Test
    public void isAdvertisingIdProvidersAvailable() {
        assertThat(AdvertisingIdClient.isAdvertisingIdProvidersAvailable(mContext)).isTrue();
    }

    @Test
    public void isAdvertisingIdProvidersAvailable_noProvider() {
        mockQueryIntentServices(Collections.<ResolveInfo>emptyList());

        assertThat(AdvertisingIdClient.isAdvertisingIdProvidersAvailable(mContext)).isFalse();
    }

    @Test
    public void isAdvertisingIdProvidersAvailable_twoProviders() {
        mockQueryIntentServices(Lists.newArrayList(
                createResolveInfo("com.a", "A"),
                createResolveInfo("com.b", "B")));

        assertThat(AdvertisingIdClient.isAdvertisingIdProvidersAvailable(mContext)).isTrue();
    }

    private ResolveInfo createResolveInfo(String packageName, String name) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.serviceInfo.name = name;
        return resolveInfo;
    }
}
