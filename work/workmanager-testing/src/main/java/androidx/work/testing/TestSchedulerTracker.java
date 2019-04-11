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

package androidx.work.testing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.impl.model.WorkSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents a list of requests being tracked by {@link TestScheduler}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestSchedulerTracker {

    private static final Object sLock = new Object();
    private static final long AWAIT_TIME = 30L;

    private Drainable mDrainable;
    private CountDownLatch mCurrentLatch;

    private final Context mContext;
    private final Map<String, InternalWorkState> mActiveRequests;
    private final Set<String> mTerminalWorkSpecs;

    TestSchedulerTracker(@NonNull Context context) {
        mContext = context;
        mActiveRequests = new HashMap<>();
        mTerminalWorkSpecs = new HashSet<>();
    }

    InternalWorkState getActiveRequest(@NonNull String workSpecId) {
        synchronized (sLock) {
            return mActiveRequests.get(workSpecId);
        }
    }

    void track(@NonNull WorkSpec workSpec) {
        synchronized (sLock) {
            if (!mActiveRequests.containsKey(workSpec.id)) {
                mActiveRequests.put(workSpec.id, new InternalWorkState(mContext, workSpec));
                mTerminalWorkSpecs.remove(workSpec.id);
            }
        }
    }

    void markComplete(@NonNull String workSpecId) {
        synchronized (sLock) {
            mActiveRequests.remove(workSpecId);
            mTerminalWorkSpecs.add(workSpecId);
            checkIfDrainComplete();
        }
    }

    void await(@NonNull Drainable drainable) throws InterruptedException {
        mDrainable = drainable;
        mCurrentLatch = new CountDownLatch(1);
        synchronized (sLock) {
            checkIfDrainComplete();
        }
        if (mCurrentLatch.getCount() > 0) {
            mCurrentLatch.await(AWAIT_TIME, TimeUnit.SECONDS);
        }
    }

    private void checkIfDrainComplete() {
        if (mDrainable != null) {
            if (mDrainable instanceof Drainable.DrainAll) {
                // wait for all requests to be drained
                if (mActiveRequests.size() <= 0) {
                    notifyDrainComplete();
                }
            } else if (mDrainable instanceof Drainable.DrainWorkSpec) {
                Drainable.DrainWorkSpec drainWorkSpec = (Drainable.DrainWorkSpec) mDrainable;
                if (mTerminalWorkSpecs.contains(drainWorkSpec.getId())) {
                    notifyDrainComplete();
                }
            }
        }
    }

    private void notifyDrainComplete() {
        // reset
        mCurrentLatch.countDown();
        mDrainable = null;
    }
}
