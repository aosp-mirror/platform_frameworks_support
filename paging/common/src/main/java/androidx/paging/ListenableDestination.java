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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public abstract class ListenableDestination<K, V> {
    static class KeyPair<K> {
        @Nullable
        final K previousKey;
        @Nullable
        final K nextKey;

        KeyPair(@Nullable K previousKey, @Nullable K nextKey) {
            this.previousKey = previousKey;
            this.nextKey = nextKey;
        }
    }

    abstract ListenableFuture<Void> store(@NonNull ListenableSource.LoadType type,
            @Nullable K previousKey, @NonNull List<V> data, @Nullable K nextKey);

    abstract ListenableFuture<KeyPair<K>> restoreKeys();
}
