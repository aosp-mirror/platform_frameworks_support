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

package androidx.browser.trusted;

import static androidx.browser.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;
import static androidx.browser.customtabs.testutil.TestUtil.getBrowserActivityWhenLaunched;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.CustomTabsSessionToken;
import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.browser.customtabs.TestCustomTabsService;
import androidx.browser.customtabs.TestCustomTabsServiceSupportsTwas;
import androidx.browser.trusted.splashscreens.SplashScreenStrategy;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Instrumentation tests for {@link TwaLauncher}
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TwaLauncherTest {

    private static final Uri URL = Uri.parse("https://www.test.com/default_url/");

    private Context mContext = ApplicationProvider.getApplicationContext();

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class,
            TestCustomTabsService.class
    );
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    private TestActivity mActivity;

    private TwaLauncher mTwaLauncher;

    @Before
    public void setUp() {
        TwaProviderPicker.restrictToPackageForTesting(mContext.getPackageName());
        mActivity = mActivityTestRule.getActivity();
        mTwaLauncher = new TwaLauncher(mActivity);
    }

    @After
    public void tearDown() {
        TwaProviderPicker.restrictToPackageForTesting(null);
        mTwaLauncher.destroy();
    }

    @Test
    public void launchesTwaWithJustUrl() {
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                mTwaLauncher.launch(URL);
            }
        };
        TestBrowser browser = getBrowserActivityWhenLaunched(launchRunnable);
        assertTrue(browser.getIntent().getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY,
                false));
        assertEquals(URL, browser.getIntent().getData());
    }

    @Test
    public void transfersTwaBuilderParams() {
        // Checking just one parameters. TrustedWebActivityBuilderTest tests the rest. Here we just
        // check that TwaLauncher doesn't ignore the passed builder.
        final TrustedWebActivityBuilder builder = makeBuilder().setStatusBarColor(0x0000ff);
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                mTwaLauncher.launch(builder, null, null);
            }
        };
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();
        assertEquals(0x0000ff, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void fallsBackToCustomTab() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        final TwaLauncher launcher = new TwaLauncher(mActivity);

        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                launcher.launch(URL);
            }
        };
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertFalse(intent.hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    @Test
    public void customTabFallbackUsesStatusBarColor() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        final TwaLauncher launcher = new TwaLauncher(mActivity);

        final TrustedWebActivityBuilder builder = makeBuilder().setStatusBarColor(0x0000ff);
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                launcher.launch(builder, null, null);
            }
        };
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertEquals(0x0000ff, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void reusesSessionForSubsequentLaunches() {
        final TwaLauncher launcher1 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(new Runnable() {
                    @Override
                    public void run() {
                        launcher1.launch(URL);
                    }
                });
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        final TwaLauncher launcher2 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(new Runnable() {
                    @Override
                    public void run() {
                        launcher2.launch(URL);
                    }
                });
        launcher2.destroy();

        assertEquals(token1, token2);
    }

    @Test
    public void createsDifferentSessions_IfDifferentIdsSpecified() {
        int sessionId1 = 1;
        int sessionId2 = 2;

        final TwaLauncher launcher1 = new TwaLauncher(mActivity, null, sessionId1);
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(new Runnable() {
                    @Override
                    public void run() {
                        launcher1.launch(URL);
                    }
                });
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        final TwaLauncher launcher2 = new TwaLauncher(mActivity, null, sessionId2);
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(new Runnable() {
                    @Override
                    public void run() {
                        launcher2.launch(URL);
                    }
                });
        launcher2.destroy();

        assertNotEquals(token1, token2);
    }

    @Test
    public void completionCallbackCalled() {
        final Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                mTwaLauncher.launch(TwaLauncherTest.this.makeBuilder(), null, callback);
            }
        };
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
    }

    @Test
    public void completionCallbackCalled_WhenFallingBackToCct() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        final TwaLauncher twaLauncher = new TwaLauncher(mActivity);

        final Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                twaLauncher.launch(TwaLauncherTest.this.makeBuilder(), null, callback);
            }
        };
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
        twaLauncher.destroy();
    }

    @Test
    public void notifiesSplashScreenStrategyOfLaunchInitiation() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        mTwaLauncher.launch(makeBuilder().setStatusBarColor(0xff0000), strategy, null);
        verify(strategy).onTwaLaunchInitiated(
                eq(ApplicationProvider.getApplicationContext().getPackageName()),
                eq(0xff0000));
    }

    @Test
    public void doesntLaunch_UntilSplashScreenStrategyFinishesConfiguring() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);

        // Using spy to verify launchActivity not called to avoid testing directly that activity is
        // not launched.
        TrustedWebActivityBuilder builder = spy(makeBuilder());
        mTwaLauncher.launch(builder, strategy, null);
        verify(builder, never()).launchActivity(any(CustomTabsSession.class));
    }

    @Test
    public void launches_WhenSplashScreenStrategyFinishesConfiguring() {
        final SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArgument(2)).run();
                return null;
            }
        }).when(strategy).configureTwaBuilder(any(TrustedWebActivityBuilder.class),
                any(CustomTabsSession.class), any(Runnable.class));

        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                mTwaLauncher.launch(TwaLauncherTest.this.makeBuilder(), strategy, null);
            }
        };
        assertNotNull(getBrowserActivityWhenLaunched(launchRunnable));
    }

    private TrustedWebActivityBuilder makeBuilder() {
        return new TrustedWebActivityBuilder(mActivity, URL);
    }


    private CustomTabsSessionToken getSessionTokenFromLaunchedBrowser(Runnable launchRunnable) {
        return CustomTabsSessionToken.getSessionTokenFromIntent(
                getBrowserActivityWhenLaunched(launchRunnable).getIntent());
    }
}
