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

package androidx.work.impl.background.gcm;

import android.support.annotation.NonNull;

import androidx.work.Logger;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * The {@link GcmTaskService} responsible for handling requests for executing
 * {@link androidx.work.WorkRequest}s.
 */
public class WorkManagerGcmService extends GcmTaskService {
    private static final String TAG = Logger.tagWithPrefix("WorkManagerGcmService");

    @Override
    public int onRunTask(@NonNull TaskParams taskParams) {
        Logger.get().debug(TAG, String.format("Handing task %s", taskParams));

        String tag = taskParams.getTag();
        if (tag == null || tag.isEmpty()) {
            // Bad request. No WorkSpec id.
            return GcmNetworkManager.RESULT_FAILURE;
        }

        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
