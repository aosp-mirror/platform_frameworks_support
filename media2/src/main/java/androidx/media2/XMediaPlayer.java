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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.concurrent.listenablefuture.ListenableFuture;
import androidx.concurrent.listenablefuture.SettableFuture;
import androidx.media.AudioAttributesCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @hide
 */
@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(LIBRARY)
public class XMediaPlayer extends SessionPlayer2 {
    private static final String TAG = "XMediaPlayer";

    private static final int END_OF_PLAYLIST = -1;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sResultCodeMap;

    static {
        sResultCodeMap = new androidx.collection.ArrayMap<>();
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_NO_ERROR, RESULT_CODE_NO_ERROR);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN, RESULT_CODE_ERROR_UNKNOWN);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_INVALID_OPERATION, RESULT_CODE_INVALID_OPERATION);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_BAD_VALUE, RESULT_CODE_BAD_VALUE);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_PERMISSION_DENIED, RESULT_CODE_PERMISSION_DENIED);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_IO, RESULT_CODE_ERROR_IO);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_SKIPPED, RESULT_CODE_SKIPPED);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaPlayer2 mPlayer;
    private ExecutorService mExecutor;

    /* A list for tracking the commands submitted to MediaPlayer2.*/
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayDeque<Pair<Integer, SettableFuture<CommandResult2>>>
            mCallTypeAndFutures = new ArrayDeque<>();

    private final Object mStateLock = new Object();
    @GuardedBy("mStateLock")
    private @PlayerState int mState;
    @GuardedBy("mStateLock")
    private Map<DataSourceDesc2, Integer> mDSDToBuffStateMap = new HashMap<>();

    private final Object mPlaylistLock = new Object();
    @GuardedBy("mPlaylistLock")
    private ArrayList<DataSourceDesc2> mPlaylist = new ArrayList<>();
    @GuardedBy("mPlaylistLock")
    private ArrayList<DataSourceDesc2> mShuffledList = new ArrayList<>();
    @GuardedBy("mPlaylistLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mPlaylistLock")
    private int mRepeatMode;
    @GuardedBy("mPlaylistLock")
    private int mShuffleMode;
    @GuardedBy("mPlaylistLock")
    private int mCurrentShuffledIdx;

    public XMediaPlayer(Context context) {
        mState = PLAYER_STATE_IDLE;
        mPlayer = MediaPlayer2.create(context);
        mExecutor = Executors.newFixedThreadPool(1);
        mPlayer.setEventCallback(mExecutor, new Mp2Callback());
    }

    @Override
    public ListenableFuture<CommandResult2> play() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PLAY, future));
            mPlayer.play();
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> pause() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PAUSE, future));
            mPlayer.pause();
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> prepare() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PREPARE, future));
            mPlayer.prepare();
        }
        // TODO: Changing buffering state is not correct. Think about changing MP2 event APIs for
        // the initial buffering for prepare case.
        setBufferingState(mPlayer.getCurrentDataSource(), BUFFERING_STATE_BUFFERING_AND_STARVED);
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> seekTo(long position) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO, future));
            mPlayer.seekTo(position);
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaybackSpeed(float playbackSpeed) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(
                    MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS, future));
            mPlayer.setPlaybackParams(new PlaybackParams2.Builder(
                    mPlayer.getPlaybackParams().getPlaybackParams())
                    .setSpeed(playbackSpeed).build());
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> setAudioAttributes(AudioAttributesCompat attr) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(
                    MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES, future));
            mPlayer.setAudioAttributes(attr);
        }
        return future;
    }

    @Override
    public int getPlayerState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    @Override
    public long getCurrentPosition() {
        try {
            return mPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return MediaPlayerConnector.UNKNOWN_TIME;
        }
    }

    @Override
    public long getDuration() {
        try {
            return mPlayer.getDuration();
        } catch (IllegalStateException e) {
            return MediaPlayerConnector.UNKNOWN_TIME;
        }
    }

    @Override
    public long getBufferedPosition() {
        try {
            return mPlayer.getBufferedPosition();
        } catch (IllegalStateException e) {
            return MediaPlayerConnector.UNKNOWN_TIME;
        }
    }

    @Override
    public int getBufferingState() {
        Integer buffState;
        synchronized (mStateLock) {
            buffState = mDSDToBuffStateMap.get(mPlayer.getCurrentDataSource());
        }
        return buffState == null ? BUFFERING_STATE_UNKNOWN : buffState;
    }

    @Override
    public float getPlaybackSpeed() {
        try {
            return mPlayer.getPlaybackParams().getSpeed();
        } catch (IllegalStateException e) {
            return 1.0f;
        }
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        try {
            return mPlayer.getAudioAttributes();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaylist(
            List<DataSourceDesc2> list, MediaMetadata2 metadata) {
        DataSourceDesc2 curItem = null;
        DataSourceDesc2 nextItem = null;
        synchronized (mPlaylistLock) {
            mPlaylistMetadata = metadata;
            mPlaylist.clear();
            if (list == null) {
                mCurrentShuffledIdx = END_OF_PLAYLIST;
            } else {
                mPlaylist.addAll(list);
                applyShuffleModeLocked();
                mCurrentShuffledIdx = 0;
                curItem = mShuffledList.get(mCurrentShuffledIdx);
                if (mShuffledList.size() > 1) {
                    nextItem = mShuffledList.get(mCurrentShuffledIdx + 1);
                }
            }
        }
        notifyPlaylistChanged();

        if (nextItem == null) {
            return setMediaItem(curItem);
        }

        final SettableFuture<CommandResult2> future = SettableFuture.create();
        final ListenableFuture<CommandResult2> setMediaItemFuture = setMediaItem(curItem);
        final ListenableFuture<CommandResult2> setNextMediaItemFuture = setNextMediaItem(nextItem);

        setMediaItemFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    CommandResult2 result = setMediaItemFuture.get();
                    if (result.getResultCode() != RESULT_CODE_NO_ERROR) {
                        setNextMediaItemFuture.cancel(true);
                        future.set(result);
                    }
                } catch (Exception e) {
                    setNextMediaItemFuture.cancel(true);
                    future.setException(e);
                }
            }
        }, mExecutor);

        setNextMediaItemFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!future.isDone()) {
                        future.set(setNextMediaItemFuture.get());
                    }
                } catch (Exception e) {
                    if (!future.isDone()) {
                        future.setException(e);
                    }
                }
            }
        }, mExecutor);
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> setMediaItem(DataSourceDesc2 item) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE, future));
            mPlayer.setDataSource(item);
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> addPlaylistItem(int index, DataSourceDesc2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        DataSourceDesc2 curItem;
        synchronized (mPlaylistLock) {
            index = clamp(index, mPlaylist.size());
            int shuffledIdx = index;
            mPlaylist.add(index, item);
            if (mShuffleMode == MediaPlaylistAgent.SHUFFLE_MODE_NONE) {
                mShuffledList.add(index, item);
            } else {
                // Add the item in random position of mShuffledList.
                shuffledIdx = (int) (Math.random() * (mShuffledList.size() + 1));
                mShuffledList.add(shuffledIdx, item);
            }
            curItem = mShuffledList.get(mCurrentShuffledIdx);
        }
        notifyPlaylistChanged();

        SettableFuture<CommandResult2> future = SettableFuture.create();
        future.set(new CommandResult2(RESULT_CODE_NO_ERROR, System.currentTimeMillis(), curItem));
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> removePlaylistItem(DataSourceDesc2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        int removedItemShuffledIdx;
        DataSourceDesc2 curItem;
        synchronized (mPlaylistLock) {
            removedItemShuffledIdx = mShuffledList.indexOf(item);
            if (removedItemShuffledIdx >= 0) {
                mPlaylist.remove(item);
                mShuffledList.remove(removedItemShuffledIdx);
            }
            curItem = mShuffledList.get(mCurrentShuffledIdx);
        }
        ListenableFuture<CommandResult2> future = null;
        if (removedItemShuffledIdx >= 0) {
            notifyPlaylistChanged();
            if (removedItemShuffledIdx == mCurrentShuffledIdx) {
                future = skipToNextItem();
            }
        }

        if (future == null) {
            future = SettableFuture.create();
            ((SettableFuture<CommandResult2>) future).set(
                    new CommandResult2(RESULT_CODE_NO_ERROR, System.currentTimeMillis(), curItem));
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> replacePlaylistItem(int index, DataSourceDesc2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPreviousItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToNextItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPlaylistItem(DataSourceDesc2 desc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> updatePlaylistMetadata(MediaMetadata2 metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> setRepeatMode(int repeatMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> setShuffleMode(int shuffleMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataSourceDesc2> getPlaylist() {
        synchronized (mPlaylistLock) {
            return new ArrayList<>(mPlaylist);
        }
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        synchronized (mPlaylistLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public int getRepeatMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getShuffleMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSourceDesc2 getCurrentMediaItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws Exception {
        mPlayer.close();
        mExecutor.shutdown();
    }

    public void reset() {
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.clear();
            mPlayer.reset();
        }
        synchronized (mStateLock) {
            mState = PLAYER_STATE_IDLE;
            mDSDToBuffStateMap.clear();
        }
    }

    public ListenableFuture<CommandResult2> setSurface(Surface surface) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_SURFACE, future));
            mPlayer.setSurface(surface);
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setState(@PlayerState final int state) {
        boolean needToNotify = false;
        synchronized (mStateLock) {
            if (mState != state) {
                mState = state;
                needToNotify = true;
            }
        }
        if (needToNotify) {
            notifyPlayerCallback(new PlayerCallbackNotifier() {
                @Override
                public void notify(PlayerCallback callback) {
                    callback.onPlayerStateChanged(XMediaPlayer.this, state);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setBufferingState(final DataSourceDesc2 dsd, @BuffState final int state) {
        Integer previousState;
        synchronized (mStateLock) {
            previousState = mDSDToBuffStateMap.put(dsd, state);
        }
        if (previousState == null || previousState.intValue() != state) {
            notifyPlayerCallback(new PlayerCallbackNotifier() {
                @Override
                public void notify(PlayerCallback callback) {
                    callback.onBufferingStateChanged(XMediaPlayer.this, dsd, state);
                }
            });
        }
    }

    void notifyPlayerCallback(final PlayerCallbackNotifier notifier) {
        Map<PlayerCallback, Executor> map = getCallbacks();
        for (final PlayerCallback callback : map.keySet()) {
            map.get(callback).execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(callback);
                }
            });
        }
    }

    private ListenableFuture<CommandResult2> setNextMediaItem(DataSourceDesc2 item) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE, future));
            mPlayer.setNextDataSource(item);
        }
        return future;
    }

    @SuppressWarnings("GuardedBy")
    private void applyShuffleModeLocked() {
        mShuffledList.clear();
        mShuffledList.addAll(mPlaylist);
        if (mShuffleMode == MediaPlaylistAgent.SHUFFLE_MODE_ALL
                || mShuffleMode == MediaPlaylistAgent.SHUFFLE_MODE_GROUP) {
            Collections.shuffle(mShuffledList);
        }
    }

    private void notifyPlaylistChanged() {
        final List<DataSourceDesc2> playlist = getPlaylist();
        final MediaMetadata2 metadata = getPlaylistMetadata();
        for (Map.Entry<PlayerCallback, Executor> entry : getCallbacks().entrySet()) {
            final PlayerCallback callback = entry.getKey();
            entry.getValue().execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistChanged(XMediaPlayer.this, playlist, metadata);
                }
            });
        }
    }

    // Clamps value to [0, size]
    private static int clamp(int value, int size) {
        if (value < 0) {
            return 0;
        }
        return (value > size) ? size : value;
    }

    private interface PlayerCallbackNotifier {
        void notify(SessionPlayer2.PlayerCallback callback);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    class Mp2Callback extends MediaPlayer2.EventCallback {
        @Override
        public void onVideoSizeChanged(
                MediaPlayer2 mp, DataSourceDesc2 dsd, int width, int height) {
            // TODO: extend PlayerCallback to provide this event.
        }

        @Override
        public void onTimedMetaDataAvailable(
                MediaPlayer2 mp, DataSourceDesc2 dsd, TimedMetaData2 data) {
            // TODO: extend PlayerCallback to provide this event.
        }

        @Override
        public void onError(MediaPlayer2 mp, DataSourceDesc2 dsd, int what, int extra) {
            setState(PLAYER_STATE_ERROR);
            setBufferingState(dsd, BUFFERING_STATE_UNKNOWN);
        }

        @Override
        public void onInfo(MediaPlayer2 mp, DataSourceDesc2 dsd, int what, int extra) {
            switch (what) {
                case MediaPlayer2.MEDIA_INFO_BUFFERING_START:
                    setBufferingState(dsd, BUFFERING_STATE_BUFFERING_AND_STARVED);
                    break;
                case MediaPlayer2.MEDIA_INFO_PREPARED:
                case MediaPlayer2.MEDIA_INFO_BUFFERING_END:
                    setBufferingState(dsd, BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                    break;
                case MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE:
                    if (extra /* percent */ >= 100) {
                        setBufferingState(dsd, BUFFERING_STATE_BUFFERING_COMPLETE);
                    }
                    break;
            }
        }

        @Override
        public void onCallCompleted(
                MediaPlayer2 mp, DataSourceDesc2 dsd, int what, int status) {
            Pair<Integer, SettableFuture<CommandResult2>> pair;
            synchronized (mCallTypeAndFutures) {
                pair = mCallTypeAndFutures.pollFirst();
            }
            if (what != pair.first) {
                Log.w(TAG, "Call type does not match. expeced:" + pair.first + " actual:" + what);
                status = MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN;
            }
            if (status == MediaPlayer2.CALL_STATUS_NO_ERROR) {
                switch (what) {
                    case MediaPlayer2.CALL_COMPLETED_PREPARE:
                    case MediaPlayer2.CALL_COMPLETED_PAUSE:
                        setState(PLAYER_STATE_PAUSED);
                        break;
                    case MediaPlayer2.CALL_COMPLETED_PLAY:
                        setState(PLAYER_STATE_PLAYING);
                        break;
                    case MediaPlayer2.CALL_COMPLETED_SEEK_TO:
                        final long pos = mPlayer.getCurrentPosition();
                        notifyPlayerCallback(new PlayerCallbackNotifier() {
                            @Override
                            public void notify(PlayerCallback callback) {
                                callback.onSeekCompleted(XMediaPlayer.this, pos);
                            }
                        });
                        break;
                    case MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS:
                        // TODO: Need to check if the speed value is really changed.
                        final float speed = mPlayer.getPlaybackParams().getSpeed();
                        notifyPlayerCallback(new PlayerCallbackNotifier() {
                            @Override
                            public void notify(PlayerCallback callback) {
                                callback.onPlaybackSpeedChanged(XMediaPlayer.this, speed);
                            }
                        });
                        break;
                }
            }
            // TODO: more handling on listenable future. e.g. Canceling.
            Integer resultCode = sResultCodeMap.get(status);
            pair.second.set(new CommandResult2(
                    resultCode == null ? RESULT_CODE_ERROR_UNKNOWN : resultCode,
                    System.currentTimeMillis(), dsd));
        }

        @Override
        public void onMediaTimeDiscontinuity(
                MediaPlayer2 mp, DataSourceDesc2 dsd, MediaTimestamp2 timestamp) {
            // TODO: extend PlayerCallback to provide this event.
        }

        @Override
        public void onCommandLabelReached(MediaPlayer2 mp, Object label) {
            // TODO: extend PlayerCallback to provide this event.
        }

        @Override
        public void onSubtitleData(MediaPlayer2 mp, DataSourceDesc2 dsd, SubtitleData2 data) {
            // TODO: extend PlayerCallback to provide this event.
        }
    }
}
