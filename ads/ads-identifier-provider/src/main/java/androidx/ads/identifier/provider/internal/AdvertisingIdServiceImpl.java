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

import android.os.RemoteException;

import androidx.ads.identifier.provider.AdvertisingIdProvider;
import androidx.ads.identifier.provider.AdvertisingIdProviderManager;
import androidx.ads.identifier.provider.IAdvertisingIdService;

import java.util.concurrent.Callable;

class AdvertisingIdServiceImpl extends IAdvertisingIdService.Stub {

    private AdvertisingIdProvider mProvider;

    AdvertisingIdServiceImpl() {
        Callable<AdvertisingIdProvider> providerCallable =
                AdvertisingIdProviderManager.getProviderCallable();
        if (providerCallable == null) {
            throw new IllegalStateException("Ad ID provider not registered.");
        }
        try {
            mProvider = providerCallable.call();
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch the Ad ID provider.", e);
        }
        if (mProvider == null) {
            throw new IllegalArgumentException("Fetched Ad ID provider is null.");
        }
    }

    @Override
    public String getId() throws RemoteException {
        return mProvider.getId();
    }

    @Override
    public boolean isLimitAdTrackingEnabled() throws RemoteException {
        return mProvider.isLimitAdTrackingEnabled();
    }
}
