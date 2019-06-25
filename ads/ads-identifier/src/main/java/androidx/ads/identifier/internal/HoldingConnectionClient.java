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
import static androidx.ads.identifier.AdvertisingIdUtils.getAdIdProviders;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Pair;

import androidx.ads.identifier.AdvertisingIdNotAvailableException;
import androidx.ads.identifier.AdvertisingIdUtils;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** A client which keeps the ServiceConnection to the {@link IAdvertisingIdService}. */
public class HoldingConnectionClient {

    private static final long SERVICE_CONNECTION_TIMEOUT_SECONDS = 10;
    private static final long AUTO_DISCONNECTION_DELAY_SECONDS = 10;

    private final Context mContext;

    @GuardedBy("this")
    @Nullable
    private BlockingServiceConnection mConnection;

    @GuardedBy("this")
    @Nullable
    private Intent mIntent;

    @GuardedBy("this")
    @Nullable
    private IAdvertisingIdService mService;

    // Need to differentiate synchronization between "this" and "mAutoDisconnectTaskLock".
    // We want to make sure mAutoDisconnectTask to die using join(), but this may introduce
    // a deadlock if we use "this" to synchronize, therefore mAutoDisconnectTaskLock
    // is introduced.
    //
    // Issue:
    // TA:getInfo()->TA:sync(TA.this)->TA:scheduleAutoDisconnect()->TA:join(TB)->
    //     TB:run()->TA:finish()->sync(TA.this) [Dead lock!]
    //
    // Solution:
    // TA:getInfo()->TA:scheduleAutoDisconnect()->TA:sync(mAutoDisconnectTaskLock)->
    //     TA:join(TB)->TB:run()->TA:finish()->sync(TA.this)
    //
    private final Object mAutoDisconnectTaskLock = new Object();

    @GuardedBy("mAutoDisconnectTaskLock")
    @Nullable
    private AutoDisconnectTask mAutoDisconnectTask;

    public HoldingConnectionClient(Context context) {
        mContext = context;
    }

    /** Gets the {@link IAdvertisingIdService} and the {@link Intent} used to connect to it. */
    @NonNull
    @WorkerThread
    public Pair<IAdvertisingIdService, Intent> getServiceWithIntent() throws IOException,
            AdvertisingIdNotAvailableException, TimeoutException, InterruptedException {
        synchronized (mAutoDisconnectTaskLock) {
            if (mAutoDisconnectTask != null) {
                mAutoDisconnectTask.cancel();
                mAutoDisconnectTask = null;
            }
        }
        synchronized (this) {
            if (mConnection == null) {
                mIntent = getIntent(mContext);
                mConnection = getServiceConnection();
                mService = getServiceFromConnection();
            }
            return Pair.create(mService, mIntent);
        }
    }

    /** Schedule an auto disconnect. */
    public void scheduleAutoDisconnect() {
        synchronized (mAutoDisconnectTaskLock) {
            if (mAutoDisconnectTask != null) {
                mAutoDisconnectTask.cancel();
            }
            mAutoDisconnectTask = new AutoDisconnectTask();
        }
    }

    /** Closes the connection. */
    void finish() {
        synchronized (this) {
            if (mConnection == null) {
                return;
            }
            mContext.unbindService(mConnection);
            mConnection = null;
            mService = null;
        }
    }

    /** Closes the connection before the client is finalized. */
    @Override
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    private static Intent getIntent(Context context) throws AdvertisingIdNotAvailableException {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        intent.setComponent(getProviderComponentName(context));
        return intent;
    }

    private static ComponentName getProviderComponentName(Context context)
            throws AdvertisingIdNotAvailableException {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = getAdIdProviders(packageManager);
        ServiceInfo serviceInfo =
                AdvertisingIdUtils.selectServiceByPriority(resolveInfos, packageManager);
        if (serviceInfo == null) {
            throw new AdvertisingIdNotAvailableException("No advertising ID provider available.");
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    @VisibleForTesting
    BlockingServiceConnection getServiceConnection() throws IOException {
        BlockingServiceConnection bsc = new BlockingServiceConnection();
        if (mContext.bindService(mIntent, bsc, Service.BIND_AUTO_CREATE)) {
            return bsc;
        } else {
            throw new IOException("Connection failure");
        }
    }

    /**
     * Get the service from the blocking queue.
     *
     * <p>If the connection not setup yet, this should wait until onServiceConnected event with a
     * {@link #SERVICE_CONNECTION_TIMEOUT_SECONDS} second timeout.
     *
     * @throws TimeoutException     if connection timeout period has expired.
     * @throws InterruptedException if connection has been interrupted before connected.
     */
    @WorkerThread
    private IAdvertisingIdService getServiceFromConnection()
            throws TimeoutException, InterruptedException {
        // Block until the bind is complete, or timeout period is over.
        return IAdvertisingIdService.Stub.asInterface(mConnection.getServiceWithTimeout());
    }

    @VisibleForTesting
    boolean isConnected() {
        synchronized (this) {
            return mConnection != null;
        }
    }

    /**
     * A one-time use ServiceConnection that facilitates waiting for the bind to complete and the
     * passing of the IBinder from the callback thread to the waiting thread.
     */
    class BlockingServiceConnection implements ServiceConnection {
        // Facilitates passing of the IBinder across threads
        private final BlockingQueue<IBinder> mBlockingQueue = new LinkedBlockingQueue<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBlockingQueue.add(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            finish();
        }

        /**
         * Blocks until the bind is complete with a timeout and returns the bound IBinder.
         *
         * <p>This must only be called once.
         *
         * @return the IBinder of the bound service
         * @throws InterruptedException if the current thread is interrupted while waiting for
         *                              the bind
         * @throws TimeoutException     if the timeout period has elapsed
         */
        @WorkerThread
        IBinder getServiceWithTimeout() throws InterruptedException, TimeoutException {
            IBinder binder =
                    mBlockingQueue.poll(SERVICE_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (binder == null) {
                throw new TimeoutException("Timed out waiting for the service connection");
            } else {
                return binder;
            }
        }
    }

    class AutoDisconnectTask extends Thread {
        CountDownLatch mCountDown;

        AutoDisconnectTask() {
            mCountDown = new CountDownLatch(1);
            this.start();
        }

        void cancel() {
            mCountDown.countDown();
        }

        @Override
        public void run() {
            try {
                // If timed out, perform a disconnect.
                if (!mCountDown.await(AUTO_DISCONNECTION_DELAY_SECONDS, TimeUnit.SECONDS)) {
                    finish();
                }
            } catch (InterruptedException e) {
                // In case of an InterruptedException happens. close the connection by default.
                finish();
            }
        }
    }
}
