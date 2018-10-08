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

package androidx.media2;

import static androidx.media2.BaseResult2.RESULT_CODE_INVALID_STATE;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaMetadata2.FLAG_BROWSABLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_MEDIA_ID;

import static org.junit.Assert.assertEquals;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionCallback;
import androidx.media2.TestUtils.SyncHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

/**
 * Mock implementation of {@link MediaLibraryService2} for testing.
 */
public class MockMediaLibraryService2 extends MediaLibraryService2 {
    /**
     * ID of the session that this service will create
     */
    public static final String ID = "TestLibrary";

    public static final String ROOT_ID = "rootId";
    public static final Bundle EXTRAS = new Bundle();

    public static final String MEDIA_ID_GET_ITEM = "media_id_get_item";

    public static final String PARENT_ID = "parent_id";
    public static final String PARENT_ID_NO_CHILDREN = "parent_id_no_children";
    public static final String PARENT_ID_ERROR = "parent_id_error";

    public static final List<MediaItem2> GET_CHILDREN_RESULT = new ArrayList<>();
    public static final int CHILDREN_COUNT = 100;

    public static final String SEARCH_QUERY = "search_query";
    public static final String SEARCH_QUERY_TAKES_TIME = "search_query_takes_time";
    public static final int SEARCH_TIME_IN_MS = 5000;
    public static final String SEARCH_QUERY_EMPTY_RESULT = "search_query_empty_result";

    public static final List<MediaItem2> SEARCH_RESULT = new ArrayList<>();
    public static final int SEARCH_RESULT_COUNT = 50;

    private static final String TAG = "MockMediaLibrarySvc2";

    static {
        EXTRAS.putString(ROOT_ID, ROOT_ID);
    }
    @GuardedBy("MockMediaLibraryService2.class")
    private static SessionToken2 sToken;

    private MediaLibrarySession mSession;

    public MockMediaLibraryService2() {
        super();
        GET_CHILDREN_RESULT.clear();
        String getChildrenMediaIdPrefix = "get_children_media_id_";
        for (int i = 0; i < CHILDREN_COUNT; i++) {
            GET_CHILDREN_RESULT.add(createMediaItem(getChildrenMediaIdPrefix + i));
        }

        SEARCH_RESULT.clear();
        String getSearchResultMediaIdPrefix = "get_search_result_media_id_";
        for (int i = 0; i < SEARCH_RESULT_COUNT; i++) {
            SEARCH_RESULT.add(createMediaItem(getSearchResultMediaIdPrefix + i));
        }
    }

    @Override
    public void onCreate() {
        TestServiceRegistry.getInstance().setServiceInstance(this);
        super.onCreate();
    }

