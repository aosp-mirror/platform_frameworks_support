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
import android.os.Looper;

import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.core.util.Preconditions;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A one-time use service client that facilitates waiting for the bind and query to complete and the
 * passing of the {@link Result} from the callback thread to the waiting thread.
 * @param <T> The type of the result value.
 */
public class BlockingServiceClient<T> {

    // Keeps track of whether or not getService() has already been called
    private boolean mUsed = false;

    // Facilitates passing of the Result across threads
    final BlockingQueue<Result<T>> mBlockingQueue = new LinkedBlockingQueue<>();

    final Function<IBinder, Result<T>> mOnServiceConnected;

    public BlockingServiceClient(Context context, Intent intent,
            Function<IBinder, Result<T>> onServiceConnected) throws IOException {
        mOnServiceConnected = onServiceConnected;
        bindService(context, intent);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBlockingQueue.add(mOnServiceConnected.apply(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Don't worry about clearing the returned binder in this case. If it does
            // happen a RemoteException will be thrown, which is already handled.
        }
    };

    /** Connect to an application service, creating it if needed. */
    @VisibleForTesting
    public void bindService(Context context, Intent intent) throws IOException {
        try {
            if (!context.bindService(intent, mConnection, Service.BIND_AUTO_CREATE)) {
                throw new IOException("Connection failure");
            }
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    /**
     * Blocks until the bind and query is complete with a timeout and returns the {@link Result}.
     * This must only be called once.
     *
     * @return the result of the bound service
     * @throws InterruptedException  if the current thread is interrupted while waiting for the bind
     * @throws IllegalStateException if called more than once
     * @throws TimeoutException      if the timeout period has elapsed
     */
    public Result<T> getResultWithTimeout(long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        // Calling this method on the main thread risks an ANR - don't allow it.
        checkNotMainThread(
                "BlockingServiceConnection.getResultWithTimeout() called on main thread");

        if (mUsed) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        }
        mUsed = true;
        Result<T> result = mBlockingQueue.poll(timeout, timeUnit);
        if (result == null) {
            throw new TimeoutException("Timed out waiting for the service");
        } else {
            return result;
        }
    }

    /** Disconnect from an application service. */
    public void unbindService(Context context) {
        context.unbindService(mConnection);
    }

    /**
     * Check that the caller is NOT calling us on the main (UI) thread.
     *
     * @param errorMessage The exception message to use if the check fails.
     */
    public static void checkNotMainThread(String errorMessage) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * The result pass to the waiting thread contains the value if success, or the exception if
     * failure.
     * @param <T> The type of the result value.
     */
    public static class Result<T> {
        private final T mValue;
        private final Exception mException;

        private Result(T value) {
            mValue = Preconditions.checkNotNull(value);
            mException = null;
        }

        private Result(Exception exception) {
            mValue = null;
            mException = exception;
        }

        public boolean isSuccess() {
            return mValue != null;
        }

        public T getValue() {
            return mValue;
        }

        public Exception getException() {
            return mException;
        }

        /** Throws failure exception if it is an instance of {@code declaredType}. */
        public <X extends Throwable> void throwIfInstanceOf(Class<X> declaredType) throws X {
            if (declaredType.isInstance(mException)) {
                throw declaredType.cast(mException);
            }
        }

        /**
         * Create a succeed Result.
         */
        public static <T> Result<T> success(T value) {
            return new Result<>(value);
        }

        /**
         * Create a failed Result.
         */
        public static <T> Result<T> failure(Exception exception) {
            return new Result<T>(exception);
        }
    }
}
