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

package androidx.media2;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.concurrent.futures.AbstractFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manages sequence number Futures that contains sequence number to be shared across the process.
 */
@TargetApi(Build.VERSION_CODES.P)
class CallSequenceManager implements AutoCloseable {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mSequenceNumber;
    @GuardedBy("mLock")
    private SimpleArrayMap<Integer, RemoteProcessFuture> mCommandSeqToResultMap =
            new SimpleArrayMap<>();

    /**
     * Obtains sequence number without. Used for methods with no return (e.g. close())
     *
     * @return sequence number
     */
    public int obtainSequenceNumber() {
        synchronized (mLock) {
            return mSequenceNumber++;
        }
    }

    /**
     * Creates {@link RemoteProcessFuture} with sequence number. Used to return
     * {@link ListenableFuture} for remote process call.
     *
     * @param <V> genric type for the future
     * @return AbstractFuture with sequence number
     */
    public <V> RemoteProcessFuture<V> createRemoteProcessFuture() {
        final RemoteProcessFuture<V> result;
        final int seq;
        synchronized (mLock) {
            seq = mSequenceNumber++;
            result = RemoteProcessFuture.create(seq);
            mCommandSeqToResultMap.put(seq, result);
        }
        return result;
    }

    /**
     * Removes and return the {@link RemoteProcessFuture} with the sequence id.
     *
     * @param seq
     * @param <V>
     * @return AbstractFuture with sequence number
     */
    public <V> RemoteProcessFuture<V> removeRemoteProcessFuture(int seq) {
        synchronized (mLock) {
            return mCommandSeqToResultMap.remove(seq);
        }
    }

    @Override
    public void close() {
        synchronized (mLock) {
            mCommandSeqToResultMap.clear();
        }
    }

    static final class RemoteProcessFuture<V> extends AbstractFuture<V> {
        private final int mSequenceNumber;

        /**
         * Creates a new {@code SettableFuture} that can be completed or cancelled by a later method
         * call.
         */
        public static <V> RemoteProcessFuture<V> create(int seq) {
            return new RemoteProcessFuture<V>(seq);
        }

        @Override
        public boolean set(@Nullable V value) {
            return super.set(value);
        }

        @Override
        public boolean setException(Throwable throwable) {
            return super.setException(throwable);
        }

        @Override
        public boolean setFuture(ListenableFuture<? extends V> future) {
            return super.setFuture(future);
        }

        public int getSequenceNumber() {
            return mSequenceNumber;
        }

        private RemoteProcessFuture(int seq) {
            mSequenceNumber = seq;
        }
    }
}
