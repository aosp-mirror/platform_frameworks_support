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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** BlockingServiceConnection */
public class BlockingServiceConnection implements ServiceConnection {
    // Keeps track of whether or not getService() has already been called
    private boolean mUsed = false;

    // Facilitates passing of the IBinder across threads
    private final BlockingQueue<IBinder> mBlockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBlockingQueue.add(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Don't worry about clearing the returned binder in this case. If it does
        // happen a RemoteException will be thrown, which is already handled.
    }

    /**
     * Blocks until the bind is complete with a timeout and returns the bound IBinder. This must
     * only
     * be called once.
     *
     * @return the IBinder of the bound service
     * @throws InterruptedException  if the current thread is interrupted while waiting for the bind
     * @throws IllegalStateException if called more than once
     * @throws TimeoutException      if the timeout period has elapsed
     */
    public IBinder getServiceWithTimeout(long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        // Calling this method on the main thread risks an ANR - don't allow it.
        checkNotMainThread(
                "BlockingServiceConnection.getServiceWithTimeout() called on main thread");

        if (mUsed) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        }
        mUsed = true;
        IBinder binder = mBlockingQueue.poll(timeout, timeUnit);
        if (binder == null) {
            throw new TimeoutException("Timed out waiting for the service connection");
        } else {
            return binder;
        }
    }

    /**
     * Check that the caller is NOT calling us on the main (UI) thread.
     *
     * @param errorMessage The exception message to use if the check fails.
     */
    public static void checkNotMainThread(String errorMessage) {
        if (isMainThread()) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
