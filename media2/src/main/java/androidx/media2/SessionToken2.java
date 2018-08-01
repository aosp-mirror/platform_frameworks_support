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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.app.BundleCompat;
import androidx.core.util.ObjectsCompat;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Represents an ongoing {@link MediaSession2} or a {@link MediaLibrarySession}.
 * <p>
 * This may be passed to apps by the session owner to allow them to create a
 * {@link MediaController2} to communicate with the session.
 */
public final class SessionToken2 extends Token2 {
    private static final String TAG = "SessionToken2";

    private static final long WAIT_TIME_MS_FOR_SESSION_READY = 300;
    private static final int MSG_SEND_TOKEN2_FOR_LEGACY_SESSION = 1000;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {TYPE_SESSION, TYPE_LIBRARY_SESSION, TYPE_SESSION_LEGACY})
    public @interface SessionTokenType {
    }

    /**
     * Type for {@link MediaSession2}.
     */
    public static final int TYPE_SESSION = 0;

    /**
     * Type for {@link MediaLibrarySession}.
     */
    public static final int TYPE_LIBRARY_SESSION = 1;

    /**
     * Type for {@link MediaSessionCompat}.
     */
    static final int TYPE_SESSION_LEGACY = 100;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_UID = "android.media.sessiontoken2.uid";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_TYPE = "android.media.sessiontoken2.type";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_PACKAGE_NAME = "android.media.sessiontoken2.package_name";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_SESSION_ID = "android.media.sessiontoken2.session_id";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_SESSION_BINDER = "android.media.sessiontoken2.session_binder";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_TOKEN_LEGACY = "android.media.sessiontoken2.LEGACY";

    private final SessionToken2Impl mImpl;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public SessionToken2(int uid, int type, String packageName, String sessionId,
            IMediaSession2 iSession2) {
        mImpl = new SessionToken2ImplBase(uid, type, packageName, sessionId, iSession2);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken2(SessionToken2Impl impl) {
        mImpl = impl;
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2)) {
            return false;
        }
        SessionToken2 other = (SessionToken2) obj;
        return mImpl.equals(other.mImpl);
    }

    @Override
    public String toString() {
        return mImpl.toString();
    }

    /**
     * @return uid of the session
     */
    @Override
    public int getUid() {
        return mImpl.getUid();
    }

    /**
     * @return package name
     */
    @Override
    public @NonNull String getPackageName() {
        return mImpl.getPackageName();
    }

    /**
     * @return id
     */
    public String getId() {
        return mImpl.getSessionId();
    }

    /**
     * @return type of the token
     * @see #TYPE_SESSION
     * @see #TYPE_LIBRARY_SESSION
     */
    public @SessionTokenType int getSessionType() {
        return mImpl.getSessionType();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public boolean isLegacy() {
        return mImpl.isLegacy();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public Object getBinder() {
        return mImpl.getBinder();
    }

    /**
     * Create a {@link Bundle} from this token to share it across processes.
     * @return Bundle
     */
    @Override
    public Bundle toBundle() {
        return mImpl.toBundle();
    }

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return SessionToken2 object
     */
    public static SessionToken2 fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int type = bundle.getInt(KEY_TYPE, -1);
        SessionToken2Impl impl = null;
        switch (type) {
            case TYPE_SESSION:
            case TYPE_LIBRARY_SESSION:
                impl = SessionToken2ImplBase.fromBundle(bundle);
                break;
            case TYPE_SESSION_LEGACY:
                impl = SessionToken2ImplLegacy.fromBundle(bundle);
                break;
        }
        return impl != null ? new SessionToken2(impl) : null;
    }

    /**
     * Creates SessionToken2 object from MediaSessionCompat.Token.
     * When the SessionToken2 is ready, OnSessionToken2CreateListner will be called.
     *
     * TODO: Consider to use this in the constructor of MediaController2.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static void createSessionToken2(@NonNull final Context context,
            @NonNull final MediaSessionCompat.Token token, @NonNull final Executor executor,
            @NonNull final OnSessionToken2CreatedListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener shouldn't be null");
        }

        try {
            Bundle token2Bundle = token.getSessionToken2Bundle();
            if (token2Bundle != null) {
                notifySessionToken2Created(executor, listener, token,
                        SessionToken2.fromBundle(token2Bundle));
                return;
            }
            final MediaControllerCompat controller = new MediaControllerCompat(context, token);
            final int uid = getUid(context.getPackageManager(), controller.getPackageName());
            final SessionToken2 token2ForLegacySession = new SessionToken2(
                    new SessionToken2ImplLegacy(token, controller.getPackageName(), uid));

            final HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            final Handler handler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    synchronized (listener) {
                        if (msg.what == MSG_SEND_TOKEN2_FOR_LEGACY_SESSION) {
                            // token for framework session.
                            controller.unregisterCallback((MediaControllerCompat.Callback) msg.obj);
                            token.setSessionToken2Bundle(token2ForLegacySession.toBundle());
                            notifySessionToken2Created(executor, listener, token,
                                    token2ForLegacySession);
                            if (Build.VERSION.SDK_INT >= 18) {
                                thread.quitSafely();
                            } else {
                                thread.quit();
                            }
                        }
                    }
                }
            };
            MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
                @Override
                public void onSessionReady() {
                    synchronized (listener) {
                        handler.removeMessages(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION);
                        controller.unregisterCallback(this);
                        if (token.getSessionToken2Bundle() == null) {
                            token.setSessionToken2Bundle(token2ForLegacySession.toBundle());
                        }
                        notifySessionToken2Created(executor, listener, token,
                                SessionToken2.fromBundle(token.getSessionToken2Bundle()));
                        if (Build.VERSION.SDK_INT >= 18) {
                            thread.quitSafely();
                        } else {
                            thread.quit();
                        }
                    }
                }
            };
            synchronized (listener) {
                controller.registerCallback(callback, handler);
                Message msg = handler.obtainMessage(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION, callback);
                handler.sendMessageDelayed(msg, WAIT_TIME_MS_FOR_SESSION_READY);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to create session token2.", e);
        }
    }


    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void notifySessionToken2Created(final Executor executor,
            final OnSessionToken2CreatedListener listener, final MediaSessionCompat.Token token,
            final SessionToken2 token2) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onSessionToken2Created(token, token2);
            }
        });
    }

    /**
     * @hide
     * Interface definition of a listener to be invoked when a {@link SessionToken2 token2} object
     * is created from a {@link MediaSessionCompat.Token compat token}.
     *
     * @see #createSessionToken2
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnSessionToken2CreatedListener {
        /**
         * Called when SessionToken2 object is created.
         *
         * @param token the compat token used for creating {@code token2}
         * @param token2 the created SessionToken2 object
         */
        void onSessionToken2Created(MediaSessionCompat.Token token, SessionToken2 token2);
    }

    interface SessionToken2Impl extends Token2Impl{
        String getSessionId();
        @SessionTokenType int getSessionType();
        Object getBinder();
    }

    private static class SessionToken2ImplBase implements SessionToken2Impl {
        private final int mUid;
        private final @SessionTokenType int mType;
        private final String mPackageName;
        private final String mSessionId;
        private final IMediaSession2 mISession2;

        SessionToken2ImplBase(int uid, @SessionTokenType int type, String packageName,
                String sessionId, IMediaSession2 iSession2) {
            mUid = uid;
            mType = type;
            mPackageName = packageName;
            mSessionId = sessionId;
            mISession2 = iSession2;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public String getPackageName() {
            return mPackageName;
        }

        @Override
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_UID, mUid);
            bundle.putString(KEY_PACKAGE_NAME, mPackageName);
            bundle.putString(KEY_SESSION_ID, mSessionId);
            bundle.putInt(KEY_TYPE, mType);
            BundleCompat.putBinder(bundle, KEY_SESSION_BINDER, mISession2.asBinder());
            return bundle;
        }

        public static SessionToken2ImplBase fromBundle(Bundle bundle) {
            int uid = bundle.getInt(KEY_UID, UID_UNKNOWN);
            String packageName = bundle.getString(KEY_PACKAGE_NAME);
            if (TextUtils.isEmpty(packageName)) {
                return null;
            }
            String sessionId = bundle.getString(KEY_SESSION_ID);
            if (sessionId == null) {
                return null;
            }
            int type = bundle.getInt(KEY_TYPE, -1);
            IMediaSession2 iSession = IMediaSession2.Stub.asInterface(
                    BundleCompat.getBinder(bundle, KEY_SESSION_BINDER));
            if (iSession == null) {
                return null;
            }
            return new SessionToken2ImplBase(uid, type, packageName, sessionId, iSession);
        }

        @Override
        public boolean isLegacy() {
            return false;
        }

        @Override
        public String getSessionId() {
            return mSessionId;
        }

        @Override
        public int getSessionType() {
            return mType;
        }

        @Override
        public Object getBinder() {
            return mISession2.asBinder();
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mUid, mType, mPackageName, mSessionId, mISession2.asBinder());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SessionToken2ImplBase)) {
                return false;
            }
            SessionToken2ImplBase other = (SessionToken2ImplBase) obj;
            return mUid == other.mUid
                    && TextUtils.equals(mPackageName, other.mPackageName)
                    && TextUtils.equals(mSessionId, other.mSessionId)
                    && mType == other.mType
                    && ObjectsCompat.equals(mISession2.asBinder(), other.mISession2.asBinder());
        }

        @Override
        public String toString() {
            return "SessionToken {pkg=" + mPackageName + ", id=" + mSessionId + ", type=" + mType
                    + ", IMediaSession2=" + mISession2.asBinder() + "}";
        }
    }

    private static class SessionToken2ImplLegacy implements SessionToken2Impl {
        private final MediaSessionCompat.Token mLegacyToken;
        private final int mUid;
        private final String mPackageName;

        SessionToken2ImplLegacy(MediaSessionCompat.Token token, String packageName, int uid) {
            if (token == null) {
                throw new IllegalArgumentException("token shouldn't be null.");
            }
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException("packageName shouldn't be null.");
            }

            mLegacyToken = token;
            mUid = uid;
            mPackageName = packageName;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public String getPackageName() {
            return mPackageName;
        }

        @Override
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_TYPE, TYPE_SESSION_LEGACY);
            bundle.putBundle(KEY_TOKEN_LEGACY, mLegacyToken.toBundle());
            bundle.putInt(KEY_UID, mUid);
            bundle.putString(KEY_PACKAGE_NAME, mPackageName);
            return bundle;
        }

        public static SessionToken2ImplLegacy fromBundle(Bundle bundle) {
            Bundle tokenBundle = bundle.getBundle(KEY_TOKEN_LEGACY);
            if (tokenBundle == null) {
                return null;
            }
            MediaSessionCompat.Token token = MediaSessionCompat.Token.fromBundle(tokenBundle);
            if (token == null) {
                return null;
            }
            int uid = bundle.getInt(KEY_UID);
            String packageName = bundle.getString(KEY_PACKAGE_NAME);
            return new SessionToken2ImplLegacy(token, packageName, uid);
        }

        @Override
        public boolean isLegacy() {
            return true;
        }

        @Override
        public String getSessionId() {
            // TODO: Fill here
            return "";
        }

        @Override
        public Object getBinder() {
            return mLegacyToken;
        }

        @Override
        public int getSessionType() {
            return TYPE_SESSION_LEGACY;
        }

        @Override
        public int hashCode() {
            return mLegacyToken.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj instanceof SessionToken2ImplLegacy)) {
                return false;
            }
            SessionToken2ImplLegacy other = (SessionToken2ImplLegacy) obj;
            return mUid == other.mUid
                    && ObjectsCompat.equals(mLegacyToken, other.mLegacyToken);
        }

        @Override
        public String toString() {
            return "SessionToken2 {legacyToken=" + mLegacyToken + "}";
        }
    }
}
