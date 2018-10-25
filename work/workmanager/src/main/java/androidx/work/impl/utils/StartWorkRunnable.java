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

package androidx.work.impl.utils;

import android.support.annotation.RestrictTo;

import androidx.work.WorkerParameters;
import androidx.work.impl.WorkManagerEngine;

/**
 * A {@link Runnable} that can start work on the
 * {@link androidx.work.impl.Processor}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StartWorkRunnable implements Runnable {

    private WorkManagerEngine mEngine;
    private String mWorkSpecId;
    private WorkerParameters.RuntimeExtras mRuntimeExtras;

    public StartWorkRunnable(
            WorkManagerEngine engine,
            String workSpecId,
            WorkerParameters.RuntimeExtras runtimeExtras) {
        mEngine = engine;
        mWorkSpecId = workSpecId;
        mRuntimeExtras = runtimeExtras;
    }

    @Override
    public void run() {
        mEngine.getProcessor().startWork(mWorkSpecId, mRuntimeExtras);
    }
}
