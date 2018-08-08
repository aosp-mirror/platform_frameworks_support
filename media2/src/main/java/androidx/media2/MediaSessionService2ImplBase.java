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

import android.app.Notification;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaSessionService2.MediaNotification;
import androidx.media2.MediaSessionService2.MediaSessionService2Impl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2Impl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    MediaSessionService2Stub mStub;
    @GuardedBy("mLock")
    MediaSessionService2 mService;
    @GuardedBy("mLock")
    MainHandlerExecutor mExecutor;
    @GuardedBy("mLock")
    private Map<String, MediaSession2> mSessions = new ArrayMap<>();

    MediaSessionService2ImplBase() {
    }

    @Override
    public void onCreate(MediaSessionService2 service) {
        synchronized (mLock) {
            mService = service;
            mStub = new MediaSessionService2Stub(this);
            mExecutor = new MainHandlerExecutor(service);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        final MediaSessionService2 service = mService;
        if (service == null) {
            Log.w(TAG, "Service hasn't created before onBind()");
            return null;
        }
        switch (intent.getAction()) {
            case MediaSessionService2.SERVICE_INTERFACE: {
                final MediaSessionService2Stub stub = mStub;
                if (stub == null) {
                    return null;
                }
                return stub.asBinder();
            }
            case MediaBrowserServiceCompat.SERVICE_INTERFACE: {
                final MediaSession2 session = service.onGetSession();
                addSession(session);
                // Return a specific session's legacy binder although the Android framework caches
                // the returned binder here and next binding request may reuse cached binder even
                // after the session is closed.
                // Disclaimer: Although MediaBrowserCompat can only get the session that initially
                // set, it doesn't make things bad. Such limitation had been there between
                // MediaBrowserCompat and MediaBrowserServiceCompat.
                return session.getLegacyBrowerServiceBinder();
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        synchronized (mLock) {
            if (mExecutor != null) {
                mExecutor.clear();
                mExecutor = null;
            }
            mService = null;
            if (mStub != null) {
                mStub.close();
                mStub = null;
            }
        }
    }

    @Override
    public void addSession(final MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        final MediaSession2 old;
        synchronized (mLock) {
            old = mSessions.get(session.getId());
            if (old != null && old != session) {
                // TODO(b/112114183): Also check the uniqueness before sessions're returned by
                //                    onGetSession.
                throw new IllegalArgumentException("Session ID should be unique.");
            }
            mSessions.put(session.getId(), session);
        }
        if (old == null) {
            // TODO: Check whether the session is registered in multiple sessions.
            session.getCallback().setOnHandleForegroundServiceCallback(
                    new MediaSession2.SessionCallback.OnHandleForegroundServiceCallback() {
                        @Override
                        public void onPlayerStateChanged(int state) {
                            final MediaSessionService2 service = mService;
                            if (service == null) {
                                return;
                            }
                            if (state == MediaPlayerConnector.PLAYER_STATE_IDLE
                                    || state == MediaPlayerConnector.PLAYER_STATE_ERROR) {
                                service.stopForeground(false /* removeNotification */);
                                return;
                            }
                            // state is PLAYER_STATE_PLAYING or PLAYER_STATE_PAUSE.
                            MediaNotification mediaNotification =
                                    service.onUpdateNotification(session);
                            if (mediaNotification == null) {
                                return;
                            }

                            int notificationId = mediaNotification.getNotificationId();
                            Notification notification = mediaNotification.getNotification();

                            NotificationManagerCompat manager =
                                    NotificationManagerCompat.from(service);
                            manager.notify(notificationId, notification);
                            service.startForeground(notificationId, notification);
                        }

                        @Override
                        public void onSessionClosed() {
                            removeSession(session);
                        }
                    });
        }
    }

    @Override
    public void removeSession(MediaSession2 session) {
        if (session == null) {
            throw new IllegalArgumentException("session shouldn't be null");
        }
        synchronized (mLock) {
            mSessions.remove(session.getId());
        }
    }

    @Override
    public MediaNotification onUpdateNotification(MediaSession2 session) {
        // May supply default implementation later
        return null;
    }

    @Override
    public List<MediaSession2> getSessions() {
        List<MediaSession2> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mSessions.values());
        }
        return list;
    }

    private static final class MediaSessionService2Stub extends IMediaSessionService2.Stub
            implements AutoCloseable {
        private final WeakReference<MediaSessionService2ImplBase> mServiceImpl;

        MediaSessionService2Stub(final MediaSessionService2ImplBase serviceImpl) {
            mServiceImpl = new WeakReference<>(serviceImpl);
        }

        @Override
        public void connect(final IMediaController2 caller, final String packageName) {
            final MediaSessionService2ImplBase serviceImpl = mServiceImpl.get();
            if (serviceImpl == null) {
                if (DEBUG) {
                    Log.d(TAG, "ServiceImpl isn't available");
                }
                return;
            }
            final Executor executor = serviceImpl.mExecutor;
            if (executor == null) {
                if (DEBUG) {
                    Log.d(TAG, "ServiceImpl.mExecutor isn't available");
                }
                return;
            }
            final int pid = Binder.getCallingPid();
            final int uid = Binder.getCallingUid();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    boolean shouldNotifyDisconnected = true;
                    try {
                        final MediaSessionService2ImplBase serviceImpl = mServiceImpl.get();
                        if (serviceImpl == null) {
                            if (DEBUG) {
                                Log.d(TAG, "ServiceImpl isn't available");
                            }
                            return;
                        }
                        final MediaSessionService2 service = serviceImpl.mService;
                        if (service == null) {
                            if (DEBUG) {
                                Log.d(TAG, "Service isn't available");
                            }
                            return;
                        }
                        final MediaSession2 session;
                        try {
                            session = service.onGetSession();
                            service.addSession(session);
                            shouldNotifyDisconnected = false;

                            session.connectFromService(caller, packageName, pid, uid);
                        } catch (Exception e) {
                            // Don't propagate exception in service to the controller.
                            Log.w(TAG, "Failed to add a session to session service", e);
                        }
                    } finally {
                        // Trick to call onDisconnected() in one place.
                        if (shouldNotifyDisconnected) {
                            if (DEBUG) {
                                Log.d(TAG, "Service has destroyed prematurely."
                                        + " Rejecting connection");
                            }
                            try {
                                caller.onDisconnected();
                            } catch (RemoteException e) {
                                // Controller may be died prematurely.
                                // Not an issue because we'll ignore it anyway.
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void close() {
            mServiceImpl.clear();
        }
    }
}
