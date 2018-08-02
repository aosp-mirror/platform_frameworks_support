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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.core.app.RemoteActionCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.textclassifier.LegacyTextClassifier.MatchMakerImpl;
import androidx.textclassifier.LegacyTextClassifier.MatchMakerImpl.PermissionsChecker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Unit tests for {@link MatchMakerImpl}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class MatchMakerImplTest {

    private static final ResolveInfo RESOLVE_INFO = new ResolveInfo();
    static {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "test.action.package";
        activityInfo.name = "TestAction";
        activityInfo.applicationInfo = new ApplicationInfo();
        RESOLVE_INFO.activityInfo = activityInfo;
    }

    private Context mContext;
    private PackageManager mPackageManager;
    private Bundle mUserRestrictions;
    private PermissionsChecker mPermissionsChecker;
    private MatchMaker mMatchMaker;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mPackageManager = spy(mContext.getPackageManager());
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(RESOLVE_INFO);
        mUserRestrictions = new Bundle();
        mPermissionsChecker = new PermissionsChecker() {
            @Override
            public boolean hasPermission(String packageName, String permission, boolean exported) {
                return true;
            }
        };
        mMatchMaker = new MatchMakerImpl(
                mContext, mPackageManager, mUserRestrictions, mPermissionsChecker);
    }

    @Test
    public void getUrlActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).hasSize(1);
    }

    @Test
    public void getEmailActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_EMAIL, "email@android.com");

        assertThat(actions).hasSize(2);
    }

    @Test
    public void getPhoneActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_PHONE, "(987) 654-3210");

        assertThat(actions).hasSize(3);
    }

    @Test
    public void unsupportedEntityType() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_ADDRESS, "1 Android way");

        assertThat(actions).isEmpty();
    }

    @Test
    public void noMatchingApp() throws Exception {
        final PackageManager packageManager = spy(mContext.getPackageManager());
        when(packageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(null);

        final MatchMaker matchMaker = new MatchMakerImpl(
                mContext, packageManager, mUserRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                matchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).isEmpty();
    }

    @Test
    public void noMatchingActivity() throws Exception {
        final PackageManager packageManager = spy(mContext.getPackageManager());
        when(packageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());

        final MatchMaker matchMaker = new MatchMakerImpl(
                mContext, packageManager, mUserRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                matchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).isEmpty();
    }
}
