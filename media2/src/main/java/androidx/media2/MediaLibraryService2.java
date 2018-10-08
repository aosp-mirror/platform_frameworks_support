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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_NOT_SUPPORTED;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.media2.MediaBrowser2.BrowserResult.ResultCode;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.Builder;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Base class for media library services, which is the service containing
 * {@link MediaLibrarySession}.
 * <p>
 * Media library services enable applications to browse media content provided by an application
 * and ask the application to start playing it. They may also be used to control content that
 * is already playing by way of a {@link MediaSession2}.
 * <p>
 * When extending this class, also add the following to your {@code AndroidManifest.xml}.
 * <pre>
 * &lt;service android:name="component_name_of_your_implementation" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.media.MediaLibraryService2" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 * <p>
 * You may also declare <pre>android.media.browse.MediaBrowserService</pre> for compatibility with
 * {@link android.support.v4.media.MediaBrowserCompat}. This service can handle it automatically.
 *
 * @see MediaSessionService2
 */
public abstract class MediaLibraryService2 extends MediaSessionService2 {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "android.media.MediaLibraryService2";

    /**
     * Session for the {@link MediaLibraryService2}. Build this object with
     * {@link Builder} and return in {@link #onGetSession()}.
     */
    public static final class MediaLibrarySession extends MediaSession2 {
        /**
         * Callback for the {@link MediaLibrarySession}.
         */
        public static class MediaLibrarySessionCallback extends MediaSession2.SessionCallback {
            /**
             * Called to get the root information for browsing by a particular client.
             * <p>
             * The implementation should verify that the client package has permission
             * to access browse media information before returning the root id; it
             * should return {@code null} if the client is not allowed to access this information.
             * <p>
             * Interoperability: this callback may be called on the main thread, regardless of the
             * callback executor.
             *
             * @param session the session for this event
             * @param controller information of the controller requesting access to browse media.
             * @param params An optional library params of service-specific arguments to send
             *               to the media library service when connecting and retrieving the
             *               root id for browsing, or {@code null} if none. The contents of this
             *               bundle may affect the information returned when browsing.
             * @return The library result
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT
             */
            public @NonNull LibraryResult onGetLibraryRoot(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_CODE_NOT_SUPPORTED);
            }

            /**
             * Called to get an item. Return result here for the browser.
             * <p>
             * Return {@code null} for no result or error.
             *
             * @param session the session for this event
             * @param controller controller
             * @param mediaId item id to get media item.
             * @return a media item. {@code null} for no result or error.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_ITEM
             */
            public @NonNull LibraryResult onGetItem(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaId) {
                return new LibraryResult(RESULT_CODE_NOT_SUPPORTED);
            }

