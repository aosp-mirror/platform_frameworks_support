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

package androidx.concurrent.futures;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * An utility class that provides convenience callback to listen
 * for results of {@link ListenableFuture}
 */
public class FutureCallback {

    private FutureCallback() {

    }

    /**
     * A callback for accepting the results of a {@link java.util.concurrent.Future} computation
     * asynchronously.
     *
     * <p>To attach to a {@link com.google.common.util.concurrent.ListenableFuture}
     * use {@link FutureCallback#addCallback}.
     *
     * @author Anthony Zana
     */
    public interface Callback<V> {
        /** Invoked with the result of the {@code Future} computation when it is successful. */
        void onSuccess(V result);

        /**
         * Invoked when a {@code Future} computation fails or is canceled.
         *
         * <p>If the future's {@link java.util.concurrent.Future#get() get} method throws an
         * {@link java.util.concurrent.ExecutionException}, then
         * the cause is passed to this method. Any other thrown object is passed unaltered.
         */
        void onFailure(@NonNull Throwable t);
    }

    /**
     * Registers separate success and failure callbacks to be run when the {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * <p>The callback is run on {@code executor}. There is no guaranteed ordering of execution of
     * callbacks, but any callback added through this method is guaranteed to be called once the
     * computation is complete.
     *
     * <p>Example:
     *
     * <pre>{@code
     * ListenableFuture<QueryResult> future = ...;
     * Executor e = ...
     * addCallback(future,
     *     new Callback<QueryResult>() {
     *       public void onSuccess(QueryResult result) {
     *         storeInCache(result);
     *       }
     *       public void onFailure(Throwable t) {
     *         reportError(t);
     *       }
     *     }, e);
     * }</pre>
     *
     * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
     * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
     * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
     * callbacks passed to this method.
     *
     * <p>For a more general interface to attach a completion listener to a {@code Future}, see {@link
     * ListenableFuture#addListener addListener}.
     *
     * @param future The future attach the callback to.
     * @param callback The callback to invoke when {@code future} is completed.
     * @param executor The executor to run {@code callback} when the future completes.
     */
    public static <V> void addCallback(
            @NonNull final ListenableFuture<V> future,
            @NonNull final Callback<? super V> callback,
            @NonNull Executor executor) {
        future.addListener(new CallbackListener<V>(future, callback), executor);
    }

    /** See {@link #addCallback(ListenableFuture, Callback, Executor)} for behavioral notes. */
    static final class CallbackListener<V> implements Runnable {
        final Future<V> future;
        final Callback<? super V> callback;

        CallbackListener(Future<V> future, Callback<? super V> callback) {
            this.future = future;
            this.callback = callback;
        }

        @Override
        public void run() {
            final V value;
            try {
                value = getUninterruptibly(future);
            } catch (ExecutionException e) {
                callback.onFailure(e.getCause());
                return;
            } catch (RuntimeException | Error e) {
                callback.onFailure(e);
                return;
            }
            callback.onSuccess(value);
        }

        @Override
        public String toString() {
            return "FutureToCallbacks { " + "callback = " + callback + " }";
        }
    }

    /**
     * internal dependency on other /util/concurrent classes.
     */
    static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}