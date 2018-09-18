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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.media2.MediaPlayer2.DrmInfo;
import androidx.media2.TestUtils.Monitor;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

/**
 * Tests for the MediaPlayer2 API and local video/audio playback.
 *
 * The files in res/raw used by testLocalVideo* are (c) copyright 2008,
 * Blender Foundation / www.bigbuckbunny.org, and are licensed under the Creative Commons
 * Attribution 3.0 License at http://creativecommons.org/licenses/by/3.0/us/.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@Suppress // Disabled as it 100% fails b/79682973
public class MediaPlayer2DrmTest extends MediaPlayer2DrmTestBase {

    private static final String TAG = "MediaPlayer2DrmTest";

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Throwable {
        super.tearDown();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    // Asset helpers

    private static Uri getUriFromFile(String path) {
        return Uri.fromFile(new File(getDownloadedPath(path)));
    }

    private static String getDownloadedPath(String fileName) {
        return getDownloadedFolder() + File.separator + fileName;
    }

    private static String getDownloadedFolder() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath();
    }

    private static final class Resolution {
        public final boolean isHD;
        public final int width;
        public final int height;

        Resolution(boolean isHD, int width, int height) {
            this.isHD = isHD;
            this.width = width;
            this.height = height;
        }
    }

    private static final Resolution RES_720P  = new Resolution(true, 1280,  720);
    private static final Resolution RES_AUDIO = new Resolution(false,   0,    0);


    // Assets

    private static final Uri CENC_AUDIO_URL = Uri.parse(
            "https://storage.googleapis.com/wvmedia/cenc/clearkey/car_cenc-20120827-8c-pssh.mp4");
    private static final Uri CENC_AUDIO_URL_DOWNLOADED = getUriFromFile("car_cenc-20120827-8c.mp4");
    private static final Uri CENC_AUDIO_URL_DOWNLOADED2 = getUriFromFile(
            "car_cenc-20120827-8c-2.mp4");

    private static final Uri CENC_VIDEO_URL = Uri.parse(
            "https://storage.googleapis.com/wvmedia/cenc/clearkey/car_cenc-20120827-88-pssh.mp4");
    private static final Uri CENC_VIDEO_URL_DOWNLOADED = getUriFromFile("car_cenc-20120827-88.mp4");


    // Tests

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V0_SYNC() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V0_SYNC_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V1_ASYNC() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V1_ASYNC_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V2_SYNC_CONFIG() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V2_SYNC_CONFIG_TEST);
    }

    @Test
    @LargeTest
    public void testCAR_CLEARKEY_AUDIO_DOWNLOADED_V3_ASYNC_DRMPREPARED() throws Exception {
        download(CENC_AUDIO_URL,
                CENC_AUDIO_URL_DOWNLOADED,
                RES_AUDIO,
                ModularDrmTestType.V3_ASYNC_DRMPREPARED_TEST);
    }

    @Test
    @LargeTest
    public void testMultiItems_CAR_CLEARKEY_AUDIO_ASYNC_DRMPREPARED() throws Exception {
        Uri file1 = downloadDrmProtectedMedia(CENC_AUDIO_URL, CENC_AUDIO_URL_DOWNLOADED);
        final MediaItem2 item1 = new UriMediaItem2.Builder(mContext, file1).build();
        Uri file2 = downloadDrmProtectedMedia(CENC_AUDIO_URL, CENC_AUDIO_URL_DOWNLOADED2);
        final MediaItem2 item2 = new UriMediaItem2.Builder(mContext, file2).build();

        final int setUpTimeout = 2000;
        final int keyType = MediaDrm.KEY_TYPE_STREAMING;
        final Monitor item1DrmSetupDone = new Monitor();
        final Monitor item2DrmSetupDone = new Monitor();
        final Monitor item2DrmSetupAllowed = new Monitor();
        final Monitor item1PlaybackDone = new Monitor();
        final Monitor item2PlaybackDone = new Monitor();
        final String[] drmSetupError = new String[1];
        mPlayer.setEventCallback(mExecutor, new MediaPlayer2.EventCallback() {
            @Override
            public void onError(MediaPlayer2 mp, MediaItem2 item, int what, int extra) {
                assertTrue("Error on item" + (item == item1 ? "1 " : "2 ") + what + " " + extra,
                        false);
            }

            @Override
            public void onInfo(MediaPlayer2 mp, MediaItem2 item, int what, int extra) {
                if (what == MediaPlayer2.MEDIA_INFO_DATA_SOURCE_END) {
                    if (item == item1) {
                        item1PlaybackDone.signal();
                    } else if (item == item2) {
                        item2PlaybackDone.signal();
                    }
                }
            }
        });
        mPlayer.setDrmEventCallback(mExecutor, new MediaPlayer2.DrmEventCallback() {
            @Override
            public void onDrmInfo(MediaPlayer2 mp, MediaItem2 item, MediaPlayer2.DrmInfo drmInfo) {
                if (drmInfo.getSupportedSchemes().isEmpty()) {
                    drmSetupError[0] = "No supported scheme in DrmInfo.";
                    return;
                }
                try {
                    // setting up with the first supported UUID
                    // instead of supportedSchemes[0] in GTS
                    mp.prepareDrm(item, CLEARKEY_SCHEME_UUID);
                } catch (Exception e) {
                    e.printStackTrace();
                    drmSetupError[0] = e.getMessage();
                    return;
                }
            }

            @Override
            public void onDrmPrepared(MediaPlayer2 mp, MediaItem2 item, int status) {
                if (status != MediaPlayer2.PREPARE_DRM_STATUS_SUCCESS) {
                    drmSetupError[0] = "Failed to prepare DRM " + status;
                    return;
                }
                if (item == item2) {
                    try {
                        item2DrmSetupAllowed.waitForSignal();
                    } catch (InterruptedException e) {
                        drmSetupError[0] = e.getMessage();
                        return;
                    }
                }

                // instead of supportedSchemes[0] in GTS
                UUID drmScheme = CLEARKEY_SCHEME_UUID;
                DrmInfo drmInfo = mp.getDrmInfo(item);
                byte[] psshData = drmInfo.getPssh().get(drmScheme);
                byte[] initData = null;
                if (psshData == null) {
                    initData = mClearKeyPssh;
                    Log.d(TAG, "setupDrm: CLEARKEY scheme not found in PSSH."
                            + " Using default data.");
                } else {
                    // Can skip conversion if ClearKey adds support for BMFF initData b/64863112
                    initData = makeCencPSSH(CLEARKEY_SCHEME_UUID, psshData);
                }
                Log.d(TAG, "setupDrm: initData[" + drmScheme + "]: "
                        + Arrays.toString(initData));

                String mime = "cenc";

                try {
                    MediaDrm.KeyRequest request =
                            mp.getDrmKeyRequest(item, null, initData, mime, keyType, null);
                    // diverging from GTS
                    byte[][] clearKeys = new byte[][] { CLEAR_KEY_CENC };
                    byte[] response = createKeysResponse(request, clearKeys);

                    mp.provideDrmKeyResponse(item, null, response);

                } catch (MediaPlayer2.NoDrmSchemeException | DeniedByServerException e) {
                    e.printStackTrace();
                    drmSetupError[0] = e.getMessage();
                    return;
                }
                if (item == item1) {
                    item1DrmSetupDone.signal();
                } else if (item == item2) {
                    item2DrmSetupDone.signal();
                }
            }
        });

        // TEST CASE 1 : Finish DRM setup for both media items and start playing.
        mPlayer.setMediaItem(item1);
        mPlayer.setNextMediaItem(item2);
        mPlayer.prepare();

        item2DrmSetupAllowed.signal();
        assertTrue(item1DrmSetupDone.waitForSignal(setUpTimeout));
        assertTrue(item2DrmSetupDone.waitForSignal(setUpTimeout));
        assertNull(drmSetupError[0], drmSetupError[0]);

        mPlayer.play();

        item1PlaybackDone.waitForSignal();
        item2PlaybackDone.waitForSignal();

        item1DrmSetupDone.reset();
        item2DrmSetupDone.reset();;
        item2DrmSetupAllowed.reset();;
        item1PlaybackDone.reset();;
        item2PlaybackDone.reset();;
        mPlayer.reset();

        // TEST CASE 2 : Hold the DRM setup for item2 until playback for item1 finishes.
        mPlayer.setMediaItem(item1);
        mPlayer.setNextMediaItem(item2);
        mPlayer.prepare();

        assertTrue(item1DrmSetupDone.waitForSignal(setUpTimeout));
        assertNull(drmSetupError[0], drmSetupError[0]);

        mPlayer.play();
        item1PlaybackDone.waitForSignal();


        Thread.sleep(1000);
        item2DrmSetupAllowed.signal();
        assertTrue(item2DrmSetupDone.waitForSignal(setUpTimeout));
        assertNull(drmSetupError[0], drmSetupError[0]);

        // Playback for item2 should be automatically started once DRM setup is done.
        item2PlaybackDone.waitForSignal();
    }

    // helpers

    private void stream(Uri uri, Resolution res, ModularDrmTestType testType) throws Exception {
        playModularDrmVideo(uri, res.width, res.height, testType);
    }

    private void download(Uri remote, Uri local, Resolution res, ModularDrmTestType testType)
            throws Exception {
        playModularDrmVideoDownload(remote, local, res.width, res.height, testType);
    }

}
