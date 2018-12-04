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

package androidx.webkit;

import androidx.concurrent.futures.ResolvableFuture;

import org.junit.Assume;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper methods for common webkit test tasks.
 *
 * <p>
 * This should remain functionally equivalent to android.webkit.cts.WebkitUtils.
 * Modifications to this class should be reflected in that class as necessary. See
 * http://go/modifying-webview-cts.
 */
public final class WebkitUtils {

    // Arbitrary timeout. Note that @SmallTest and @MediumTest are documented as both requiring
    // execution times < 1000ms.
    private static final long TEST_TIMEOUT_MS = 20000L; // 20s.

    /**
     * Throws {@link org.junit.AssumptionViolatedException} if the device does not support the
     * particular feature, otherwise returns.
     *
     * <p>
     * This provides a more descriptive error message than a bare {@code assumeTrue} call.
     *
     * <p>
     * Note that this method is AndroidX-specific, and is not reflected in the CTS class.
     *
     * @param featureName the feature to be checked
     */
    public static void checkFeature(String featureName) {
        final String msg = "This device does not have the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeTrue(msg, hasFeature);
    }

    /**
     * Waits for {@code future} and returns its value (or times out).
     */
    public static <T> T waitForFuture(Future<T> future) throws InterruptedException,
             ExecutionException,
             TimeoutException {
        return future.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public static class FutureFactory<T> {
        private final List<ResolvableFuture<T>> mFutures;
        private int mNumberOfFutures;
        private final Object mLock = new Object();
        private final int mExpectedNumberOfFutures;
        private final ResolvableFuture<Void> mInternalFuture;

        public FutureFactory(int expectedNumberOfFutures) {
            mFutures = new ArrayList<>();
            mNumberOfFutures = 0;
            mExpectedNumberOfFutures = expectedNumberOfFutures;
            mInternalFuture = ResolvableFuture.create();
        }

        public ResolvableFuture<T> getNewFuture() {
            ResolvableFuture<T> future = ResolvableFuture.create();
            synchronized (mLock) {
                if (mNumberOfFutures > mExpectedNumberOfFutures) {
                    throw new IllegalStateException("Cannot create more than "
                            + mExpectedNumberOfFutures + " Futures");
                }
                mFutures.add(future);
                mNumberOfFutures++;

                if (mNumberOfFutures == mExpectedNumberOfFutures) {
                    // Done modifying the map, so getAllCreatedFutures() may return now.
                    mInternalFuture.set(null);
                }
            }
            return future;
        }

        public List<ResolvableFuture<T>> getAllCreatedFutures()
                throws InterruptedException, ExecutionException, TimeoutException {
            // Wait for all expected Futures to be created.
            mInternalFuture.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return mFutures;
        }
    }

    // Do not instantiate this class.
    private WebkitUtils() {}
}
