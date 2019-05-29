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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.Nullable;

/**
 * Provide a mock for {@link androidx.ads.identifier.provider.IAdvertisingIdService}.
 * To be used in unit tests.
 */
public class MockAdvertisingIdService extends Service {

    enum ResultType {
        NORMAL,
        TIMEOUT,
        RUNTIME_EXCEPTION,
        REMOTE_EXCEPTION,
    }

    static ResultType sGetIdResultType = ResultType.NORMAL;
    static String sId = null;
    static boolean sLimitAdTrackingEnabled = false;

    private MockAdvertisingIdServiceImpl mAdvertisingIdServiceImpl;

    @Override
    public void onCreate() {
        mAdvertisingIdServiceImpl = new MockAdvertisingIdServiceImpl();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAdvertisingIdServiceImpl;
    }

    private static class MockAdvertisingIdServiceImpl extends IAdvertisingIdService.Stub {
        @Override
        public String getId() throws RemoteException {
            switch (sGetIdResultType) {
                case TIMEOUT:
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException ignored) {
                    }
                    return sId;
                case RUNTIME_EXCEPTION:
                    throw new RuntimeException();
                case REMOTE_EXCEPTION:
                    throw new RemoteException();
                default:
                    return sId;
            }
        }

        @Override
        public boolean isLimitAdTrackingEnabled() throws RemoteException {
            return sLimitAdTrackingEnabled;
        }
    }
}
