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

import static androidx.media2.MediaPlayerConnector.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.MediaPlayerConnector.PLAYER_STATE_IDLE;
import static androidx.media2.MediaSession2.ControllerCb;
import static androidx.media2.MediaSession2.ControllerInfo;
import static androidx.media2.MediaSession2.OnDataSourceMissingHelper;
import static androidx.media2.MediaSession2.SessionCallback;
import static androidx.media2.SessionToken2.TYPE_SESSION;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaPlayerConnector.PlayerEventCallback;
import androidx.media2.MediaPlaylistAgent.PlaylistEventCallback;
import androidx.media2.MediaSession2.ErrorCode;
import androidx.media2.MediaSession2.MediaSession2Impl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

class MediaSession2ImplBase implements MediaSession2Impl {
    private static final String DEFAULT_MEDIA_SESSION_TAG_PREFIX = "android.media.session2.id";
    private static final String DEFAULT_MEDIA_SESSION_TAG_DELIM = ".";

    static final String TAG = "MS2ImplBase";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSession2Stub mSession2Stub;
    private final MediaSessionLegacyStub mSessionLegacyStub;
    private final Executor mCallbackExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SessionCallback mCallback;
    private final String mSessionId;
    private final SessionToken2 mSessionToken;
    private final AudioManager mAudioManager;
    private final SessionPlayer2.PlayerCallback mPlayerCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AudioFocusHandler mAudioFocusHandler;
    private final MediaSession2 mInstance;
    private final PendingIntent mSessionActivity;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PlaybackInfo mPlaybackInfo;

    @GuardedBy("mLock")
    private SessionPlayer2 mPlayer;
    @GuardedBy("mLock")
    private MediaBrowserServiceCompat mBrowserServiceLegacyStub;

