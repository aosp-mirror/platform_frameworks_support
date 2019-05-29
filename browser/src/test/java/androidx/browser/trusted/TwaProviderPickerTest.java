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

import static androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.trusted.TwaProviderPicker.LaunchMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

/**
 * Tests for {@link TwaProviderPicker}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class TwaProviderPickerTest {
    private PackageManager mPackageManager;
    private ShadowPackageManager mShadowPackageManager;

    private static final String BROWSER1 = "com.browser.one";
    private static final String BROWSER2 = "com.browser.two";
    private static final String CUSTOM_TABS_PROVIDER1 = "com.customtabs.one";
    private static final String CUSTOM_TABS_PROVIDER2 = "com.customtabs.two";
    private static final String TWA_PROVIDER1 = "com.trustedweb.one";
    private static final String TWA_PROVIDER2 = "com.trustedweb.two";

    // TODO(peconn): Deduplicate these with members in TrustedWebUtils (should probably move
    // TrustedWebUtils into this package, which requires removing TrustedWebUtils' dependencies on
    // package methods in CustomTabsSession (in #launchBrowserSessions)).
    private static final String CHROME = "com.android.chrome";
    private static final int CHROME_72_VERSION = 362600000;
    private static final int CHROME_71_VERSION = 357800000;

    @Before
    public void setUp() {
        mPackageManager = RuntimeEnvironment.application.getPackageManager();
        mShadowPackageManager = shadowOf(mPackageManager);
    }

    /**
     * Tests that we attempt don't do anything stupid if the user (somehow) does not have any
     * browsers on their device.
     */
    @Test
    public void noBrowsers() {
        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.BROWSER, action.launchMode);
        assertNull(action.provider);
    }

    /**
     * Tests that in lack of any Custom Tabs or Trusted Web Activity providers, we choose Android's
     * preferred browser.
     */
    @Test
    public void noCustomTabsProviders() {
        installBrowser(BROWSER1);
        installBrowser(BROWSER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.BROWSER, action.launchMode);
        assertEquals(BROWSER1, action.provider);
    }

    /**
     * Tests that a Custom Tabs provider is chosen over non-Custom Tabs browsers.
     */
    @Test
    public void customTabsProvider() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.CUSTOM_TAB, action.launchMode);
        assertEquals(CUSTOM_TABS_PROVIDER1, action.provider);
    }

    /**
     * Tests that a Trusted Web Activity provider takes preference over all.
     */
    @Test
    public void trustedWebActivityProvider() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installTrustedWebActivityProvider(TWA_PROVIDER1);
        installTrustedWebActivityProvider(TWA_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(TWA_PROVIDER1, action.provider);
    }

    /**
     * Tests that we recognise Chrome 72 as supporting Trusted Web Activities even though it doesn't
     * have the TRUSTED_WEB_ACTIVITY_CATEGORY.
     */
    @Test
    public void choosesChrome72() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installChrome(CHROME_72_VERSION);
        installTrustedWebActivityProvider(TWA_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(CHROME, action.provider);
    }

    /**
     * Tests that we do not recognise Chrome versions before 72 as supporting Trusted Web
     * Activities.
     */
    @Test
    public void doesNotChooseChrome71() {
        installBrowser(BROWSER1);
        installCustomTabsProvider(CUSTOM_TABS_PROVIDER1);
        installChrome(CHROME_71_VERSION);
        installTrustedWebActivityProvider(TWA_PROVIDER2);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(TWA_PROVIDER2, action.provider);
    }

    /**
     * Tests that if the user has a non-Chrome TWA provider as their default, we choose that.
     */
    @Test
    public void choosesDefaultOverChrome() {
        installTrustedWebActivityProvider(TWA_PROVIDER1);
        installChrome(CHROME_72_VERSION);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertEquals(LaunchMode.TRUSTED_WEB_ACTIVITY, action.launchMode);
        assertEquals(TWA_PROVIDER1, action.provider);
    }

    private void installBrowser(String packageName) {
        Intent intent = new Intent()
                .setData(Uri.parse("http://"))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installCustomTabsProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installTrustedWebActivityProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.filter = Mockito.mock(IntentFilter.class);
        when(resolveInfo.filter.hasCategory(eq(TRUSTED_WEB_ACTIVITY_CATEGORY))).thenReturn(true);

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installChrome(int version) {
        // Chrome was still a Custom Tabs provider before Chrome 72.
        installCustomTabsProvider(CHROME);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = version;
        packageInfo.packageName = CHROME;

        mShadowPackageManager.addPackage(packageInfo);
    }
}
