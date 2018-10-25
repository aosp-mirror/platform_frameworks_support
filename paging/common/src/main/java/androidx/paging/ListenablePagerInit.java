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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public class ListenablePagerInit {
    static class RetryableException extends Exception {
        final Runnable retry;
        final Throwable cause;
        RetryableException(Runnable retry, Throwable cause) {
            this.retry = retry;
            this.cause = cause;
        }
    }

    static <K, V> ListenableFuture<ListenableDestination.KeyPair<K>> performLoad(
            @NonNull final ListenableSource.LoadType type,
            @Nullable final K initialKey,
            @NonNull final PagedList.Config config,
            @NonNull final ListenableSource<K, V> source,
            @NonNull final ListenableDestination<K, V> destination,
            @NonNull final Executor notifyExecutor) {
        final ResolvableFuture<ListenableDestination.KeyPair<K>> future = ResolvableFuture.create();
        final Runnable[] retryArray = new Runnable[1];

        retryArray[0] = new Runnable() {
            @Override
            public void run() {
                ListenableFuture<ListenableDestination.KeyPair<K>> keyFuture = null;
                if (type == ListenableSource.LoadType.INITIAL) {
                    keyFuture = destination.restoreKeys();
                }
                if (keyFuture != null) {
                    Futures.addCallback(keyFuture, new FutureCallback<ListenableDestination
                            .KeyPair<K>>() {
                        @Override
                        public void onSuccess(ListenableDestination.KeyPair<K> value) {
                            future.set(new ListenableDestination.KeyPair<>(
                                    value.previousKey, value.nextKey));
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            // TODO: only wrap if retryable...
                            future.setException(new RetryableException(retryArray[0], throwable));
                        }
                    }, notifyExecutor);
                    // TODO: else if item / position based, wait on boundary callback
                    // TODO: can we pass those in as ListenableFuture<int, Item>?
                } else {
                    ListenableFuture<? extends ListenableSource.BaseResult<V>> fetchFuture =
                            source.load(new ListenableSource.Params<>(
                                    type,
                                    initialKey,
                                    config.initialLoadSizeHint,
                                    config.enablePlaceholders,
                                    config.pageSize));
                    Futures.addCallback(fetchFuture, new FutureCallback<ListenableSource
                            .BaseResult<V>>() {
                        @Override
                        public void onSuccess(final ListenableSource.BaseResult<V> value) {
                            retryArray[0] = new Runnable() {
                                @Override
                                public void run() {
                                    //noinspection unchecked
                                    @SuppressWarnings("unchecked")
                                    final K prevKey = (K) value.mPrevKey;
                                    @SuppressWarnings("unchecked")
                                    final K nextKey = (K) value.mNextKey;

                                    Futures.addCallback(destination.store(type, prevKey,
                                            value.mData, nextKey), new FutureCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            future.set(new ListenableDestination.KeyPair<>(
                                                    prevKey, nextKey));
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable throwable) {
                                            // TODO: only wrap if retryable...
                                            future.setException(new RetryableException(
                                                    retryArray[0], throwable));
                                        }
                                    }, notifyExecutor);
                                }
                            };
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            // TODO: if retryable...
                            future.setException(new RetryableException(retryArray[0], throwable));
                        }
                    }, notifyExecutor);
                }
            }
        };
        retryArray[0].run();
        return future;
    }
}
