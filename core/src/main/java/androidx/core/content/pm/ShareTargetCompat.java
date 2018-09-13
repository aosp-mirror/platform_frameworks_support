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

package androidx.core.content.pm;

import android.content.Intent;

import androidx.annotation.NonNull;

class ShareTargetCompat {
    // TODO: Consider if ShareTarget needs to have multiple dataTypes, intents, categories
    String mDataType;
    Intent mIntent;
    String mCategory;

    ShareTargetCompat(@NonNull String dataType,@NonNull Intent intent,@NonNull String category) {
        mDataType = dataType;
        mIntent = intent;
        mCategory = category;
    }

    @Override
    public String toString() {
        return "dataType=" + mDataType + " intent=" + mIntent.toString() + " category=" + mCategory;
    }
}
