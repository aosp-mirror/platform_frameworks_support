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

package androidx.room;

import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

/**
 * Implementation of {@link SupportSQLiteOpenHelper.Factory} that creates
 * {@link SQLiteCopyOpenHelper}.
 */
class SQLiteCopyOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

    private final String mCopyFromFilePath;
    private final SupportSQLiteOpenHelper.Factory mDelegate =
            new FrameworkSQLiteOpenHelperFactory();

    SQLiteCopyOpenHelperFactory(String copyFromFilePath) {
        mCopyFromFilePath = copyFromFilePath;
    }

    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
        return new SQLiteCopyOpenHelper(
                configuration.context,
                mCopyFromFilePath,
                mDelegate.create(configuration));
    }
}
