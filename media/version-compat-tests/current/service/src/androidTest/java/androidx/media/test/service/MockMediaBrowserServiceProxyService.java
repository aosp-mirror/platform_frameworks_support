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

package androidx.media.test.service;

import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ITEM;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IMockMediaBrowserServiceProxy;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.media.test.lib.TestUtils;
import androidx.media.test.service.MockMediaBrowserServiceCompat.Proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * A service that gets requests from client app for setting
 * {@link MockMediaBrowserServiceCompat.Proxy} in order to override the callback of
 * {@link MockMediaBrowserServiceCompat}.
 */
public class MockMediaBrowserServiceProxyService extends Service {
    private IBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new MockMediaBrowserServiceProxyStub();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class MockMediaBrowserServiceProxyStub extends IMockMediaBrowserServiceProxy.Stub {

        @Override
        public void cleanUp() throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(null);
        }

        @Override
        public void overrideOnGetRoot(final String givenPackageName, final int givenUid,
                final Bundle givenRootHints, final String resultRootId,
                final Bundle resultRootExtras) throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
                @Override
                public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                        Bundle rootHints) {
                    if (isStringMatched(givenPackageName, clientPackageName)
                            && isUidMatched(givenUid, clientUid)
                            && isBundleMatched(givenRootHints, rootHints)) {
                        return new BrowserRoot(resultRootId, resultRootExtras);
                    }
                    return new BrowserRoot("stub", null);
                }
            });
        }

        @Override
        public void overrideOnLoadChildren(final String givenParentId,
                final List<Bundle> resultList) throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
                @Override
                public void onLoadChildren(String parentId,
                        Result<List<MediaItem>> result) {
                    if (isStringMatched(givenParentId, parentId)) {
                        result.sendResult(getItemListFromBundleList(resultList));
                        return;
                    }
                    result.detach();
                }
            });
        }

        @Override
        public void overrideOnLoadChildrenWithOptions(final String givenParentId,
                final Bundle givenOptions, final List<Bundle> resultList) throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
                @Override
                public void onLoadChildren(String parentId, Result<List<MediaItem>> result,
                        Bundle options) {
                    if (isStringMatched(givenParentId, parentId)
                            && isBundleMatched(givenOptions, options)) {
                        result.sendResult(getItemListFromBundleList(resultList));
                        return;
                    }
                    result.detach();
                }
            });
        }

        @Override
        public void overrideOnLoadItem(final String givenItemId, final Bundle resultItem)
                throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
                @Override
                public void onLoadItem(String itemId, Result<MediaItem> result) {
                    if (isStringMatched(givenItemId, itemId)) {
                        result.sendResult(getItemFromBundle(resultItem));
                        return;
                    }
                    result.detach();
                }
            });
        }

        @Override
        public void overrideOnSearch(final String givenQuery, final Bundle givenExtras,
                final List<Bundle> resultList) throws RemoteException {
            MockMediaBrowserServiceCompat.setMediaBrowserServiceProxy(new Proxy() {
                @Override
                public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result) {
                    if (isStringMatched(givenQuery, query)
                            && isBundleMatched(givenExtras, extras)) {
                        result.sendResult(getItemListFromBundleList(resultList));
                        return;
                    }
                    result.detach();
                }
            });
        }

        private boolean isStringMatched(String expected, String actual) {
            if (expected == null) {
                return true;
            }
            return expected.equals(actual);
        }

        private boolean isUidMatched(int expected, int actual) {
            if (expected < 0) {
                return true;
            }
            return expected == actual;
        }

        private boolean isBundleMatched(Bundle expected, Bundle actual) {
            if (expected == null) {
                return true;
            }
            // The 'actual' bundle should have all the contents in 'expected' bundle.
            return TestUtils.contains(actual, expected);
        }
    }

    public static List<MediaItem> getItemListFromBundleList(List<Bundle> bundleList) {
        if (bundleList == null) {
            return null;
        }
        List<MediaItem> result = new ArrayList<>();
        for (int i = 0; i < bundleList.size(); i++) {
            Bundle bundle = bundleList.get(i);
            result.add(getItemFromBundle(bundle));
        }
        return result;
    }

    public static MediaItem getItemFromBundle(Bundle itemBundle) {
        if (itemBundle == null || !itemBundle.containsKey(KEY_MEDIA_ITEM)) {
            return null;
        }
        return (MediaItem) itemBundle.getParcelable(KEY_MEDIA_ITEM);
    }
}
