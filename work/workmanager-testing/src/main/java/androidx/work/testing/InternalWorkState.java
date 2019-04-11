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
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

/**
 * Keeps track of a WorkRequest's internal state.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InternalWorkState {
    @NonNull
    Context mContext;
    @NonNull
    WorkSpec mWorkSpec;
    boolean mConstraintsMet;
    boolean mInitialDelayMet;
    boolean mPeriodDelayMet;

    InternalWorkState(@NonNull Context context, @NonNull WorkSpec workSpec) {
        mContext = context;
        mWorkSpec = workSpec;
        mConstraintsMet = !mWorkSpec.hasConstraints();
        mInitialDelayMet = (mWorkSpec.initialDelay == 0L);
        mPeriodDelayMet = true;
    }

    void reset() {
        mConstraintsMet = !mWorkSpec.hasConstraints();
        mPeriodDelayMet = !mWorkSpec.isPeriodic();
        if (mWorkSpec.isPeriodic()) {
            // Reset the startTime to simulate the first run of PeriodicWork.
            // Otherwise WorkerWrapper de-dupes runs of PeriodicWork to 1 execution per interval
            WorkManagerImpl workManager = WorkManagerImpl.getInstance(mContext);
            WorkDatabase workDatabase = workManager.getWorkDatabase();
            workDatabase.workSpecDao().setPeriodStartTime(mWorkSpec.id, 0);
        }
    }

    boolean isRunnable() {
        return (mConstraintsMet && mInitialDelayMet && mPeriodDelayMet);
    }
}
