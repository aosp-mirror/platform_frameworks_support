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

package androidx.browser.trusted.splashscreens;

import static androidx.browser.customtabs.TrustedWebUtils.EXTRA_SPLASH_SCREEN_PARAMS;
import static androidx.browser.customtabs.testutil.TestUtil.runOnUiThreadBlocking;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.browser.customtabs.TestCustomTabsService;
import androidx.browser.customtabs.TestCustomTabsServiceNoSplashScreens;
import androidx.browser.customtabs.TestCustomTabsServiceSupportsTwas;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.customtabs.testutil.CustomTabConnectionRule;
import androidx.browser.customtabs.testutil.TestUtil;
import androidx.browser.test.R;
import androidx.browser.trusted.TestBrowser;
import androidx.browser.trusted.TrustedWebActivityBuilder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link PwaWrapperSplashScreenStrategy}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class PwaWrapperSplashScreenStrategyTest {

    // For clean-up. See SplashImageTransferTask.java.
    private static final String FOLDER_NAME = "twa_splash";
    private static final String FILE_PROVIDER_AUTHORITY =
            "android.support.customtabs.trusted.test_fileprovider";

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class
    );

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    @Rule
    public final CustomTabConnectionRule mConnectionRule = new CustomTabConnectionRule();

    private TestActivity mActivity;
    private CustomTabsSession mSession;
    private PwaWrapperSplashScreenStrategy mStrategy;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mSession = mConnectionRule.establishSessionBlocking(mActivity);
        mStrategy = new PwaWrapperSplashScreenStrategy(mActivity, R.drawable.splash, 0,
                ImageView.ScaleType.FIT_XY, null, 0, FILE_PROVIDER_AUTHORITY);
    }

    @After
    public void tearDown() {
        new File(mActivity.getFilesDir(), FOLDER_NAME).delete(); // Clean up save splash image file.
    }

    @Test
    public void showsSplashScreenInClient_WhenTwaLaunchInitiated() {
        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mStrategy.onTwaLaunchInitiated(mActivity.getPackageName(), 0);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // The splash screen should be full-screen blue. Check just one pixel.
        assertEquals(mActivity.getColor(R.color.splash_screen_color), getColorOfAPixel());
    }

    @Test
    public void doesntShowsSplashScreenInClient_IfSplashScreensNotSupported() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        mEnableComponents.manuallyEnable(TestCustomTabsServiceNoSplashScreens.class);

        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mStrategy.onTwaLaunchInitiated(mActivity.getPackageName(), 0);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertNotEquals(mActivity.getColor(R.color.splash_screen_color), getColorOfAPixel());
    }

    @Test
    public void imageFileIsSuccessfullyTransferredToService() {
        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mStrategy.onTwaLaunchInitiated(mActivity.getPackageName(), 0);
            }
        });
        mStrategy.configureTwaBuilder(new TrustedWebActivityBuilder(mActivity, Uri.EMPTY),
                mSession, null);
        assertTrue(TestCustomTabsService.getInstance().waitForSplashImageFile(3000));
    }

    @Test
    public void setsParametersOnTwaBuilder() throws InterruptedException {
        int bgColor = 0x115599;
        ImageView.ScaleType scaleType = ImageView.ScaleType.MATRIX;
        Matrix matrix = new Matrix();
        matrix.setSkew(1, 2, 3, 4);
        int fadeOutDuration = 300;

        final PwaWrapperSplashScreenStrategy strategy =
                new PwaWrapperSplashScreenStrategy(mActivity, R.drawable.splash, bgColor,
                        scaleType, matrix, fadeOutDuration, FILE_PROVIDER_AUTHORITY);
        strategy.onActivityEnterAnimationComplete();

        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                strategy.onTwaLaunchInitiated(mActivity.getPackageName(), 0);
            }
        });
        final TrustedWebActivityBuilder builder = new TrustedWebActivityBuilder(mActivity,
                Uri.parse("https://test.com"));

        final CountDownLatch latch = new CountDownLatch(1);
        strategy.configureTwaBuilder(builder, mSession, new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        Intent intent = TestUtil.getBrowserActivityWhenLaunched(
                new Runnable() {
                    @Override
                    public void run() {
                        builder.launchActivity(mSession);
                    }
                }).getIntent();
        Bundle bundle = intent.getBundleExtra(EXTRA_SPLASH_SCREEN_PARAMS);

        assertEquals(bgColor, bundle.getInt(TrustedWebUtils.SplashScreenParamKey.BACKGROUND_COLOR));
        assertEquals(scaleType.ordinal(),
                bundle.getInt(TrustedWebUtils.SplashScreenParamKey.SCALE_TYPE));
        assertEquals(fadeOutDuration,
                bundle.getInt(TrustedWebUtils.SplashScreenParamKey.FADE_OUT_DURATION_MS));

        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        assertArrayEquals(matrixValues, bundle.getFloatArray(
                TrustedWebUtils.SplashScreenParamKey.IMAGE_TRANSFORMATION_MATRIX), 1e-3f);
    }

    @Test
    public void waitsForEnterAnimationCompletion_BeforeDeclaringReady()
            throws InterruptedException {
        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mStrategy.onTwaLaunchInitiated(mActivity.getPackageName(), 0);
            }
        });
        final Runnable readyCallback = mock(Runnable.class);
        mStrategy.configureTwaBuilder(new TrustedWebActivityBuilder(mActivity, Uri.EMPTY),
                mSession, readyCallback);
        TestCustomTabsService.getInstance().waitForSplashImageFile(3000);

        final CountDownLatch latch = new CountDownLatch(1);
        runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                verify(readyCallback, never()).run();
                mStrategy.onActivityEnterAnimationComplete();
                verify(readyCallback).run();
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    private int getColorOfAPixel() {
        View view = runOnUiThreadBlocking(new Callable<View>() {
            @Override
            public View call() throws Exception {
                return mActivity.findViewById(android.R.id.content);
            }
        });
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap.getPixel(0, 0);
    }

}
