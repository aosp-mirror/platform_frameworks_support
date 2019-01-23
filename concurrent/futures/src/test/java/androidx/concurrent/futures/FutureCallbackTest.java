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

/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import static androidx.concurrent.futures.FutureCallback.addCallback;

import static com.google.common.truth.Truth.assertThat;

import androidx.concurrent.futures.FutureCallback.Callback;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import junit.framework.TestCase;

/**
 * Test for {@link Callback}.
 *
 * @author Anthony Zana
 */
public class FutureCallbackTest extends TestCase {

    private static final Executor DIRECT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

    public void testSameThreadSuccess() {
        ResolvableFuture<String> f = ResolvableFuture.create();
        MockCallback callback = new MockCallback("foo");
        addCallback(f, callback, DIRECT_EXECUTOR);
        f.set("foo");
    }

    public void testExecutorSuccess() {
        CountingSameThreadExecutor ex = new CountingSameThreadExecutor();
        ResolvableFuture<String> f = ResolvableFuture.create();
        MockCallback callback = new MockCallback("foo");
        addCallback(f, callback, ex);
        f.set("foo");
        assertEquals(1, ex.runCount);
    }

    // Error cases
    public void testSameThreadExecutionException() {
        ResolvableFuture<String> f = ResolvableFuture.create();
        Exception e = new IllegalArgumentException("foo not found");
        MockCallback callback = new MockCallback(e);
        addCallback(f, callback, DIRECT_EXECUTOR);
        f.setException(e);
    }

    public void testCancel() {
        ResolvableFuture<String> f = ResolvableFuture.create();
        Callback<String> callback =
                new Callback<String>() {
                    private boolean called = false;

                    @Override
                    public void onSuccess(String result) {
                        fail("Was not expecting onSuccess() to be called.");
                    }

                    @Override
                    public synchronized void onFailure(Throwable t) {
                        assertFalse(called);
                        assertThat(t).isInstanceOf(CancellationException.class);
                        called = true;
                    }
                };
        addCallback(f, callback, DIRECT_EXECUTOR);
        f.cancel(true);
    }

    public void testOnSuccessThrowsRuntimeException() throws Exception {
        String result = "result";
        ResolvableFuture<String> future = ResolvableFuture.create();
        Callback<String> callback = new MockCallback(result) {
            @Override
            public synchronized void onSuccess(String result) {
                super.onSuccess(result);
                throw new RuntimeException();
            }
        };
        addCallback(future, callback, DIRECT_EXECUTOR);
        future.set(result);
        assertEquals(result, future.get());
    }

    public void testOnSuccessThrowsError() throws Exception {
        class TestError extends Error {}
        final TestError error = new TestError();
        String result = "result";
        ResolvableFuture<String> future = ResolvableFuture.create();
        Callback<String> callback = new MockCallback(result) {
            @Override
            public void onSuccess(String result) {
                super.onSuccess(result);
                throw error;
            }
        };
        addCallback(future, callback, DIRECT_EXECUTOR);
        try {
            future.set(result);
            fail("Should have thrown");
        } catch (TestError e) {
            assertSame(error, e);
        }
        assertEquals(result, future.get());
    }


    public void testWildcardFuture() {
        ResolvableFuture<String> settable = ResolvableFuture.create();
        ListenableFuture<?> f = settable;
        Callback<Object> callback =
                new Callback<Object>() {
                    @Override
                    public void onSuccess(Object result) {}

                    @Override
                    public void onFailure(Throwable t) {}
                };
        addCallback(f, callback, DIRECT_EXECUTOR);
    }

    private class CountingSameThreadExecutor implements Executor {
        int runCount = 0;

        @Override
        public void execute(Runnable command) {
            command.run();
            runCount++;
        }
    }

    private class MockCallback implements Callback<String> {
        private String value = null;
        private Throwable failure = null;
        private boolean wasCalled = false;

        MockCallback(String expectedValue) {
            this.value = expectedValue;
        }

        public MockCallback(Throwable expectedFailure) {
            this.failure = expectedFailure;
        }

        @Override
        public synchronized void onSuccess(String result) {
            assertFalse(wasCalled);
            wasCalled = true;
            assertEquals(value, result);
        }

        @Override
        public synchronized void onFailure(Throwable t) {
            assertFalse(wasCalled);
            wasCalled = true;
            assertEquals(failure, t);
        }
    }
}