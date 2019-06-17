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

package androidx.ads.identifier.provider.internal;

import static com.google.common.truth.Truth.assertThat;

import android.os.RemoteException;

import androidx.ads.identifier.provider.AdvertisingIdProvider;
import androidx.ads.identifier.provider.AdvertisingIdProviderManager;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvertisingIdAidlServiceImplTest {

    private static final String TESTING_AD_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Before
    public void setUp() {
        AdvertisingIdProviderManager.clearProviderCallable();
    }

    @Test
    public void getId() throws RemoteException {
        AdvertisingIdProviderManager.registerProviderCallable(
                () -> new MockAdvertisingIdProvider(TESTING_AD_ID, true));
        AdvertisingIdAidlServiceImpl advertisingIdService = new AdvertisingIdAidlServiceImpl();

        assertThat(advertisingIdService.getId()).isEqualTo(TESTING_AD_ID);
        assertThat(advertisingIdService.isLimitAdTrackingEnabled()).isEqualTo(true);
    }

    @Test(expected = IllegalStateException.class)
    public void getId_providerNotRegistered() throws RemoteException {
        new AdvertisingIdAidlServiceImpl();
    }

    @Test(expected = RuntimeException.class)
    public void getId_providerCallableThrowsException() throws RemoteException {
        AdvertisingIdProviderManager.registerProviderCallable(() -> {
            throw new Exception();
        });
        new AdvertisingIdAidlServiceImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getId_providerCallableReturnsNull() throws RemoteException {
        AdvertisingIdProviderManager.registerProviderCallable(() -> null);
        new AdvertisingIdAidlServiceImpl();
    }

    @Test(expected = RuntimeException.class)
    public void getId_providerThrowsException() throws RemoteException {
        AdvertisingIdProviderManager.registerProviderCallable(() -> {
            MockAdvertisingIdProvider mockAdvertisingIdProvider =
                    new MockAdvertisingIdProvider(TESTING_AD_ID, true);
            mockAdvertisingIdProvider.mGetIdThrowsException = true;
            return mockAdvertisingIdProvider;
        });

        AdvertisingIdAidlServiceImpl advertisingIdService = new AdvertisingIdAidlServiceImpl();
        advertisingIdService.getId();
    }

    private static class MockAdvertisingIdProvider implements AdvertisingIdProvider {
        private final String mId;
        private final boolean mLimitAdTrackingEnabled;
        boolean mGetIdThrowsException = false;

        MockAdvertisingIdProvider(String id, boolean limitAdTrackingEnabled) {
            mId = id;
            mLimitAdTrackingEnabled = limitAdTrackingEnabled;
        }

        @NonNull
        @Override
        public String getId() {
            if (mGetIdThrowsException) {
                throw new RuntimeException();
            }
            return mId;
        }

        @Override
        public boolean isLimitAdTrackingEnabled() {
            return mLimitAdTrackingEnabled;
        }
    }
}