    MediaSession2ImplBase(MediaSession2 instance, Context context, String id,
            SessionPlayer2 player, PendingIntent sessionActivity, Executor callbackExecutor,
            SessionCallback callback) {
        mContext = context;
        mInstance = instance;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mSession2Stub = new MediaSession2Stub(this);
        mSessionActivity = sessionActivity;

        mCallback = callback;
        mCallbackExecutor = callbackExecutor;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mPlayerCallback = new SessionPlayerCallback(this);
        mAudioFocusHandler = new AudioFocusHandler(context, instance);

        mSessionId = id;
        mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(),
                TYPE_SESSION, context.getPackageName(), mSession2Stub));
        String sessionCompatId = TextUtils.join(DEFAULT_MEDIA_SESSION_TAG_DELIM,
                new String[] {DEFAULT_MEDIA_SESSION_TAG_PREFIX, id});

        mSessionCompat = new MediaSessionCompat(context, sessionCompatId, mSessionToken);
        // NOTE: mSessionLegacyStub should be created after mSessionCompat created.
        mSessionLegacyStub = new MediaSessionLegacyStub(this);

        mSessionCompat.setSessionActivity(sessionActivity);
        mSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        updatePlayer(player);
        // Do followings at the last moment. Otherwise commands through framework would be sent to
        // this session while initializing, and end up with unexpected situation.
        mSessionCompat.setCallback(mSessionLegacyStub, mHandler);
        mSessionCompat.setActive(true);
    }

    @Override
    public void updatePlayer(@NonNull MediaPlayerConnector player,
            @Nullable MediaPlaylistAgent playlistAgent) {
    }

    @Override
    public void updatePlayer(@NonNull SessionPlayer2 player) {
        if (player == null) {
            throw new IllegalArgumentException("player shouldn't be null");
        }

        synchronized (mLock) {
            if (player == mPlayer) {
                return;
            }
        }

        final boolean isPlaybackInfoChanged;

        final SessionPlayer2 oldPlayer;
        final PlaybackInfo info = createPlaybackInfo(player);

        synchronized (mLock) {
            isPlaybackInfoChanged = !info.equals(mPlaybackInfo);

            oldPlayer = mPlayer;
            mPlayer = player;
            mPlaybackInfo = info;

            if (oldPlayer != mPlayer) {
                if (oldPlayer != null) {
                    oldPlayer.unregisterPlayerCallback(mPlayerCallback);
                }
                mPlayer.registerPlayerCallback(mCallbackExecutor, mPlayerCallback);
            }
        }

        if (oldPlayer == null) {
            // updatePlayerConnector() is called inside of the constructor.
            // There's no connected controllers at this moment, so just initialize session compat's
            // playback state. Otherwise, framework doesn't know whether this is ready to receive
            // media key event.
            mSessionCompat.setPlaybackState(createPlaybackStateCompat());
        } else {
            if (player != oldPlayer) {
                final int state = getPlayerState();
                mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onPlayerStateChanged(getInstance(), state);
                    }
                });
                notifyPlayerUpdatedNotLocked(oldPlayer);
            }
            if (isPlaybackInfoChanged) {
                // Currently hasPlaybackInfo is always true, but check this in case that we're
                // adding PlaybackInfo#equals().
                notifyPlaybackInfoChangedNotLocked(info);
            }
        }

        if (player instanceof RemoteSessionPlayer2) {
            final RemoteSessionPlayer2 remotePlayer = (RemoteSessionPlayer2) player;
            VolumeProviderCompat volumeProvider =
                    new VolumeProviderCompat(remotePlayer.getPlayerVolumeControlType(),
                            remotePlayer.getMaxPlayerVolume(),
                            remotePlayer.getPlayerVolume()) {
                        @Override
                        public void onSetVolumeTo(int volume) {
                            remotePlayer.setPlayerVolume(volume);
                        }

                        @Override
                        public void onAdjustVolume(int direction) {
                            remotePlayer.adjustPlayerVolume(direction);
                        }
                    };
            mSessionCompat.setPlaybackToRemote(volumeProvider);
        } else {
            int stream = getLegacyStreamType(player.getAudioAttributes());
            mSessionCompat.setPlaybackToLocal(stream);
        }
    }

    @NonNull PlaybackInfo createPlaybackInfo(@NonNull SessionPlayer2 player) {
        final AudioAttributesCompat attrs = player.getAudioAttributes();

        if (!(player instanceof RemoteSessionPlayer2)) {
            int stream = getLegacyStreamType(attrs);
            int controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
            if (Build.VERSION.SDK_INT >= 21 && mAudioManager.isVolumeFixed()) {
                controlType = VolumeProviderCompat.VOLUME_CONTROL_FIXED;
            }
            return PlaybackInfo.createPlaybackInfo(
                    PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                    attrs,
                    controlType,
                    mAudioManager.getStreamMaxVolume(stream),
                    mAudioManager.getStreamVolume(stream));
        } else {
            RemoteSessionPlayer2 remotePlayer = (RemoteSessionPlayer2) player;
            return PlaybackInfo.createPlaybackInfo(
                    PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                    attrs,
                    remotePlayer.getPlayerVolumeControlType(),
                    remotePlayer.getMaxPlayerVolume(),
                    remotePlayer.getPlayerVolume());
        }
    }

    private int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        int stream;
        if (attrs == null) {
            stream = AudioManager.STREAM_MUSIC;
        } else {
            stream = attrs.getLegacyStreamType();
            if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
                // Usually, AudioAttributesCompat#getLegacyStreamType() does not return
                // USE_DEFAULT_STREAM_TYPE unless the developer sets it with
                // AudioAttributesCompat.Builder#setLegacyStreamType().
                // But for safety, let's convert USE_DEFAULT_STREAM_TYPE to STREAM_MUSIC here.
                stream = AudioManager.STREAM_MUSIC;
            }
        }
        return stream;
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (mPlayer == null) {
                return;
            }
            mAudioFocusHandler.close();
            mPlayer.unregisterPlayerCallback(mPlayerCallback);
            mPlayer = null;
            mSessionCompat.release();
            mCallback.onSessionClosed(mInstance);
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onDisconnected();
                }
            });
            mHandler.removeCallbacksAndMessages(null);
            if (mHandlerThread.isAlive()) {
                if (Build.VERSION.SDK_INT >= 18) {
                    mHandlerThread.quitSafely();
                } else {
                    mHandlerThread.quit();
                }
            }
        }
    }

    @Override
    public @Nullable SessionPlayer2 getPlayer() {
        synchronized (mLock) {
            return mPlayer;
        }
    }

    @Override
    public @Nullable MediaPlayerConnector getPlayerConnector() {
        return null;
    }

    @Override
    public @NonNull MediaPlaylistAgent getPlaylistAgent() {
        return null;
    }

    @Override
    public String getId() {
        return mSessionId;
    }

    @Override
    public @NonNull SessionToken2 getToken() {
        return mSessionToken;
    }

    @Override
    public @NonNull List<ControllerInfo> getConnectedControllers() {
        List<ControllerInfo> controllers = new ArrayList<>();
        controllers.addAll(mSession2Stub.getConnectedControllersManager()
                .getConnectedControllers());
        controllers.addAll(mSessionLegacyStub.getConnectedControllersManager()
                .getConnectedControllers());
        return controllers;
    }

    @Override
    public boolean isConnected(ControllerInfo controller) {
        if (controller.equals(mSessionLegacyStub.getControllersForAll())) {
            return true;
        }
        return mSession2Stub.getConnectedControllersManager().isConnected(controller)
                || mSessionLegacyStub.getConnectedControllersManager().isConnected(controller);
    }

    @Override
    public void setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull final List<MediaSession2.CommandButton> layout) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (layout == null) {
            throw new IllegalArgumentException("layout shouldn't be null");
        }
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onCustomLayoutChanged(layout);
            }
        });
    }

    @Override
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull final SessionCommandGroup2 commands) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (commands == null) {
            throw new IllegalArgumentException("commands shouldn't be null");
        }

        if (mSession2Stub.getConnectedControllersManager().isConnected(controller)) {
            mSession2Stub.getConnectedControllersManager()
                    .updateAllowedCommands(controller, commands);
            notifyToController(controller, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onAllowedCommandsChanged(commands);
                }
            });
        } else {
            mSessionLegacyStub.getConnectedControllersManager()
                    .updateAllowedCommands(controller, commands);
        }
    }

    @Override
    public void sendCustomCommand(@NonNull final SessionCommand2 command,
            @Nullable final Bundle args) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onCustomCommand(command, args, null);
            }
        });
    }

    @Override
    public void sendCustomCommand(@NonNull ControllerInfo controller,
            @NonNull final SessionCommand2 command, @Nullable final Bundle args,
            @Nullable final ResultReceiver receiver) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onCustomCommand(command, args, receiver);
            }
        });
    }

    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void prepare() {
    }

    @Override
    public void seekTo(long pos) {
    }

    @Override
    public void skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
    }

    @Override
    public void skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
    }

    @Override
    public void notifyError(@ErrorCode final int errorCode, @Nullable final Bundle extras) {
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onError(errorCode, extras);
            }
        });
    }

    @Override
    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
            @Nullable final List<Bundle> routes) {
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onRoutesInfoChanged(routes);
            }
        });
    }

    @Override
    public @MediaPlayerConnector.PlayerState int getPlayerState() {
        return MediaPlayerConnector.PLAYER_STATE_ERROR;
    }

    @Override
    public long getCurrentPosition() {
        return MediaPlayerConnector.UNKNOWN_TIME;
    }

    @Override
    public long getDuration() {
        return MediaPlayerConnector.UNKNOWN_TIME;
    }

    @Override
    public long getBufferedPosition() {
        return MediaPlayerConnector.UNKNOWN_TIME;
    }

    @Override
    public @MediaPlayerConnector.BuffState int getBufferingState() {
        return BUFFERING_STATE_UNKNOWN;
    }

    @Override
    public float getPlaybackSpeed() {
        return 1.0f;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
    }

    @Override
    public void setOnDataSourceMissingHelper(
            @NonNull OnDataSourceMissingHelper helper) {
    }

    @Override
    public void clearOnDataSourceMissingHelper() {
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        return null;
    }

    private @Nullable List<DataSourceDesc2> getPlaylistOrNull() {
        final SessionPlayer2 player;
        synchronized (mLock) {
            player = mPlayer;
        }
        return player != null ? player.getPlaylist() : null;
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
    }

    @Override
    public void skipToPreviousItem() {
    }

    @Override
    public void skipToNextItem() {
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return null;
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return null;
    }

    private @Nullable DataSourceDesc2 getCurrentMediaItemOrNull() {
        final SessionPlayer2 player;
        synchronized (mLock) {
            player = mPlayer;
        }
        return player != null ? player.getCurrentMediaItem() : null;
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
    }

    @Override
    public @MediaPlaylistAgent.RepeatMode int getRepeatMode() {
        return MediaPlaylistAgent.REPEAT_MODE_NONE;
    }

    @Override
    public void setRepeatMode(@MediaPlaylistAgent.RepeatMode int repeatMode) {
    }

    @Override
    public @MediaPlaylistAgent.ShuffleMode int getShuffleMode() {
        return MediaPlaylistAgent.SHUFFLE_MODE_NONE;
    }

    @Override
    public void setShuffleMode(int shuffleMode) {
    }

    ///////////////////////////////////////////////////
    // package private and private methods
    ///////////////////////////////////////////////////
    @Override
    public @NonNull MediaSession2 getInstance() {
        return mInstance;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Override
    public SessionCallback getCallback() {
        return mCallback;
    }

    @Override
    public MediaSessionCompat getSessionCompat() {
        return mSessionCompat;
    }

    @Override
    public AudioFocusHandler getAudioFocusHandler() {
        return mAudioFocusHandler;
    }

    @Override
    public boolean isClosed() {
        return !mHandlerThread.isAlive();
    }

    @Override
    public PlaybackStateCompat createPlaybackStateCompat() {
        synchronized (mLock) {
            int state = MediaUtils2.convertToPlaybackStateCompatState(getPlayerState(),
                    getBufferingState());
            long allActions = PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_REWIND
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_FAST_FORWARD
                    | PlaybackStateCompat.ACTION_SET_RATING
                    | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                    | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                    | PlaybackStateCompat.ACTION_PLAY_FROM_URI | PlaybackStateCompat.ACTION_PREPARE
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                    | PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                    | PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                    | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                    | PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
            return new PlaybackStateCompat.Builder()
                    .setState(state, getCurrentPosition(), getPlaybackSpeed(),
                            SystemClock.elapsedRealtime())
                    .setActions(allActions)
                    .setBufferedPosition(getBufferedPosition())
                    .build();
        }
    }

    @Override
    public PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            return mPlaybackInfo;
        }
    }

    @Override
    public PendingIntent getSessionActivity() {
        return mSessionActivity;
    }

    MediaBrowserServiceCompat createLegacyBrowserService(Context context, SessionToken2 token,
            Token sessionToken) {
        return new MediaSessionService2LegacyStub(context, this, sessionToken);
    }

    @Override
    public void connectFromService(IMediaController2 caller, String packageName, int pid, int uid) {
        mSession2Stub.connect(caller, packageName, pid, uid);
    }

    /**
     * Gets the service binder from the MediaBrowserServiceCompat. Should be only called by the
     * thread with a Looper.
     *
     * @return
     */
    @Override
    public IBinder getLegacyBrowserServiceBinder() {
        MediaBrowserServiceCompat legacyStub;
        synchronized (mLock) {
            if (mBrowserServiceLegacyStub == null) {
                mBrowserServiceLegacyStub = createLegacyBrowserService(mContext, mSessionToken,
                        mSessionCompat.getSessionToken());
            }
            legacyStub = mBrowserServiceLegacyStub;
        }
        Intent intent = new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE);
        return legacyStub.onBind(intent);
    }

    MediaBrowserServiceCompat getLegacyBrowserService() {
        synchronized (mLock) {
            return mBrowserServiceLegacyStub;
        }
    }

    private boolean isInPlaybackState(@Nullable MediaPlayerConnector player) {
        return player != null
                && player.getPlayerState() != MediaPlayerConnector.PLAYER_STATE_IDLE
                && player.getPlayerState() != MediaPlayerConnector.PLAYER_STATE_ERROR;
    }

    private void notifyPlayerUpdatedNotLocked(SessionPlayer2 oldPlayer) {
        // Tells the playlist change first, to current item can change be notified with an item
        // within the playlist.
        List<DataSourceDesc2> oldPlaylist = oldPlayer.getPlaylist();
        final List<DataSourceDesc2> newPlaylist = getPlaylistOrNull();
        if (!ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(
                            newPlaylist, getPlaylistMetadata());
                }
            });
        } else {
            MediaMetadata2 oldMetadata = oldPlayer.getPlaylistMetadata();
            final MediaMetadata2 newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                notifyToAllControllers(new NotifyRunnable() {
                    @Override
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        }
        DataSourceDesc2 oldCurrentItem = oldPlayer.getCurrentMediaItem();
        final DataSourceDesc2 newCurrentItem = getCurrentMediaItemOrNull();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final int repeatMode = getRepeatMode();
        if (oldPlayer.getRepeatMode() != repeatMode) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final int shuffleMode = getShuffleMode();
        if (oldPlayer.getShuffleMode() != shuffleMode) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }

        // Always forcefully send the player state and buffered state to send the current position
        // and buffered position.
        final long currentTimeMs = SystemClock.elapsedRealtime();
        final long positionMs = getCurrentPosition();
        final int playerState = getPlayerState();
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlayerStateChanged(currentTimeMs, positionMs, playerState);
            }
        });
        final DataSourceDesc2 item = getCurrentMediaItemOrNull();
        if (item != null) {
            final int bufferingState = getBufferingState();
            final long bufferedPositionMs = getBufferedPosition();
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, bufferingState, bufferedPositionMs);
                }
            });
        }
        final float speed = getPlaybackSpeed();
        if (speed != oldPlayer.getPlaybackSpeed()) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(currentTimeMs, positionMs, speed);
                }
            });
        }
        // Note: AudioInfo is updated outside of this API.
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyPlaybackInfoChangedNotLocked(final PlaybackInfo info) {
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlaybackInfoChanged(info);
            }
        });
    }

    void notifyToController(@NonNull final ControllerInfo controller,
            @NonNull NotifyRunnable runnable) {
        if (controller == null) {
            return;
        }
        if (!isConnected(controller)) {
            // Do not send command to an unconnected controller.
            return;
        }

        try {
            runnable.run(controller.getControllerCb());
        } catch (DeadObjectException e) {
            if (DEBUG) {
                Log.d(TAG, controller.toString() + " is gone", e);
            }
            // Note: Only removing from MediaSession2Stub would be fine for now, because other
            //       (legacy) stubs wouldn't throw DeadObjectException.
            mSession2Stub.getConnectedControllersManager().removeController(controller);
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    void notifyToAllControllers(@NonNull NotifyRunnable runnable) {
        List<ControllerInfo> controllers =
                mSession2Stub.getConnectedControllersManager().getConnectedControllers();
        for (int i = 0; i < controllers.size(); i++) {
            notifyToController(controllers.get(i), runnable);
        }
        ControllerInfo controller = mSessionLegacyStub.getControllersForAll();
        notifyToController(controller, runnable);
    }

    ///////////////////////////////////////////////////
    // Inner classes
    ///////////////////////////////////////////////////
    @FunctionalInterface
    interface NotifyRunnable {
        void run(ControllerCb callback) throws RemoteException;
    }

    private static class SessionPlayerCallback extends SessionPlayer2.PlayerCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        SessionPlayerCallback(MediaSession2ImplBase session) {
            mSession = new WeakReference<>(session);
        }

        @Override
        public void onCurrentMediaItemChanged(final SessionPlayer2 player,
                final DataSourceDesc2 item) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(item);
                }
            });
        }

        @Override
        public void onPlayerStateChanged(final SessionPlayer2 player, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session == null) {
                return;
            }
            session.getCallback().onPlayerStateChanged(session.getInstance(), state);
            session.notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlayerStateChanged(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), state);
                }
            });
        }

        @Override
        public void onBufferingStateChanged(final SessionPlayer2 player,
                final DataSourceDesc2 item, final int state) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, state, player.getBufferedPosition());
                }
            });
        }

        @Override
        public void onPlaybackSpeedChanged(final SessionPlayer2 player, final float speed) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), speed);
                }
            });
        }

        @Override
        public void onSeekCompleted(final SessionPlayer2 player, final long position) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onSeekCompleted(SystemClock.elapsedRealtime(),
                            player.getCurrentPosition(), position);
                }
            });
        }

        @Override
        public void onPlaylistChanged(final SessionPlayer2 player, final List<DataSourceDesc2> list,
                final MediaMetadata2 metadata) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(list, metadata);
                }
            });
        }

        @Override
        public void onPlaylistMetadataChanged(final SessionPlayer2 player,
                final MediaMetadata2 metadata) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onRepeatModeChanged(final SessionPlayer2 player, final int repeatMode) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(repeatMode);
                }
            });
        }

        @Override
        public void onShuffleModeChanged(final SessionPlayer2 player, final int shuffleMode) {
            notifyToAllControllers(player, new NotifyRunnable() {
                @Override
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }

        private MediaSession2ImplBase getSession() {
            final MediaSession2ImplBase session = mSession.get();
            if (session == null && DEBUG) {
                Log.d(TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        private void notifyToAllControllers(@NonNull SessionPlayer2 player,
                @NonNull NotifyRunnable runnable) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || session.getPlayer() != player || player == null) {
                return;
            }
            session.notifyToAllControllers(runnable);
        }
    }
}
