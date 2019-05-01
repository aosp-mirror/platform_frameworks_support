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

package com.example.androidx.webkit;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link Activity} to exercise Safe Browsing functionality.
 */
public class SafeBrowsingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO(ntfschr): actually do something eventually.
        setContentView(R.layout.activity_safe_browsing);
        setTitle(R.string.safebrowsing_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
<<<<<<< HEAD   (e53308 Merge "Merge empty history for sparse-5498091-L6460000030224)
=======

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.safe_browsing_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.small_interstitial_activity_title),
                        new Intent(activityContext, SmallInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.medium_wide_interstitial_activity_title),
                        new Intent(activityContext, MediumInterstitialActivity.class)
                                .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, false)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.medium_tall_interstitial_activity_title),
                        new Intent(activityContext, MediumInterstitialActivity.class)
                                .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, true)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.loud_interstitial_activity_title),
                        new Intent(activityContext, LoudInterstitialActivity.class)),
        };
        listView.setItems(menuItems);
>>>>>>> BRANCH (3a06c2 Merge "Merge cherrypicks of [954920] into sparse-5520679-L60)
    }
}
