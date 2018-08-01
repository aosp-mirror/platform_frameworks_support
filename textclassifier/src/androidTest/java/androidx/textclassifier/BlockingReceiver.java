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

package androidx.textclassifier;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.util.Preconditions;

import junit.framework.AssertionFailedError;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * BroadcastReceiver that can block for a PendingIntent.
 */
final class BlockingReceiver extends BroadcastReceiver {

    private final Context mContext;
    private final String mExpectedAction;
    private final CountDownLatch mLatch;

    BlockingReceiver(Context context, String action) {
        mContext = Preconditions.checkNotNull(context);
        mExpectedAction = Preconditions.checkNotNull(action);
        mLatch = new CountDownLatch(1);
    }

    public PendingIntent registerForPendingIntent() {
        mContext.registerReceiver(this, new IntentFilter(mExpectedAction));
        return PendingIntent.getBroadcast(mContext, 0, new Intent(mExpectedAction), 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(mExpectedAction)) {
            mLatch.countDown();
        }
    }

    /**
     * @throws AssertionFailedError if the pending intent is not received before the timeout
     */
    public void awaitPendingIntent() throws InterruptedException {
        try {
            if (!mLatch.await(1000, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError(
                        "Did not receive PendingIntent(action=" + mExpectedAction + ")");
            }
        } finally {
            unregister();
        }
    }
    /**
     * @throws AssertionFailedError if the pending intent is received during the specified time
     */
    public void assertNoPendingIntent(int timeoutMs) throws InterruptedException {
        try {
            if (mLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Received unexpected PendingIntent");
            }
        } finally {
            unregister();
        }
    }

    /**
     * Unregisters this broadcast receiver.
     */
    public void unregister() {
        mContext.unregisterReceiver(this);
    }
}
