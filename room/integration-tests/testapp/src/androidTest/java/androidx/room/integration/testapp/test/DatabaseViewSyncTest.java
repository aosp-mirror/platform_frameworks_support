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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.DatabaseView;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class DatabaseViewSyncTest {

    private static final String DB_NAME = "view_sync.db";

    @Entity
    static class User {
        @PrimaryKey
        public int id;
        public String name;
    }

    @DatabaseView(value = "SELECT name FROM User", viewName = "MyView")
    static class View1 {
    }

    @DatabaseView(value = "SELECT id, name FROM User", viewName = "MyView")
    static class View2 {
    }

    @Database(entities = {User.class}, version = 1, exportSchema = false)
    abstract static class NoViewDatabase extends RoomDatabase {
    }

    @Database(entities = {User.class}, views = {View1.class}, version = 1, exportSchema = false)
    abstract static class View1Database extends RoomDatabase {
    }

    @Database(entities = {User.class}, views = {View2.class}, version = 1, exportSchema = false)
    abstract static class View2Database extends RoomDatabase {
    }

    private RoomDatabase openDatabase(Class<? extends RoomDatabase> dbClass) {
        final Context context = InstrumentationRegistry.getTargetContext();
        return Room.databaseBuilder(context, dbClass, DB_NAME).build();
    }

    @Test
    public void add() {
        final RoomDatabase db0 = openDatabase(NoViewDatabase.class);
        final HashMap<String, String> views0 = collectViews(db0);
        assertThat(views0.size(), is(0));
        db0.close();

        final RoomDatabase db1 = openDatabase(View1Database.class);
        final HashMap<String, String> views1 = collectViews(db1);
        assertThat(views1.size(), is(1));
        assertThat(views1.get("MyView"),
                is(equalTo("CREATE VIEW `MyView` AS SELECT name FROM User")));
        db1.close();
    }

    @Test
    public void remove() {
        final RoomDatabase db1 = openDatabase(View1Database.class);
        final HashMap<String, String> views1 = collectViews(db1);
        assertThat(views1.size(), is(1));
        assertThat(views1.get("MyView"),
                is(equalTo("CREATE VIEW `MyView` AS SELECT name FROM User")));
        db1.close();

        final RoomDatabase db0 = openDatabase(NoViewDatabase.class);
        final HashMap<String, String> views0 = collectViews(db0);
        assertThat(views0.size(), is(0));
        db0.close();
    }

    @Test
    public void modify() {
        final RoomDatabase db1 = openDatabase(View1Database.class);
        final HashMap<String, String> views1 = collectViews(db1);
        assertThat(views1.size(), is(1));
        assertThat(views1.get("MyView"),
                is(equalTo("CREATE VIEW `MyView` AS SELECT name FROM User")));
        db1.close();

        final RoomDatabase db2 = openDatabase(View2Database.class);
        final HashMap<String, String> views2 = collectViews(db2);
        assertThat(views2.size(), is(1));
        assertThat(views2.get("MyView"),
                is(equalTo("CREATE VIEW `MyView` AS SELECT id, name FROM User")));
        db2.close();
    }

    private static HashMap<String, String> collectViews(RoomDatabase db) {
        final HashMap<String, String> views = new HashMap<>();
        final Cursor c = db.query("SELECT name, sql FROM sqlite_master WHERE type = 'view'", null);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (c.moveToNext()) {
                views.put(c.getString(0), c.getString(1));
            }
        } finally {
            c.close();
        }
        return views;
    }
}
