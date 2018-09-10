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

package androidx.media.test.client;

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_BROWSER_SERVICE_PROXY;
import static androidx.media.test.lib.CommonConstants.MEDIA_SESSION2_PROVIDER_SERVICE;
import static androidx.media.test.lib.TestUtils.WAIT_TIME_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IMockMediaBrowserServiceProxy;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class is used for overriding the callback of
 * Mock {@link androidx.media.MediaBrowserServiceCompat} in the service app.
 */

public class MockMediaBrowserServiceProxy {
    private static final String TAG = "MockMediaBrowserServiceProxy";

    private final Context mContext;

    private ServiceConnection mServiceConnection;
    private IMockMediaBrowserServiceProxy mBinder;
    private CountDownLatch mCountDownLatch;

    public MockMediaBrowserServiceProxy(Context context) {
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();

        if (!connect()) {
            fail("Failed to connect to the MockMediaBrowserServiceProxyService.");
        }
    }

    public void overrideOnGetRoot(String givenPackageName, int givenUid, Bundle givenRootHints,
            String resultRootId, Bundle resultRootExtras) {
        try {
            mBinder.overrideOnGetRoot(givenPackageName, givenUid, givenRootHints, resultRootId,
                    resultRootExtras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call overrideOnGetRoot()");
        }
    }

    public void overrideOnLoadChildren(String givenParentId, List<Bundle> resultList) {

    }

    public void overrideOnLoadChildrenWithOptions(String givenParentId, Bundle givenOptions,
            List<Bundle> resultList) {

    }

    public void overrideOnLoadItem(String givenItemId, Bundle resultItem) {

    }

    public void overrideOnSearch(String givenQuery, Bundle givenExtras, List<Bundle> resultList) {

    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to service app's MockMediaBrowserServiceProxyService.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        Intent intent = new Intent(ACTION_MEDIA_BROWSER_SERVICE_PROXY);
        intent.setComponent(MEDIA_SESSION2_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the MockMediaBrowserServiceProxyService of the service app");
        }

        if (bound) {
            try {
                mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from service app's MockMediaBrowserServiceProxyService.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's MockMediaBrowserServiceProxyService.");
            mBinder = IMockMediaBrowserServiceProxy.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
