/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.persistence.room.migration.bundle;

import android.support.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data class that holds the schema information about a primary key.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PrimaryKeyBundle {
    @SerializedName("columnNames")
    private List<String> mColumnNames;
    @SerializedName("autoGenerate")
    private boolean mAutoGenerate;

    public PrimaryKeyBundle(boolean autoGenerate, List<String> columnNames) {
        mColumnNames = columnNames;
        mAutoGenerate = autoGenerate;
    }

    public List<String> getColumnNames() {
        return mColumnNames;
    }

    public boolean isAutoGenerate() {
        return mAutoGenerate;
    }
}
