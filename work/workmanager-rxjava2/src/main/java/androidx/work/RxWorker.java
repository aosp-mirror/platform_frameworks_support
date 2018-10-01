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

package androidx.work;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.util.Preconditions;

import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * RxJava2 interoperability Worker implementation.
 *
 * @see Worker
 */
public abstract class RxWorker extends NonBlockingWorker {
    // Package-private to avoid synthetic accessor.
    SettableFuture<Payload> mFuture;
    Disposable mDisposable;
    private final Consumer<Payload> mOnSuccess = new Consumer<Payload>() {
        @Override
        public void accept(Payload payload) {
            try {
                mFuture.set(payload);
            } catch (Throwable t) {
                mFuture.setException(t);
            }
        }
    };

    private final Consumer<Throwable> mOnError = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) {
            mFuture.setException(throwable);
        }
    };

    private Runnable mFutureCancelListener = new Runnable() {
        @Override
        public void run() {
            if (mFuture != null && mFuture.isCancelled()) {
                Disposable disposable = mDisposable;
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }
    };

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public RxWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public final ListenableFuture<Payload> onStartWork() {
        Preconditions.checkState(mFuture == null, "called onStart twice?");
        mFuture = SettableFuture.create();
        Scheduler scheduler = getBackgroundScheduler();
        mDisposable = doWork()
                .subscribeOn(scheduler)
                .subscribe(mOnSuccess, mOnError);
        mFuture.addListener(mFutureCancelListener, getBackgroundExecutor());
        return mFuture;
    }

    /**
     * Returns the default background scheduler which uses the Thread pool that was provided to
     * the {@link WorkManager} configuration.
     *
     * @return The default {@link Scheduler}.
     */
    protected Scheduler getBackgroundScheduler() {
        return Schedulers.from(getBackgroundExecutor());
    }

    /**
     * Override this method to define your actual work and return a {@code Single} of
     * {@link androidx.work.NonBlockingWorker.Payload} which will be subscribed by the
     * {@link WorkManager}.
     * <p>
     * If the returned {@code Single} fails, the worker will be considered as failed.
     * <p>
     * If the {@link RxWorker} is cancelled by the {@link WorkManager} (e.g. due to a constraint
     * change), {@link WorkManager} will dispose the subscription immediately.
     *
     * @return a {@code Single<Payload>} that represents the work.
     */
    @MainThread
    abstract Single<Payload> doWork();

    @Override
    public void onStopped(boolean cancelled) {
        super.onStopped(cancelled);
        final Disposable disposable = mDisposable;
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
