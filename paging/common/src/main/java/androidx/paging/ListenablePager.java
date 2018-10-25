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
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class ListenablePager<K, V> {
    public enum State {

        // TODO: retryable for error types below
        IDLE(PagedList.LoadState.IDLE),
        LOADING_KEYS(PagedList.LoadState.LOADING),
        LOADING_KEYS_ERROR(PagedList.LoadState.ERROR),
        FETCHING(PagedList.LoadState.LOADING),
        FETCHING_ERROR(PagedList.LoadState.ERROR),
        STORING(PagedList.LoadState.LOADING),
        STORING_ERROR(PagedList.LoadState.ERROR),
        //ERROR(PagedList.LoadState.ERROR),
        //RETRYABLE_ERROR(PagedList.LoadState.RETRYABLE_ERROR),
        DONE(PagedList.LoadState.DONE);

        public final PagedList.LoadState loadState;

        State(PagedList.LoadState loadState) {
            this.loadState = loadState;
        }
    }

    @Nullable
    private final K mInitialKey;
    @NonNull
    private final PagedList.Config mConfig;
    @NonNull
    private final ListenableSource<K, V> mSource;
    @NonNull
    private final ListenableDestination<K, V> mDestination;
    @NonNull
    private final Executor mNotifyExecutor;
    @NonNull
    private final Executor mLoadExecutor;
    @NonNull
    private final Executor mStoreExecutor;

    private State mInitialLoadState;
    private Throwable mInitialError;
    private Runnable mInitialRetry;

    private State mStartState;
    private Throwable mStartError;
    private State mEndState;
    private Throwable mEndError;

    private void stateAssertAndSet(@NonNull ListenableSource.LoadType type,
            @NonNull State previous,
            @NonNull State current) {
        if (type == ListenableSource.LoadType.INITIAL) {
            if (mInitialLoadState == previous) {
                mInitialLoadState = current;
            } else {
                throw new IllegalStateException("state was incorrect");
            }
        }
        throw new IllegalStateException("TODO");
    }

    private void errorSet(@NonNull ListenableSource.LoadType type, @NonNull Throwable error) {
        switch (type) {
            case INITIAL:
                mInitialError = error;
                break;
            case START:
                mStartError = error;
                break;
            case END:
                mEndError = error;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private K mPrevKey; // TODO: only used if page keyed
    private K mNextKey; // TODO: only use if page keyed

    public ListenablePager(@Nullable K initialKey, @NonNull PagedList.Config config,
            @NonNull ListenableSource<K, V> source,
            @NonNull ListenableDestination<K, V> destination,
            @NonNull Executor notifyExecutor, @NonNull Executor loadExecutor,
            @NonNull Executor storeExecutor) {
        if (source.mType != ListenableSource.KeyType.PAGE_KEYED) {
            throw new IllegalArgumentException("unsupported!");
        }

        mInitialKey = initialKey;
        mConfig = config;
        mSource = source;
        mDestination = destination;
        mNotifyExecutor = notifyExecutor;
        mLoadExecutor = loadExecutor;
        mStoreExecutor = storeExecutor;

        ListenableFuture<ListenableDestination.KeyPair<K>> keyFuture =
                mDestination.restoreKeys();
        if (keyFuture != null) {
            mInitialRetry = new Runnable() {
                @Override
                public void run() {
                    fetchInitialKeys(mDestination.restoreKeys());
                }
            };
            fetchInitialKeys(keyFuture);
        // TODO: else if mDestination, and item / position based, wait on boundary callback
        } else {
            mInitialRetry = new Runnable() {
                @Override
                public void run() {
                    mInitialLoadState = State.FETCHING;
                    listenToFetchFuture(ListenableSource.LoadType.INITIAL, mSource.load(
                            new ListenableSource.Params<>(
                                    ListenableSource.LoadType.INITIAL,
                                    mInitialKey,
                                    mConfig.initialLoadSizeHint,
                                    mConfig.enablePlaceholders,
                                    mConfig.pageSize)));
                }
            };
            mInitialRetry.run();
        }
    }

    private void fetchInitialKeys(ListenableFuture<ListenableDestination.KeyPair<K>> future) {
        mInitialLoadState = State.LOADING_KEYS;
        Futures.addCallback(future, new FutureCallback<ListenableDestination.KeyPair<K>>() {
            @Override
            public void onSuccess(ListenableDestination.KeyPair<K> value) {
                stateAssertAndSet(ListenableSource.LoadType.INITIAL,
                        State.LOADING_KEYS, State.IDLE);
                mPrevKey = value.previousKey;
                mNextKey = value.nextKey;
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                stateAssertAndSet(ListenableSource.LoadType.INITIAL,
                        State.LOADING_KEYS, State.LOADING_KEYS_ERROR);
            }
        }, mNotifyExecutor);
    }

    public void retry() {
        if (mInitialLoadState.loadState == PagedList.LoadState.ERROR) {
            mInitialRetry.run();
        }
    }

    @SuppressWarnings({"WeakerAccess", "unchecked"}) /* synthetic access */
    void onFetchSuccess(@NonNull final ListenableSource.LoadType type,
            @NonNull final ListenableSource.BaseResult<V> result) {
        stateAssertAndSet(type, State.FETCHING, State.STORING);
        mInitialRetry = new Runnable() {
            @Override
            public void run() {
                listenToStoreFuture(type, mDestination.store(
                        type, (K) result.mPrevKey, result.mData, (K) result.mNextKey));
            }
        };
        mInitialRetry.run();

    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onFetchFailure(@NonNull ListenableSource.LoadType type, @NonNull Throwable error) {
        stateAssertAndSet(type, State.FETCHING, State.FETCHING_ERROR);
        errorSet(type, error);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onStoreSuccess(@NonNull ListenableSource.LoadType type) {
        stateAssertAndSet(type, State.STORING, State.IDLE);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onStoreFailure(@NonNull ListenableSource.LoadType type, @NonNull Throwable error) {
        stateAssertAndSet(type, State.STORING, State.STORING_ERROR);
        errorSet(type, error);
    }

    private void listenToFetchFuture(@NonNull final ListenableSource.LoadType type,
            @NonNull final ListenableFuture<? extends ListenableSource.BaseResult<V>> future) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                final ListenableSource.BaseResult<V> value;
                try {
                    value = future.get();
                } catch (ExecutionException e) {
                    onFetchFailure(type, e.getCause());
                    return;
                } catch (Throwable e) {
                    onFetchFailure(type, e);
                    return;
                }
                onFetchSuccess(type, value);
            }
        }, mNotifyExecutor);
    }

    private void listenToStoreFuture(@NonNull final ListenableSource.LoadType type,
            @NonNull final ListenableFuture<Void> future) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    onStoreFailure(type, e.getCause());
                    return;
                } catch (Throwable e) {
                    onStoreFailure(type, e);
                    return;
                }
                onStoreSuccess(type);
            }
        }, mNotifyExecutor);
    }
}
