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

package androidx.work.impl;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.Operation;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * A concrete implementation of a {@link Operation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OperationImpl implements Operation {

    private final LiveData<State> mOperationState;
    private final ListenableFuture<State.SUCCESS> mOperationFuture;

    OperationImpl(
            LiveData<State> operationState,
            ListenableFuture<State.SUCCESS> operationFuture) {
        mOperationState = operationState;
        mOperationFuture = operationFuture;
    }


    @NonNull
    @Override
    public LiveData<State> getState() {
        return mOperationState;
    }

    @NonNull
    @Override
    public ListenableFuture<State.SUCCESS> getResult() {
        return mOperationFuture;
    }

    public static Operation create(ListenableFuture<State.SUCCESS> future) {
        final MutableLiveData<State> state = new MutableLiveData<>();
        Futures.addCallback(future, new FutureCallback<State.SUCCESS>() {
            @Override
            public void onSuccess(State.SUCCESS success) {
                state.postValue(success);
            }

            @Override
            public void onFailure(Throwable throwable) {
                state.postValue(new State.FAILURE(throwable));
            }
        }, MoreExecutors.directExecutor());
        return new OperationImpl(state, future);
    }
}
