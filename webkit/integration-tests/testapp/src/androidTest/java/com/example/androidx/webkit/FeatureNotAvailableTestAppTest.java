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

package com.example.androidx.webkit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@LargeTest
public class FeatureNotAvailableTestAppTest {
    @Parameterized.Parameters
    public static Collection<Object[]> getFeatures() {
        // Add new features to this list
        return Arrays.asList(new Object[][] {
            {androidx.webkit.WebViewFeature.PROXY_OVERRIDE,
             R.string.proxy_override_activity_title}
        });
    }

    @Parameterized.Parameter(0)
    public String mFeature;

    @Parameterized.Parameter(1)
    public Integer mTitleResId;

    @Rule
    public IntegrationAppTestRule mRule = new IntegrationAppTestRule();

    @Before
    public void setUp() {
        mRule.getActivity();
        mRule.assumeFeatureNotAvailable(mFeature);
        mRule.clickMenuListItem(mTitleResId);
    }

    @Test
    public void testFeatureNotAvailable() {
        onView(withText(R.string.webkit_api_not_available)).check(matches(isDisplayed()));
    }
}
