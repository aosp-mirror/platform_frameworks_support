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

package androidx.room.integration.testapp.paging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static java.util.Collections.emptyList;

import androidx.annotation.NonNull;
import androidx.paging.ItemKeyedDataSource;
import androidx.room.integration.testapp.test.TestDatabaseTest;
import androidx.room.integration.testapp.test.TestUtil;
import androidx.room.integration.testapp.vo.User;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RoomItemKeyedDataSourceTest extends TestDatabaseTest {

    @After
    public void teardown() {
        mUserDao.deleteEverything();
    }

    private ItemKeyedDataSource<Integer, User> loadUsersByAgeDesc() {
        return new AgeKeyedDataSource(mDatabase, -1);
    }

    @Test
    public void emptyPage() {
        ItemKeyedDataSource<Integer, User> dataSource = loadUsersByAgeDesc();
        @SuppressWarnings("unchecked") ItemKeyedDataSource.LoadInitialCallback<User> callback =
                mock(ItemKeyedDataSource.LoadInitialCallback.class);

        dataSource.loadInitial(new ItemKeyedDataSource.LoadInitialParams<>(
                null, 1, true), callback);
        verify(callback).onResult(emptyList(), 0, 0);
    }

    @Test
    public void loadInitial() {
        List<User> users = createUsers(6);

        ItemKeyedDataSource<Integer, User> dataSource = loadUsersByAgeDesc();
        @SuppressWarnings("unchecked") ItemKeyedDataSource.LoadInitialCallback<User> callback =
                mock(ItemKeyedDataSource.LoadInitialCallback.class);

        dataSource.loadInitial(new ItemKeyedDataSource.LoadInitialParams<>(
                null, 100, true), callback);
        verify(callback).onResult(users, 0, 6);
    }

    @Test
    public void loadAfter() {
        List<User> users = createUsers(4);

        ItemKeyedDataSource<Integer, User> dataSource = loadUsersByAgeDesc();
        @SuppressWarnings("unchecked") ItemKeyedDataSource.LoadInitialCallback<User>
                initialCallback = mock(ItemKeyedDataSource.LoadInitialCallback.class);
        @SuppressWarnings("unchecked") ItemKeyedDataSource.LoadCallback<User> callback =
                mock(ItemKeyedDataSource.LoadCallback.class);

        dataSource.loadInitial(new ItemKeyedDataSource.LoadInitialParams<>(null, 2, true),
                initialCallback);
        verify(initialCallback).onResult(users.subList(0, 2), 0, 4);

        dataSource.loadAfter(new ItemKeyedDataSource.LoadParams<>(
                users.get(1).getId(), 100), callback);
        verify(callback).onResult(users.subList(2, 4));
    }

    @Test
    public void loadBefore() {
        List<User> users = createUsers(4);

        ItemKeyedDataSource<Integer, User> dataSource = loadUsersByAgeDesc();
        @SuppressWarnings("unchecked") ItemKeyedDataSource.LoadCallback<User> callback =
                mock(ItemKeyedDataSource.LoadCallback.class);
        dataSource.loadBefore(new ItemKeyedDataSource.LoadParams<>(
                users.get(2).getId(), 100), callback);
        verify(callback).onResult(users.subList(0, 2));
    }


    @NonNull
    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = TestUtil.createUser(i);
            user.setAge(1);
            mUserDao.insert(user);
            users.add(user);
        }
        return users;
    }
}
