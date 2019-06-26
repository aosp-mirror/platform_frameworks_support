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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.UriMediaItem;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link VideoView} with a {@link SessionPlayer} or a {@link MediaController}.
 */
@RunWith(Parameterized.class)
@LargeTest
public class VideoView_WithPlayerTest extends MediaWidgetTestBase {
    @Parameterized.Parameters(name = "PlayerType={0}")
    public static List<String> getPlayerTypes() {
        return Arrays.asList(PLAYER_TYPE_MEDIA_CONTROLLER, PLAYER_TYPE_MEDIA_PLAYER);
    }

    private String mPlayerType;
    private Activity mActivity;
    private VideoView mVideoView;
    private MediaItem mMediaItem;

    @Rule
    public ActivityTestRule<VideoViewTestActivity> mActivityRule =
            new ActivityTestRule<>(VideoViewTestActivity.class);

    public VideoView_WithPlayerTest(String playerType) {
        mPlayerType = playerType;
    }

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mMediaItem = createTestMediaItem();

        setKeepScreenOn(mActivityRule);
        checkAttachedToWindow(mVideoView);
    }

    @After
    public void tearDown() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeAll();
            }
        });
    }

    @Test
    public void testPlayVideo() throws Throwable {
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback.mPlayingLatch.getCount());
        assertEquals(SessionPlayer.PLAYER_STATE_PAUSED, playerWrapper.getPlayerState());

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayVideoWithMediaItemFromFileDescriptor() throws Throwable {
        AssetFileDescriptor afd = mContext.getResources()
                .openRawResourceFd(R.raw.testvideo_with_2_subtitle_tracks);
        final MediaItem item = new FileMediaItem.Builder(
                ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                .setFileDescriptorOffset(afd.getStartOffset())
                .setFileDescriptorLength(afd.getLength())
                .build();
        afd.close();

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, item, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetViewType() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
            }
        });

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        // WAIT_TIME_MS multiplied by the number of operations.
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS * 5, TimeUnit.MILLISECONDS));
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
    }

    // @UiThreadTest will be ignored by Parameterized test runner (b/30746303)
    @Test
    public void testAttachedMediaControlView_setPlayerOrController() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayerWrapper playerWrapper = createPlayerWrapper(new DefaultPlayerCallback(),
                        mMediaItem, null);

                MediaControlView defaultMediaControlView = mVideoView.getMediaControlView();
                assertNotNull(defaultMediaControlView);
                try {
                    if (playerWrapper.mPlayer != null) {
                        defaultMediaControlView.setPlayer(playerWrapper.mPlayer);
                    } else if (playerWrapper.mController != null) {
                        defaultMediaControlView.setMediaController(playerWrapper.mController);
                    } else {
                        fail("playerWrapper doesn't have neither mPlayer or mController");
                    }
                    fail("setPlayer or setMediaController should not be allowed "
                            + "for MediaControlView attached to VideoView");
                } catch (IllegalStateException ex) {
                    // expected
                }

                MediaControlView newMediaControlView = new MediaControlView(mContext);
                mVideoView.setMediaControlView(newMediaControlView, -1);
                try {
                    if (playerWrapper.mPlayer != null) {
                        newMediaControlView.setPlayer(playerWrapper.mPlayer);
                    } else if (playerWrapper.mController != null) {
                        newMediaControlView.setMediaController(playerWrapper.mController);
                    } else {
                        fail("playerWrapper doesn't have neither mPlayer or mController");
                    }
                    fail("setPlayer or setMediaController should not be allowed "
                            + "for MediaControlView attached to VideoView");
                } catch (IllegalStateException ex) {
                    // expected
                }
            }
        });
    }

    @Test
    public void testOnVideoSizeChanged() throws Throwable {
        final Uri nonMusicUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.testvideo_with_2_subtitle_tracks);
        final Uri musicUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_music);
        final VideoSize nonMusicVideoSize = new VideoSize(160, 90);
        final VideoSize musicVideoSize = new VideoSize(0, 0);

        final CountDownLatch latchForNonMusicItem = new CountDownLatch(1);
        final CountDownLatch latchForMusicItem = new CountDownLatch(1);

        List<MediaItem> playlist = new ArrayList<>();
        playlist.add(createTestMediaItem(nonMusicUri));
        playlist.add(createTestMediaItem(musicUri));

        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            void onVideoSizeChanged(@NonNull PlayerWrapper player, @NonNull MediaItem item,
                    @NonNull VideoSize videoSize) {
                if (item == null) {
                    return;
                }
                if (TextUtils.equals(((UriMediaItem) item).getUri().toString(),
                        nonMusicUri.toString())) {
                    if (nonMusicVideoSize.equals(videoSize)) {
                        latchForNonMusicItem.countDown();
                    }
                } else if (TextUtils.equals(((UriMediaItem) item).getUri().toString(),
                        musicUri.toString())) {
                    if (musicVideoSize.equals(videoSize)) {
                        latchForMusicItem.countDown();
                    }
                }
            }
        }, null, playlist);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForNonMusicItem.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        playerWrapper.skipToNextItem();
        assertTrue(latchForMusicItem.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    private void setPlayerWrapper(final PlayerWrapper playerWrapper) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (playerWrapper.mPlayer != null) {
                    mVideoView.setPlayer(playerWrapper.mPlayer);
                } else if (playerWrapper.mController != null) {
                    mVideoView.setMediaController(playerWrapper.mController);
                }
            }
        });
    }

    private PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist) {
        return createPlayerWrapperOfType(callback, item, playlist, mPlayerType);
    }
}
