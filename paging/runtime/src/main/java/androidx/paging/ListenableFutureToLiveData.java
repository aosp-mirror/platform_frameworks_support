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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public abstract class ListenableFutureToLiveData<T> implements FutureCallback<T> {

    @NonNull
    private final Executor mNotifyExecutor;

    @NonNull
    private T mCurrentData;

    @Nullable
    private ListenableFuture<T> mCurrentFuture = null;

    @NonNull
    private final MutableLiveData<T> mLiveData = new MutableLiveData<T>() {
        @Override
        protected void onActive() {
            super.onActive();
            // TODO
        }
    };

    ListenableFutureToLiveData(@NonNull Executor notifyExecutor, @NonNull T initialData) {
        mNotifyExecutor = notifyExecutor;
        mCurrentData = initialData;
        //invalidate();
    }

    public abstract void onItemUpdate(@NonNull T previous, @NonNull T next);

    @NonNull
    T getCurrentData() {
        return mCurrentData;
    }

    @NonNull
    LiveData<T> getLiveData() {
        invalidate(); // TODO: this is gross workaround
        return mLiveData;
    }

    abstract ListenableFuture<T> getListenableFuture();

    // subclass must override to handle errors
    @Override
    public abstract void onError(@NonNull Throwable throwable);

    @Override
    public void onSuccess(T value) {
        onItemUpdate(mCurrentData, value);
        mCurrentData = value;
        mLiveData.setValue(value);
    }

    void invalidate() {
        if (mCurrentFuture != null) {
            mCurrentFuture.cancel(false);
        }
        mCurrentFuture = getListenableFuture();
        Futures.addCallback(mCurrentFuture, this, mNotifyExecutor);
    }
}
