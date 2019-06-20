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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.util.TableInfo;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ChildDatabaseTest {
    @Entity
    static class ChildEntity {
        @PrimaryKey
        @NonNull
        private String mId;
        private String mName;

        public ChildEntity(@NonNull String id, String name) {
            mId = id;
            mName = name;
        }

        @NonNull
        public String getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }
    }

    @Entity
    static class ParentEntity {
        @PrimaryKey
        @NonNull
        private String mId;
        private String mName;

        public ParentEntity(@NonNull String id, String name) {
            mId = id;
            mName = name;
        }

        @NonNull
        public String getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }
    }

    @Database(
            entities = {ChildEntity.class},
            version = 1,
            exportSchema = false
    )
    static abstract class ChildDb extends RoomDatabase {

    }

    @Database(
            entities = {ParentEntity.class},
            version = 1,
            exportSchema = false
    )
    static abstract class ParentDb extends RoomDatabase {
        public abstract ChildDb getChildDb();
    }

    @Test
    public void getDbs() {
        ParentDb db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                ParentDb.class).build();
        TableInfo parentEntityInfo = TableInfo.read(db.getOpenHelper().getWritableDatabase(),
                "ParentEntity");
        TableInfo childEntityInfo = TableInfo.read(db.getOpenHelper().getWritableDatabase(),
                "ChildEntity");
        assertThat(parentEntityInfo, notNullValue());
        assertThat(parentEntityInfo.columns.size(), is(2));
        assertThat(childEntityInfo, notNullValue());
        assertThat(childEntityInfo.columns.size(), is(2));
    }
}
