/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.player.MediaPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;

/**
 * Test {@link VideoView} with a {@link MediaController}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoView_WithControllerTest extends VideoView_WithSthTestBase {
    private SessionPlayer mPlayer;
    private MediaSession mSession;
    private MediaController mController;

    @Override
    void initPlayerOrController() throws Throwable {
        prepareLooper();

        mPlayer = new MediaPlayer(mContext);
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setId("VVTest")
                .build();
        mController = new MediaController.Builder(mContext)
                .setSessionToken(mSession.getToken())
                .build();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaController(mController);
            }
        });
    }

    @Override
    void closePlayerOrController() throws Throwable {
        if (mController != null) {
            mController.close();
        }
        if (mSession != null) {
            mSession.close();
        }
        if (mPlayer != null) {
            mPlayer.close();
        }
    }

    @Override
    void registerCallback(PlayerWrapper.PlayerCallback callback) {
        mPlayerWrapper = new PlayerWrapper(mController, mMainHandlerExecutor, callback);
        mPlayerWrapper.attachCallback();
    }

    @Override
    void waitToPrepare(MediaItem item) throws Exception {
        mPlayer.setMediaItem(item);
        mPlayer.prepare().get();
    }
}
