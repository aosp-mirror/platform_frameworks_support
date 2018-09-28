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

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.vo.User;
import androidx.room.paging.RoomItemKeyedDataSource;

import java.util.Collections;
import java.util.List;

public class AgeKeyedDataSource extends RoomItemKeyedDataSource<Integer, User> {
    public static class Factory extends DataSource.Factory<Integer, User> {
        final TestDatabase mDb;
        final int mMinimumAge;

        public Factory(TestDatabase db, int minimumAge) {
            mDb = db;
            mMinimumAge = minimumAge;
        }

        @NonNull
        @Override
        public DataSource<Integer, User> create() {
            return new AgeKeyedDataSource(mDb, mMinimumAge);
        }
    }

    private final int mMinimumAge;
    private final UserDao mUserDao;

    protected AgeKeyedDataSource(TestDatabase db, int minimumAge) {
        super(db, "user");
        mUserDao = db.getUserDao();
        mMinimumAge = minimumAge;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params,
            @NonNull LoadInitialCallback<User> callback) {
        Integer userId = params.requestedInitialKey;
        List<User> list;
        if (userId != null) {
            // initial keyed load - load before 'userId',
            // and load after last item in before list
            int pageSize = params.requestedLoadSize / 2;
            Integer key = userId;
            list = mUserDao.pagedByAgeLoadBefore(mMinimumAge, key, pageSize);
            Collections.reverse(list);
            if (!list.isEmpty()) {
                key = getKey(list.get(list.size() - 1));
            }
            list.addAll(mUserDao.pagedByAgeLoadAfter(mMinimumAge, key, pageSize));
        } else {
            list = mUserDao.pagedByAgeInitial(mMinimumAge, params.requestedLoadSize);
        }

        if (params.placeholdersEnabled) {
            if (list.isEmpty()) {
                // empty result, no data
                callback.onResult(list, 0, 0);
            } else {
                Integer firstKey = getKey(list.get(0));
                Integer lastKey = getKey(list.get(list.size() - 1));

                // only bother counting if placeholders are desired
                final int position = mUserDao.pagedByAgeCountBefore(mMinimumAge, firstKey);
                final int count = position
                        + list.size()
                        + mUserDao.pagedByAgeCountAfter(mMinimumAge, lastKey);
                callback.onResult(list, position, count);
            }
        } else {
            callback.onResult(list);
        }
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<User> callback) {
        callback.onResult(mUserDao.pagedByAgeLoadAfter(
                mMinimumAge, params.key, params.requestedLoadSize));
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<User> callback) {
        List<User> list = mUserDao.pagedByAgeLoadBefore(
                mMinimumAge, params.key, params.requestedLoadSize);
        Collections.reverse(list);
        callback.onResult(list);
    }

    @NonNull
    @Override
    public Integer getKey(@NonNull User item) {
        return item.getId();
    }
}
