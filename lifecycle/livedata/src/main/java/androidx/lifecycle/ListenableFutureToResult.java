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

package androidx.lifecycle;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

class ListenableFutureToResult<IN, OUT> implements Function<IN, ListenableFuture<Result<OUT>>> {
    private final Function<IN, ListenableFuture<OUT>> mSource;
    ListenableFutureToResult(Function<IN, ListenableFuture<OUT>> source) {
        mSource = source;
    }

    @Override
    public ListenableFuture<Result<OUT>> apply(IN input) {

        final SettableFuture<Result<OUT>> outFuture = SettableFuture.create();
        final ListenableFuture<OUT> sourceLF = mSource.apply(input);
        final Runnable sourceListener = new Runnable() {
            @Override
            public void run() {
                if (sourceLF.isDone()) {
                    try {
                        OUT value = sourceLF.get();
                        outFuture.set(Result.success(value));
                    } catch (Throwable t) {
                        outFuture.set(Result.<OUT>failure(t));
                    }
                }
            }
        };
        final Runnable outListener = new Runnable() {
            @Override
            public void run() {
                if (outFuture.isCancelled()) {
                    // propoganate cancelation
                    sourceLF.cancel(false);
                }
            }
        };
        outFuture.addListener(outListener, ArchTaskExecutor.getMainThreadExecutor());
        sourceLF.addListener(sourceListener, ArchTaskExecutor.getMainThreadExecutor());
        return outFuture;
    }
}