            /**
             * Called to get children of given parent id. Return the children here for the browser.
             * <p>
             * Return an empty list for no children, and return {@code null} for the error.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId parent id to get children
             * @param page page number. Starts from {@code 0}.
             * @param pageSize page size. Should be greater or equal to {@code 1}.
             * @param params library params
             * @return list of children. Can be {@code null}.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_CHILDREN
             */
            public @NonNull LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_CODE_NOT_SUPPORTED);
            }

            /**
             * Called when a controller subscribes to the parent.
             * <p>
             * It's your responsibility to keep subscriptions by your own and call
             * {@link MediaLibrarySession#notifyChildrenChanged(
             * ControllerInfo, String, int, LibraryParams)} when the parent is changed until it's
             * unsubscribed.
             * <p>
             * Interoperability: This will be called when
             * {@link android.support.v4.media.MediaBrowserCompat#subscribe} is called.
             * However, this won't be called when {@link MediaBrowser#subscribe} is called.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId parent id
             * @param params library params
             * @return result code. {@link LibraryResult#RESULT_CODE_NOT_SUPPORTED} by default.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_SUBSCRIBE
             */
            public @ResultCode int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                return RESULT_CODE_NOT_SUPPORTED;
            }

            /**
             * Called when a controller unsubscribes to the parent.
             * <p>
             * Interoperability: This wouldn't be called if {@link MediaBrowser#unsubscribe} is
             * called while works well with
             * {@link android.support.v4.media.MediaBrowserCompat#unsubscribe}.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId parent id
             * @return result code. {@link LibraryResult#RESULT_CODE_NOT_SUPPORTED} by default.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_UNSUBSCRIBE
             */
            public @ResultCode int onUnsubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId) {
                return RESULT_CODE_NOT_SUPPORTED;
            }

            /**
             * Called when a controller requests search.
             * <p>
             * Return immediately the result of the attempt to search with the query, and notify
             * the number of search result through
             * {@link #notifySearchResultChanged(ControllerInfo, String, int, LibraryParams)}.
             * {@link MediaBrowser2} will ask the search result with the pagination later.
             *
             * @param session the session for this event
             * @param controller controller
             * @param query The search query sent from the media browser. It contains keywords
             *              separated by space.
             * @param params library params
             * @return result code. {@link LibraryResult#RESULT_CODE_NOT_SUPPORTED} by default.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_SEARCH
             */
            public @ResultCode int onSearch(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String query,
                    @Nullable LibraryParams params) {
                return RESULT_CODE_NOT_SUPPORTED;
            }

            /**
             * Called to get the search result. Return search result here for the browser which has
             * requested search previously.
             * <p>
             * Return an empty list for no search result.
             * <p>
             * This may be called with a query that hasn't called with {@link #onSearch}, especially
             * when {@link android.support.v4.media.MediaBrowserCompat#search} is used.
             *
             * @param session the session for this event
             * @param controller controller
             * @param query The search query which was previously sent through {@link #onSearch}.
             * @param page page number. Starts from {@code 0}.
             * @param pageSize page size. Should be greater or equal to {@code 1}.
             * @param params library params
             * @return search result.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT
             */
            public @NonNull LibraryResult onGetSearchResult(
                    @NonNull MediaLibrarySession session, @NonNull ControllerInfo controller,
                    @NonNull String query, int page, int pageSize, @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_CODE_NOT_SUPPORTED);
            }
        }

        /**
         * Builder for {@link MediaLibrarySession}.
         */
        // Override all methods just to show them with the type instead of generics in Javadoc.
        // This workarounds javadoc issue described in the MediaSession2.BuilderBase.
        // Note: Don't override #setSessionCallback() because the callback can be set by the
        // constructor.
        public static final class Builder extends MediaSession2.BuilderBase<MediaLibrarySession,
                Builder, MediaLibrarySessionCallback> {
            // Builder requires MediaLibraryService2 instead of Context just to ensure that the
            // builder can be only instantiated within the MediaLibraryService2.
            // Ideally it's better to make it inner class of service to enforce, but it violates API
            // guideline that Builders should be the inner class of the building target.
            public Builder(@NonNull MediaLibraryService2 service,
                    @NonNull SessionPlayer2 player,
                    @NonNull Executor callbackExecutor,
                    @NonNull MediaLibrarySessionCallback callback) {
                super(service, player);
                setSessionCallback(callbackExecutor, callback);
            }

            @Override
            public @NonNull Builder setSessionActivity(@Nullable PendingIntent pi) {
                return super.setSessionActivity(pi);
            }

            @Override
            public @NonNull Builder setId(@NonNull String id) {
                return super.setId(id);
            }

            @Override
            public @NonNull MediaLibrarySession build() {
                if (mCallbackExecutor == null) {
                    mCallbackExecutor = ContextCompat.getMainExecutor(mContext);
                }
                if (mCallback == null) {
                    mCallback = new MediaLibrarySession.MediaLibrarySessionCallback() {};
                }
                return new MediaLibrarySession(mContext, mId, mPlayer, mSessionActivity,
                        mCallbackExecutor, mCallback);
            }
        }

        MediaLibrarySession(Context context, String id, SessionPlayer2 player,
                PendingIntent sessionActivity, Executor callbackExecutor,
                MediaSession2.SessionCallback callback) {
            super(context, id, player, sessionActivity, callbackExecutor, callback);
        }

        @Override
        MediaLibrarySessionImpl createImpl(Context context, String id, SessionPlayer2 player,
                PendingIntent sessionActivity, Executor callbackExecutor,
                MediaSession2.SessionCallback callback) {
            return new MediaLibrarySessionImplBase(this, context, id, player, sessionActivity,
                    callbackExecutor, callback);
        }

        @Override
        MediaLibrarySessionImpl getImpl() {
            return (MediaLibrarySessionImpl) super.getImpl();
        }

        /**
         * Notify the controller of the change in a parent's children.
         * <p>
         * If the controller hasn't subscribed to the parent, the API will do nothing.
         * <p>
         * Controllers will use {@link MediaBrowser2#getChildren(String, int, int, LibraryParams)}
         * to get the list of children.
         *
         * @param controller controller to notify
         * @param parentId parent id with changes in its children
         * @param itemCount number of children.
         * @param params library params
         */
        public void notifyChildrenChanged(@NonNull ControllerInfo controller,
                @NonNull String parentId, int itemCount, @Nullable LibraryParams params) {
            getImpl().notifyChildrenChanged(controller, parentId, itemCount, params);
        }

        /**
         * Notify all controllers that subscribed to the parent about change in the parent's
         * children, regardless of the extra bundle supplied by
         * {@link MediaBrowser2#subscribe(String, LibraryParams)}.
         *  @param parentId parent id
         * @param itemCount number of children
         * @param params library params
         */
        // This is for the backward compatibility.
        public void notifyChildrenChanged(@NonNull String parentId, int itemCount,
                @Nullable LibraryParams params) {
            getImpl().notifyChildrenChanged(parentId, itemCount, params);
        }

        /**
         * Notify controller about change in the search result.
         *  @param controller controller to notify
         * @param query previously sent search query from the controller.
         * @param itemCount the number of items that have been found in the search.
         * @param params library params
         */
        public void notifySearchResultChanged(@NonNull ControllerInfo controller,
                @NonNull String query, int itemCount, @Nullable LibraryParams params) {
            getImpl().notifySearchResultChanged(controller, query, itemCount, params);
        }

        @Override
        MediaLibrarySessionCallback getCallback() {
            return (MediaLibrarySessionCallback) super.getCallback();
        }

        interface MediaLibrarySessionImpl extends MediaSession2Impl {
            // LibrarySession methods
            void notifyChildrenChanged(
                    @NonNull String parentId, int itemCount, @Nullable LibraryParams params);
            void notifyChildrenChanged(@NonNull ControllerInfo controller,
                    @NonNull String parentId, int itemCount, @Nullable LibraryParams params);
            void notifySearchResultChanged(@NonNull ControllerInfo controller,
                    @NonNull String query, int itemCount, @Nullable LibraryParams params);

            // LibrarySession callback implementations called on the executors
            LibraryResult onGetLibraryRootOnExecutor(@NonNull ControllerInfo controller,
                    @Nullable LibraryParams params);
            LibraryResult onGetItemOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String mediaId);
            LibraryResult onGetChildrenOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId, int page, int pageSize,
                    @Nullable LibraryParams params);
            int onSubscribeOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId, @Nullable LibraryParams params);
            int onUnsubscribeOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId);
            int onSearchOnExecutor(@NonNull ControllerInfo controller, @NonNull String query,
                    @Nullable LibraryParams params);
            LibraryResult onGetSearchResultOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String query, int page, int pageSize, @Nullable LibraryParams params);

            // Internally used methods - only changing return type
            @Override
            MediaLibrarySession getInstance();

            @Override
            MediaLibrarySessionCallback getCallback();
        }
    }

    @Override
    MediaSessionService2Impl createImpl() {
        return new MediaLibraryService2ImplBase();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    /**
     * Called when another app has requested to get {@link MediaLibrarySession}.
     * <p>
     * Session returned here will be added to this service automatically. You don't need to call
     * {@link #addSession(MediaSession2)} for that.
     * <p>
     * This method will be called on the main thread.
     *
     * @return a new library session
     * @see Builder
     * @see #getSessions()
     */
    @Override
    public @NonNull abstract MediaLibrarySession onGetSession();

    /**
     * Contains information that the library service needs to send to the client.
     */
    @VersionedParcelize
    public static final class LibraryParams implements VersionedParcelable {
        /**
         * The lookup key for a boolean that indicates whether the library service should return
         * recently played media items.
         * <p>
         * When creating a media browser for a given media library service, this key can be
         * supplied for retrieving media items that are recently played.
         * If the media library service can provide such media items, the implementation must return
         * the media item
         * key in the metadata of the media item
         * from the {@link MediaLibrarySessionCallback#onGetLibraryRoot}.
         */

        // Note: Value is the same as MediaBrowserServiceCompat.BrowserRoot#EXTRA_RECENT for
        //       interop.
        public static final String KEY_RECENT = "android.media.extra.RECENT";

        /**
         * The lookup key for a boolean that indicates whether the library service should return
         * offline media items, which can be played without an internet connection.
         * <p>
         * If the media library service can provide such media items, the implementation must return
         * the result with the key.
         * <p>
         * The library param may contain multiple keys.
         */
        // Note: Value is the same as MediaBrowserServiceCompat.BrowserRoot#EXTRA_OFFLINE for
        //       interop.
        public static final String KEY_OFFLINE = "android.media.extra.OFFLINE";

        /**
         * The lookup key for a boolean that indicates whether the library service should return
         * suggested media items.
         * <p>
         * If the media library service can provide such media items, the implementation must return
         * the result with the key. The list of media items is considered ordered by relevance,
         * first being the top suggestion.
         * <p>
         * The library param may contain multiple keys.
         */
        // Note: Value is the same as MediaBrowserServiceCompat.BrowserRoot#EXTRA_SUGGESTED for
        //       interop.
        public static final String KEY_SUGGESTED = "android.media.extra.SUGGESTED";

        static final String KEY_EXTRAS = "android.media.extra.EXTRAS";

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @StringDef({KEY_RECENT, KEY_OFFLINE, KEY_SUGGESTED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface BooleanKey {}

        static final int TYPE_BOOLEAN = 1;
        static final int TYPE_BUNDLE = 2;
        static final ArrayMap<String, Integer> KEY_TYPE_MAP;

        static {
            KEY_TYPE_MAP = new ArrayMap<>();
            KEY_TYPE_MAP.put(KEY_RECENT, TYPE_BOOLEAN);
            KEY_TYPE_MAP.put(KEY_OFFLINE, TYPE_BOOLEAN);
            KEY_TYPE_MAP.put(KEY_SUGGESTED, TYPE_BOOLEAN);
            KEY_TYPE_MAP.put(KEY_EXTRAS, TYPE_BUNDLE);
        }

        @ParcelField(1)
        Bundle mBundle;

        // For versioned parcelable.
        LibraryParams() {
            // no-op
        }

        private LibraryParams(Bundle bundle) {
            mBundle = bundle;
        }

        /**
         * Gets extras.
         */
        public @Nullable Bundle getExtras() {
            return mBundle.getBundle(KEY_EXTRAS);
        }

        /**
         * Returns the value associated with the given key, or {@code false} if no mapping of
         * the desired type exists for the given key.
         *
         * @param key The key the value is stored under
         * @return a boolean value
         */
        public boolean getBoolean(@NonNull @BooleanKey String key) {
            if (key == null) {
                throw new IllegalArgumentException("key shouldn't be null");
            }
            return mBundle.getBoolean(key);
        }

        /**
         * Builds {@link LibraryParams}.
         */
        public static final class Builder {
            private Bundle mBundle;

            /**
             * Constructor.
             */
            public Builder() {
                mBundle = new Bundle();
            }

            Builder(@Nullable Bundle bundle) {
                mBundle = bundle == null ? new Bundle() : new Bundle(bundle);
            }

            /**
             * Put a boolean value into the metadata. Custom keys may be used.
             */
            public Builder putBoolean(@NonNull @BooleanKey String key, boolean value) {
                if (key == null) {
                    throw new IllegalArgumentException("key shouldn't be null");
                }
                if (KEY_TYPE_MAP.containsKey(key)) {
                    if (KEY_TYPE_MAP.get(key) != TYPE_BOOLEAN) {
                        throw new IllegalArgumentException("The " + key
                                + " key cannot be used to put a float");
                    }
                }
                mBundle.putBoolean(key, value);
                return this;
            }

            /**
             * Set a bundle of extras.
             *
             * @param extras The extras to include with this description or null.
             * @return The Builder to allow chaining
             */
            public Builder setExtras(@Nullable Bundle extras) {
                mBundle.putBundle(KEY_EXTRAS, extras);
                return this;
            }

            /**
             * Builds {@link LibraryParams}
             *
             * @return new LibraryParams
             */
            public LibraryParams build() {
                return new LibraryParams(mBundle);
            }
        }
    }

    /**
     * Result class to be used with {@link ListenableFuture} for asynchronous calls.
     */
    // Specify full class name to workaround build error 'cannot find symbol'.
    @androidx.versionedparcelable.VersionedParcelize
    public static class LibraryResult implements RemoteResult2,
            androidx.versionedparcelable.VersionedParcelable {
        /**
         * @hide
         */
        @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
                RESULT_CODE_SUCCESS,
                RESULT_CODE_UNKNOWN_ERROR,
                RESULT_CODE_INVALID_STATE,
                RESULT_CODE_BAD_VALUE,
                RESULT_CODE_PERMISSION_DENIED,
                RESULT_CODE_IO_ERROR,
                RESULT_CODE_SKIPPED,
                RESULT_CODE_DISCONNECTED,
                RESULT_CODE_NOT_SUPPORTED,
                RESULT_CODE_AUTHENTICATION_EXPIRED,
                RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED,
                RESULT_CODE_CONCURRENT_STREAM_LIMIT,
                RESULT_CODE_PARENTAL_CONTROL_RESTRICTED,
                RESULT_CODE_NOT_AVAILABLE_IN_REGION,
                RESULT_CODE_SKIP_LIMIT_REACHED,
                RESULT_CODE_SETUP_REQUIRED})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface ResultCode {}

        @ParcelField(1)
        int mResultCode;
        @ParcelField(2)
        long mCompletionTime;
        @ParcelField(4)
        MediaItem2 mItem;
        @ParcelField(5)
        List<MediaItem2> mItems;

        // For versioned parcelable
        LibraryResult() {
            // no-op.
        }

        /**
         * Constructor only with the result code.
         * <p>
         * For success, consider using other constructor that you can also return the result.
         *
         * @param resultCode result code
         */
        public LibraryResult(@ResultCode int resultCode) {
            this(resultCode, null, null);
        }

        public LibraryResult(@ResultCode int resultCode, @Nullable MediaItem2 item) {
            this(resultCode, item, null);
        }

        public LibraryResult(@ResultCode int resultCode, @Nullable List<MediaItem2> items) {
            this(resultCode, null, items);
        }

        private LibraryResult(@ResultCode int resultCode, @Nullable MediaItem2 item,
                @Nullable List<MediaItem2> items) {
            mResultCode = resultCode;
            mCompletionTime = SystemClock.elapsedRealtime();
            mItem = item;
            mItems = items;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public int getResultCode() {
            return mResultCode;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public long getCompletionTime() {
            return mCompletionTime;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public MediaItem2 getMediaItem() {
            return mItem;
        }

        List<MediaItem2> getMediaItems() {
            return mItems;
        }
    }
}
