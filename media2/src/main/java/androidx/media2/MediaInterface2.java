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

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

class MediaInterface2 {
    private MediaInterface2() {
    }

    // TODO: relocate methods among different interfaces and classes.
    interface SessionPlaybackControl {
        ListenableFuture<CommandResult2> prepare();
        ListenableFuture<CommandResult2> play();
        ListenableFuture<CommandResult2> pause();

        ListenableFuture<CommandResult2> seekTo(long pos);

        int getPlayerState();
        long getCurrentPosition();
        long getDuration();

        long getBufferedPosition();
        int getBufferingState();

        float getPlaybackSpeed();
        ListenableFuture<CommandResult2> setPlaybackSpeed(float speed);
    }

    interface SessionPlaylistControl {
        List<MediaItem2> getPlaylist();
        MediaMetadata2 getPlaylistMetadata();
        ListenableFuture<CommandResult2> setPlaylist(List<MediaItem2> list,
                MediaMetadata2 metadata);
        ListenableFuture<CommandResult2> updatePlaylistMetadata(MediaMetadata2 metadata);

        MediaItem2 getCurrentMediaItem();
        ListenableFuture<CommandResult2> skipToPlaylistItem(MediaItem2 item);
        ListenableFuture<CommandResult2> skipToPreviousItem();
        ListenableFuture<CommandResult2> skipToNextItem();

        ListenableFuture<CommandResult2> addPlaylistItem(int index, MediaItem2 item);
        ListenableFuture<CommandResult2> removePlaylistItem(MediaItem2 item);
        ListenableFuture<CommandResult2> replacePlaylistItem(int index, MediaItem2 item);

        int getRepeatMode();
        ListenableFuture<CommandResult2> setRepeatMode(int repeatMode);
        int getShuffleMode();
        ListenableFuture<CommandResult2> setShuffleMode(int shuffleMode);
    }

    // Common interface for session2 and controller2
    // TODO: consider to add fastForward, rewind.
    interface SessionPlayer extends SessionPlaybackControl, SessionPlaylistControl {
        ListenableFuture<CommandResult2> skipForward();
        ListenableFuture<CommandResult2> skipBackward();
        void notifyError(@MediaSession2.ResultCode int errorCode,
                @Nullable Bundle extras);
    }
}
