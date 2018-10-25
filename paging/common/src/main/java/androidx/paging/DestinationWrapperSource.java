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
import androidx.paging.futures.AsyncFunction;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

class DestinationWrapperSource<K, V> extends ListenableSource<K, V> {
    ListenableSource<K, V> mOriginalSource;
    ListenableDestination<K, V> mDestination;

    DestinationWrapperSource(
            ListenableSource<K, V> orig,
            ListenableDestination destination) {
        super(orig.mType);
        mOriginalSource = orig;
        mDestination = destination;
    }

    private static final Executor sImmediateExecutor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

    // TODO: load keys from disk
    static class KeyResult<V> extends ListenableSource.BaseResult<V> {
        KeyResult(Object nextKey, Object prevKey) {
            super(null, nextKey, prevKey, 0, 0, 0);
        }
    }

    @Override
    ListenableFuture<? extends BaseResult<V>> load(@NonNull final Params<K> params) {
        // want to transform, but keep original result.
        // Need to think about isRetryableError here too, how to avoid
        // dispatching that to mOriginalSource.isRetryableError
        return Futures.awaitAsync(mOriginalSource.load(params),
                new AsyncFunction<BaseResult<V>, Void>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public ListenableFuture<Void> apply(BaseResult<V> input) {
                        return mDestination.store(params.type,
                                (K) input.mPrevKey,
                                input.mData,
                                (K) input.mNextKey);
                    }
                }, sImmediateExecutor);
    }

    @Nullable
    @Override
    K getKey(@NonNull V item) {
        return mOriginalSource.getKey(item);
    }

    @Override
    boolean isRetryableError(@NonNull Throwable error) {
        return mOriginalSource.isRetryableError(error);
    }

    @Nullable
    @Override
    K getKey(int lastLoad, @NonNull V item) {
        return mOriginalSource.getKey(lastLoad, item);
    }
}
