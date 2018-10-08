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

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.util.Function;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedList;
import java.util.concurrent.CancellationException;

class FutureMapTransformation<IN, OUT> implements Observer<IN> {
    final Function<IN, ListenableFuture<OUT>> func;
    final LiveData<IN> source;
    final MediatorLiveData<OUT> outLiveData;
    final LinkedList<FutureListener> pendingFutures  = new LinkedList<>();
    private int latestFinishedFutureVersion = -1;

    FutureMapTransformation(
            LiveData<IN> source,
            Function<IN, ListenableFuture<OUT>> func) {
        this.func = func;
        this.source = source;
        outLiveData = new MediatorLiveData<>();
        outLiveData.addSource(source, this);
    }

    @Override
    public void onChanged(IN in) {
        final ListenableFuture<OUT> future = func.apply(in);
        FutureListener listener = new FutureListener(future, source.getVersion());
        pendingFutures.add(listener);
        future.addListener(listener, ArchTaskExecutor.getMainThreadExecutor());
    }

    @SuppressWarnings("WeakerAccess")
    @MainThread
    void handleCompletion(FutureListener listener) {
        pendingFutures.remove(listener);
        if (listener.version < latestFinishedFutureVersion) {
            return;//ignore, too late.
        }
        try {
            final OUT value = listener.future.get();
            latestFinishedFutureVersion = listener.version;
            outLiveData.setValue(value);
        } catch (CancellationException e) {
            // it was cancelled, ignore.
        } catch(Throwable t) {
            // eat error ?
            Log.w("FutureMapTransform", "Future failed and exception will be "
                    + "eaten", t);
        }
        // cancel any other future that is still waiting and older
        while (!pendingFutures.isEmpty()) {
            FutureListener pending = pendingFutures.peek();
            if (pending.version < latestFinishedFutureVersion) {
                pendingFutures.poll();
                pending.future.cancel(false);
            } else {
                break;
            }
        }
    }

    LiveData<OUT> asLiveData() {
        return outLiveData;

    }

    class FutureListener implements Runnable {
        final ListenableFuture<OUT> future;
        final int version;

        FutureListener(ListenableFuture<OUT> future, int version) {
            this.future = future;
            this.version = version;
        }

        @Override
        public void run() {
            if (future.isDone()) {
                handleCompletion(this);
            }
        }
    }
}
