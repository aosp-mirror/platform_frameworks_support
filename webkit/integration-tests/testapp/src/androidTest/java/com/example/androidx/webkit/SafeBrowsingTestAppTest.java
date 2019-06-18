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
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;

import static org.hamcrest.core.Is.is;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.WebViewFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SafeBrowsingTestAppTest {
    @Rule
    public IntegrationAppTestRule mRule = new IntegrationAppTestRule();

    @Before
    public void setUp() {
        mRule.getActivity();
        mRule.clickMenuListItem(R.string.safebrowsing_activity_title);
        IdlingRegistry.getInstance().register(SafeBrowsingActivity.getIdlingResources());
        // Skip features after registering IdlingResources, so that resources are registered before
        // we hit tearDown().
        mRule.assumeFeatureNotAvailable(WebViewFeature.START_SAFE_BROWSING);
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(SafeBrowsingActivity.getIdlingResources());
    }

    @Test
    public void testStartSafeBrowsing() {
        onView(withClassName(is(MenuListView.class.getName()))).check(matches(isDisplayed()));
    }
}
