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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.ads.identifier.AdvertisingIdNotAvailableException;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
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
    private final IntentCallback mIntentCallback;
    private final Function<IBinder, IAdvertisingIdService> mServiceCallback;

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

    public HoldingConnectionClient(Context context, IntentCallback intentCallback,
            Function<IBinder, IAdvertisingIdService> serviceCallback) {
        mContext = context;
        mIntentCallback = intentCallback;
        mServiceCallback = serviceCallback;
    }

    /** Gets the {@link IAdvertisingIdService} and the {@link Intent} used to connect to it. */
    public Pair<IAdvertisingIdService, Intent> getServiceWithIntent()
            throws IOException, AdvertisingIdNotAvailableException {
        synchronized (mAutoDisconnectTaskLock) {
            if (mAutoDisconnectTask != null) {
                mAutoDisconnectTask.cancel();
                mAutoDisconnectTask = null;
            }
        }
        synchronized (this) {
            if (mConnection == null) {
                mIntent = mIntentCallback.getIntent();
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
    public void finish() {
        synchronized (this) {
            if (mConnection == null) {
                return;
            }
            try {
                mContext.unbindService(mConnection);
            } catch (Throwable e) {
                // This could happen when the thread takes too long to disconnect from the service.
            }
            mConnection = null;
            mService = null;
        }
    }

    @VisibleForTesting
    private BlockingServiceConnection getServiceConnection() throws IOException {
        final BlockingServiceConnection bsc = new BlockingServiceConnection();
        try {
            if (mContext.bindService(mIntent, bsc, Service.BIND_AUTO_CREATE)) {
                return bsc;
            }
        } catch (Throwable e) {
            throw new IOException(e);
        }

        throw new IOException("Connection failure");
    }

    /**
     * Get the service from the blocking queue.
     *
     * <p>If the connection not setup yet, this should wait until onServiceConnected event with a
     * {@link #SERVICE_CONNECTION_TIMEOUT_SECONDS} second timeout.
     *
     * @throws IOException if connection failed or the timeout period has expired.
     */
    private IAdvertisingIdService getServiceFromConnection() throws IOException {
        IBinder binder;
        try {
            // Block until the bind is complete, or timeout period is over.
            binder = mConnection.getServiceWithTimeout();
        } catch (Throwable e) {
            throw new IOException(e);
        }
        return mServiceCallback.apply(binder);
    }

    /**
     * A one-time use ServiceConnection that facilitates waiting for the bind to complete and the
     * passing of the IBinder from the callback thread to the waiting thread.
     */
    class BlockingServiceConnection implements ServiceConnection {
        // Keeps track of whether or not getServiceWithTimeout() has already been called
        private boolean mUsed = false;

        // Facilitates passing of the IBinder across threads
        private final BlockingQueue<IBinder> mBlockingQueue = new LinkedBlockingQueue<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("BSC", "onServiceConnected");
            mBlockingQueue.add(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("BSC", "onServiceDisconnected");
            finish();
        }

        /**
         * Blocks until the bind is complete with a timeout and returns the bound IBinder. This must
         * only be called once.
         *
         * @return the IBinder of the bound service
         * @throws InterruptedException  if the current thread is interrupted while waiting for
         *                               the bind
         * @throws IllegalStateException if called more than once
         * @throws TimeoutException      if the timeout period has elapsed
         */
        IBinder getServiceWithTimeout() throws InterruptedException, TimeoutException {
            if (mUsed) {
                throw new IllegalStateException(
                        "Cannot call get on this connection more than once");
            }
            mUsed = true;
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