    @Override
    public MediaLibrarySession onGetSession() {
        TestServiceRegistry registry = TestServiceRegistry.getInstance();
        TestServiceRegistry.OnGetSessionHandler onGetSessionHandler =
                registry.getOnGetSessionHandler();
        if (onGetSessionHandler != null) {
            return (MediaLibrarySession) onGetSessionHandler.onGetSession();
        }
        final MockPlayer player = new MockPlayer(1);
        final SyncHandler handler = (SyncHandler) registry.getHandler();
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                handler.post(runnable);
            }
        };
        SessionCallback callback = registry.getSessionCallback();
        MediaLibrarySessionCallback librarySessionCallback;
        if (callback instanceof MediaLibrarySessionCallback) {
            librarySessionCallback = (MediaLibrarySessionCallback) callback;
        } else {
            // Callback hasn't set. Use default callback
            librarySessionCallback = new TestLibrarySessionCallback();
        }
        mSession = new MediaLibrarySession.Builder(MockMediaLibraryService2.this, player, executor,
                librarySessionCallback).setId(ID).build();
        return mSession;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TestServiceRegistry.getInstance().cleanUp();
    }

    public static SessionToken2 getToken(Context context) {
        synchronized (MockMediaLibraryService2.class) {
            if (sToken == null) {
                sToken = new SessionToken2(context, new ComponentName(
                        context.getPackageName(), MockMediaLibraryService2.class.getName()));
                assertEquals(SessionToken2.TYPE_LIBRARY_SERVICE, sToken.getType());
            }
            return sToken;
        }
    }

    private class TestLibrarySessionCallback extends MediaLibrarySessionCallback {
        private String mLastQuery;

        @Override
        public LibraryResult onGetLibraryRoot(MediaLibrarySession session,
                ControllerInfo controller, LibraryParams params) {
            MediaMetadata2 metadata = new MediaMetadata2.Builder()
                    .putString(METADATA_KEY_MEDIA_ID, ROOT_ID).setExtras(EXTRAS).build();
            MediaItem2 item = new MediaItem2.Builder(FLAG_BROWSABLE).setMetadata(metadata).build();
            return new LibraryResult(RESULT_CODE_SUCCESS, item);
        }

        @Override
        public LibraryResult onGetItem(MediaLibrarySession session, ControllerInfo controller,
                String mediaId) {
            if (MEDIA_ID_GET_ITEM.equals(mediaId)) {
                return new LibraryResult(RESULT_CODE_SUCCESS, createMediaItem(mediaId));
            } else {
                return new LibraryResult(RESULT_CODE_BAD_VALUE);
            }
        }

        @Override
        public LibraryResult onGetChildren(MediaLibrarySession session,
                ControllerInfo controller, String parentId, int page, int pageSize,
                LibraryParams params) {
            if (PARENT_ID.equals(parentId)) {
                return new LibraryResult(RESULT_CODE_SUCCESS,
                        getPaginatedResult(GET_CHILDREN_RESULT, page, pageSize));
            } else if (PARENT_ID_ERROR.equals(parentId)) {
                return new LibraryResult(RESULT_CODE_BAD_VALUE);
            }
            // Includes the case of PARENT_ID_NO_CHILDREN.
            return new LibraryResult(RESULT_CODE_SUCCESS, new ArrayList<MediaItem2>());
        }

        @Override
        public int onSearch(MediaLibrarySession session,
                final ControllerInfo controllerInfo, final String query,
                final LibraryParams params) {
            mLastQuery = query;
            if (SEARCH_QUERY.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, SEARCH_RESULT_COUNT,
                        params);
            } else if (SEARCH_QUERY_TAKES_TIME.equals(query)) {
                // Searching takes some time. Notify after 5 seconds.
                Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                    @Override
                    public void run() {
                        mSession.notifySearchResultChanged(
                                controllerInfo, query, SEARCH_RESULT_COUNT, params);
                    }
                }, SEARCH_TIME_IN_MS, TimeUnit.MILLISECONDS);
            } else if (SEARCH_QUERY_EMPTY_RESULT.equals(query)) {
                mSession.notifySearchResultChanged(controllerInfo, query, 0, params);
            } else {
                return RESULT_CODE_BAD_VALUE;
            }
            return RESULT_CODE_SUCCESS;
        }

        @Override
        public LibraryResult onGetSearchResult(MediaLibrarySession session,
                ControllerInfo controllerInfo, String query, int page, int pageSize,
                LibraryParams params) {
            if (!TextUtils.equals(mLastQuery, query)) {
                // Ensure whether onSearch() has called before
                return new LibraryResult(RESULT_CODE_INVALID_STATE);
            }
            if (SEARCH_QUERY.equals(query) || SEARCH_QUERY_TAKES_TIME.equals(query)) {
                return new LibraryResult(RESULT_CODE_SUCCESS,
                        getPaginatedResult(SEARCH_RESULT, page, pageSize));
            } else {
                return new LibraryResult(RESULT_CODE_BAD_VALUE);
            }
        }
    }

    private List<MediaItem2> getPaginatedResult(List<MediaItem2> items, int page, int pageSize) {
        if (items == null) {
            return null;
        } else if (items.size() == 0) {
            return new ArrayList<>();
        }

        final int totalItemCount = items.size();
        int fromIndex = page * pageSize;
        int toIndex = Math.min((page + 1) * pageSize, totalItemCount);

        List<MediaItem2> paginatedResult = new ArrayList<>();
        try {
            // The case of (fromIndex >= totalItemCount) will throw exception below.
            paginatedResult = items.subList(fromIndex, toIndex);
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            Log.d(TAG, "Result is empty for given pagination arguments: totalItemCount="
                    + totalItemCount + ", page=" + page + ", pageSize=" + pageSize, ex);
        }
        return paginatedResult;
    }

    private MediaItem2 createMediaItem(String mediaId) {
        Context context = MockMediaLibraryService2.this;
        return new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE)
                .setMediaId(mediaId)
                .setMetadata(new MediaMetadata2.Builder()
                                .putString(METADATA_KEY_MEDIA_ID, mediaId)
                                .build())
                .build();
    }
}
