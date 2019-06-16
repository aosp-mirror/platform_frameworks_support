/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.room.RoomMasterTable.COLUMN_ID;
import static androidx.room.RoomMasterTable.COLUMN_IDENTITY_HASH;
import static androidx.room.RoomMasterTable.TABLE_NAME;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Application;

import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RoomOpenHelperTest {

    private static String sCreteV1Query = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_IDENTITY_HASH + " TEXT)";


    private static String sTestDbName = "RoomOpenHelperTest";
    private DatabaseConfiguration mConfiguration;
    private RoomOpenHelper.Delegate mDelegate = mock(RoomOpenHelper.Delegate.class);
    private Executor mInstantExecutor = runnable -> runnable.run();
    private Application mApplication;
    private List<SupportSQLiteDatabase> mCreatedDbs = new ArrayList<>();

    @After
    public void deleteDb() throws IOException {
        for (SupportSQLiteDatabase db : mCreatedDbs) {
            db.close();
        }
        mApplication.deleteDatabase(sTestDbName);
    }

    @Before
    public void init() {
        mApplication = ApplicationProvider.getApplicationContext();
        mApplication.deleteDatabase(sTestDbName);
        mConfiguration = new DatabaseConfiguration(
                mApplication, //context
                sTestDbName, // name
                configuration -> null, // open helper factory
                new RoomDatabase.MigrationContainer(),
                Collections.<RoomDatabase.Callback>emptyList(),
                false, // allow main thread queries
                RoomDatabase.JournalMode.AUTOMATIC,
                mInstantExecutor,
                mInstantExecutor,
                false, // multi instance
                false, // require migration
                true, // allow destructive
                null, // migration not required from
                DatabaseConfiguration.COPY_FROM_NONE,
                null // copy from path
        );
    }

    private SupportSQLiteDatabase openDb() {
        SupportSQLiteOpenHelper db = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(mApplication).name(
                        sTestDbName).callback(
                        new SupportSQLiteOpenHelper.Callback(1) {
                                @Override
                                public void onCreate(SupportSQLiteDatabase db) {
                                }

                                @Override
                                public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                        int newVersion) {
                                }
                            }).build());
        mCreatedDbs.add(db.getWritableDatabase());
        return db.getWritableDatabase();
    }

    private void setExistingHash(String hash) throws IOException {
        SupportSQLiteDatabase db = openDb();
        db.execSQL(
                RoomMasterTable.CREATE_QUERY
        );
        db.execSQL(
                RoomMasterTable.createInsertQuery(hash)
        );
        db.close();
    }

    @Test
    public void room_v1_helper() throws IOException {
        setExistingHash("foo");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "foo");
        SupportSQLiteDatabase db = openDb();
        helper.onOpen(db);
        verify(mDelegate).onOpen(db);
        verifyNoMoreInteractions(mDelegate);
    }

    @Test
    public void room_v1_1_helper() throws IOException {
        setExistingHash("bar");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        SupportSQLiteDatabase db = openDb();
        helper.onOpen(db);
        verify(mDelegate).onOpen(db);
        verifyNoMoreInteractions(mDelegate);
    }

    @Test
    public void room_v1_1_helper_badHash() throws IOException {
        setExistingHash("bad_hash");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        try {
            helper.onOpen(openDb());
            Assert.fail("should've thrown an exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void room_v1_1_helper_legacy() throws IOException {
        setExistingHash("foo");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        SupportSQLiteDatabase db = openDb();
        helper.onOpen(db);
        verify(mDelegate).onOpen(db);
        verifyNoMoreInteractions(mDelegate);
    }

    @Test
    public void room_v1_1_helper_legacy_badHash() throws IOException {
        setExistingHash("bad_hash");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        try {
            helper.onOpen(openDb());
            Assert.fail("should've thrown an exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void updateMasterTable_fromV1() throws IOException {
        // FROM V1 to V2, we change id column from INT to String
        SupportSQLiteDatabase oldDb = openDb();
        oldDb.execSQL(
                sCreteV1Query
        );
        oldDb.execSQL(
                RoomMasterTable.createInsertQuery("foo")
        );
        oldDb.close();

        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        SupportSQLiteDatabase db = openDb();
        helper.onOpen(db);
        verify(mDelegate).onOpen(db);
        verifyNoMoreInteractions(mDelegate);
        TableInfo masterTableInfo = TableInfo.read(db, TABLE_NAME);
        TableInfo.Column idColumn = masterTableInfo.columns.get(COLUMN_ID);
        Assert.assertNotNull(idColumn);
        Assert.assertEquals(ColumnInfo.TEXT, idColumn.affinity);

        TableInfo.Column idHashColumn = masterTableInfo.columns.get(COLUMN_IDENTITY_HASH);
        Assert.assertNotNull(idHashColumn);
        Assert.assertEquals(ColumnInfo.TEXT, idHashColumn.affinity);

        Assert.assertEquals(2, masterTableInfo.columns.size());
    }
}
