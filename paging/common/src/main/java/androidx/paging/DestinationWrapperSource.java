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
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

abstract class DestinationWrapperSource<K, V> extends ListenableSource<K, V> {
    ListenableSource<K, V> mOriginalSource;
    ListenableDestination<K, V> mDestination;
    DestinationWrapperSource(
            ListenableSource<K, V> orig,
            ListenableDestination dest) {
        super(orig.mType);
        mOriginalSource = orig;
        mDestination = dest;
    }

    @Override
    ListenableFuture<? extends BaseResult<V>> load(Params<K> params) {
        mOriginalSource.load(params);
        // want to transform, but keep original result.
        // Need to think about isRetryableError here too, how to avoid
        // dispatching that to mOriginalSource.isRetryableError
        return null;
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
}
