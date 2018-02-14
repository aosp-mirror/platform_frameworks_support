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

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.integration.testapp.TestDatabase;
import android.arch.persistence.room.integration.testapp.dao.UserDao;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
@SdkSuppress(minSdkVersion = 16)
public class WriteAheadLoggingTest {

    private static final String DATABASE_NAME = "wal.db";
    private TestDatabase mDatabase;

    @Before
    public void openDatabase() {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        mDatabase = Room.databaseBuilder(context, TestDatabase.class, DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build();
    }

    @After
    public void closeDatabase() {
        mDatabase.close();
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void checkJournalMode() {
        Cursor c = null;
        try {
            SupportSQLiteDatabase db = mDatabase.getOpenHelper().getWritableDatabase();
            c = db.query("PRAGMA journal_mode");
            c.moveToFirst();
            String journalMode = c.getString(0);
            assertThat(journalMode, is(equalToIgnoringCase("wal")));
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Test
    public void disableWal() {
        Context context = InstrumentationRegistry.getTargetContext();
        mDatabase.close();
        mDatabase = Room.databaseBuilder(context, TestDatabase.class, DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build();
        Cursor c = null;
        try {
            SupportSQLiteDatabase db = mDatabase.getOpenHelper().getWritableDatabase();
            c = db.query("PRAGMA journal_mode");
            c.moveToFirst();
            String journalMode = c.getString(0);
            assertThat(journalMode, is(not(equalToIgnoringCase("wal"))));
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Test
    public void observeLiveData() {
        UserDao dao = mDatabase.getUserDao();
        LiveData<User> user1 = dao.liveUserById(1);
        Observer<User> observer = startObserver(user1);
        dao.insert(TestUtil.createUser(1));
        verify(observer, timeout(30000).atLeastOnce())
                .onChanged(argThat(user -> user != null && user.getId() == 1));
        stopObserver(user1, observer);
    }

    @Test
    public void parallelWrites() throws InterruptedException, ExecutionException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            final int id = i + 1;
            futures.add(i, executor.submit(() -> {
                User user = TestUtil.createUser(id);
                user.setName("user" + id);
                mDatabase.getUserDao().insert(user);
                return true;
            }));
        }
        LiveData<List<User>> usersList = mDatabase.getUserDao().liveUsersListByName("user");
        Observer<List<User>> observer = startObserver(usersList);
        for (Future future : futures) {
            assertThat(future.get(), is(true));
        }
        verify(observer, timeout(3000).atLeastOnce())
                .onChanged(argThat(users -> users != null && users.size() == numberOfThreads));
        stopObserver(usersList, observer);
    }

    @Test
    public void readInBackground() throws InterruptedException, ExecutionException {
        final UserDao dao = mDatabase.getUserDao();
        final User user1 = TestUtil.createUser(1);
        dao.insert(user1);
        Future<Boolean> future;
        try {
            mDatabase.beginTransaction();
            dao.delete(user1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            future = executor.submit(() -> {
                assertThat(dao.load(1), is(equalTo(user1)));
                return true;
            });
            future.get();
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
        assertThat(dao.count(), is(0));
    }

    @Test
    public void invalidation() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDatabase.getInvalidationTracker().addObserver(new InvalidationTracker.Observer("User") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                latch.countDown();
            }
        });
        mDatabase.getUserDao().insert(TestUtil.createUser(1));
        latch.await(3000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 10; i++) {
            // This can (occasionally) detect if there is an recursive loop in InvalidationTracker
            // invalidating itself by running its refresh query in a transaction.
            assertThat(mDatabase.inTransaction(), is(false));
        }
    }

    private static <T> Observer<T> startObserver(LiveData<T> liveData) {
        @SuppressWarnings("unchecked")
        Observer<T> observer = mock(Observer.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(observer));
        return observer;
    }

    private static <T> void stopObserver(LiveData<T> liveData, Observer<T> observer) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.removeObserver(observer));
    }
}
