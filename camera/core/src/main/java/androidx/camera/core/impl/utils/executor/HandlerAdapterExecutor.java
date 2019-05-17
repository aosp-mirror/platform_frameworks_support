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

package androidx.camera.core.impl.utils.executor;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class HandlerAdapterExecutor implements Executor {

    private static ThreadLocal<HandlerAdapterExecutor> sHandlerThreadLocal =
            new ThreadLocal<HandlerAdapterExecutor>() {
                @Override
                public HandlerAdapterExecutor initialValue() {
                    if (Looper.myLooper() != null) {
                        Handler handler = new Handler(Looper.myLooper());
                        return new HandlerAdapterExecutor(handler);
                    }

                    return null;
                }
            };
    private final Handler mHandler;

    HandlerAdapterExecutor(@NonNull Handler handler) {
        mHandler = Preconditions.checkNotNull(handler);
    }

    /**
     * Retrieves a cached executor derived from the current thread's looper.
     */
    static Executor currentThreadExecutor() {
        HandlerAdapterExecutor executor = sHandlerThreadLocal.get();
        if (executor == null || !isCurrent(executor)) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("Current thread has no looper!");
            }

            executor = new HandlerAdapterExecutor(new Handler(looper));
            sHandlerThreadLocal.set(executor);
        }

        return executor;
    }

    /**
     * Checks if the provided HandlerAdapterExecutor wraps the current thread's looper.
     */
    private static boolean isCurrent(@NonNull HandlerAdapterExecutor executor) {
        return executor.mHandler.getLooper() == Looper.myLooper();
    }

    @Override
    public void execute(Runnable command) {
        if (!mHandler.post(command)) {
            throw new RejectedExecutionException(mHandler + " is shutting down");
        }
    }
}
