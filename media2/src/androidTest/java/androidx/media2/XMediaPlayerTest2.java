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

import static android.content.Context.KEYGUARD_SERVICE;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.AudioAttributesCompat.USAGE_MEDIA;

import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.media.AudioAttributesCompat;
import androidx.media2.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
public class XMediaPlayerTest2 {
    private static final String LOG_TAG = "XMediaPlayerTest";

    private static final int SLEEP_TIME = 1000;
    private static final float FLOAT_TOLERANCE = .0001f;

    private Context mContext;
    private Resources mResources;
    private ExecutorService mExecutor;
    protected XMediaPlayer mPlayer;

    private MediaStubActivity mActivity;
    private Instrumentation mInstrumentation;
    @Rule
    public ActivityTestRule<MediaStubActivity> mActivityRule =
            new ActivityTestRule<>(MediaStubActivity.class);
    private KeyguardManager mKeyguardManager;
    private List<AssetFileDescriptor> mFdsToClose = new ArrayList<>();

    @Before
    @CallSuper
    public void setUp() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mKeyguardManager = (KeyguardManager)
                mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
        mActivity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Keep screen on while testing.
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mActivity.setTurnScreenOn(true);
                mActivity.setShowWhenLocked(true);
                mKeyguardManager.requestDismissKeyguard(mActivity, null);
            }
        });
        mInstrumentation.waitForIdleSync();

        mContext = mActivityRule.getActivity();
        mResources = mContext.getResources();
//        mExecutor = Executors.newFixedThreadPool(1);
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        if (mPlayer != null) {
            mPlayer.close();
            mPlayer = null;
        }
        mActivity = null;
        for (AssetFileDescriptor afd :  mFdsToClose) {
            afd.close();
        }
    }

    @Test
    @LargeTest
    public void testJaewan1_hangs() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mPlayer = new XMediaPlayer(mContext);
        testBase();
    }

    @Test
    @LargeTest
    public void testJaewan2() throws Exception {
        mPlayer = new XMediaPlayer(mContext);
        testBase();
    }

    @Test
    @LargeTest
    public void testJaewan3() throws Exception {
        try {
            mActivityRule.runOnUiThread(new Runnable() {
                public void run() {
                    mPlayer = new XMediaPlayer(mActivity);
                }
            });
        } catch (Throwable e) {
            fail();
        }
        testBase();
    }

    private void testBase() throws IOException {
        AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(R.raw.testmp3);
        mFdsToClose.add(afd);
        mPlayer.setMediaItem(new FileMediaItem2.Builder(afd.getFileDescriptor(),
                afd.getStartOffset(), afd.getLength()).build());
        AudioAttributesCompat attr = new AudioAttributesCompat.Builder()
                .setContentType(CONTENT_TYPE_MUSIC).setUsage(USAGE_MEDIA).build();
        mPlayer.setAudioAttributes(attr);
        android.util.Log.d("jaewan", "pre-prepare");
        try {
            mPlayer.prepare().get();
        } catch (InterruptedException | ExecutionException e) {
            android.util.Log.d("jaewan", "prepare error", e);
        }
        android.util.Log.d("jaewan", "prepare done");
    }
}
