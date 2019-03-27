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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for Futures.
 * @hide
 */
@RestrictTo(LIBRARY)
public class FutureUtil {

    private FutureUtil() {}

    /**
     * Future that already completed.
     *
     * @param <T> The result object of the Future
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class CompletedFuture<T> implements Future<T> {

        private final T mResult;

        public CompletedFuture(@Nullable T result) {
            mResult = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws ExecutionException, InterruptedException {
            return mResult;
        }

        @Override
        public T get(long timeout, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            return mResult;
        }
    }

}
