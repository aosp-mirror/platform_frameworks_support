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

package androidx.paging.futures;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class Futures {
    private Futures() {}

    public static <V> void addCallback(@NonNull final ListenableFuture<V> future,
            @NonNull final FutureCallback<? super V> callback, @NonNull Executor executor) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                final V value;
                try {
                    value = future.get();
                } catch (ExecutionException e) {
                    callback.onError(e.getCause());
                    return;
                } catch (Throwable e) {
                    callback.onError(e);
                    return;
                }
                callback.onSuccess(value);
            }
        }, executor);
    }

    public static <I, O> ListenableFuture<O> transformAsync(
            @NonNull ListenableFuture<I> input,
            @NonNull final AsyncFunction<? super I, ? extends O> function,
            @NonNull final Executor executor) {
        // TODO: cancellation
        final ResolvableFuture<O> out = ResolvableFuture.create();
        addCallback(input, new FutureCallback<I>() {
            @Override
            public void onSuccess(I value) {
                addCallback(function.apply(value), new FutureCallback<O>() {
                    @Override
                    public void onSuccess(O value) {
                        out.set(value);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        out.setException(throwable);
                    }
                }, executor);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                out.setException(throwable);
            }
        }, executor);
        return out;
    }
}
