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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;

/**
 * An {@link Activity} for exercising various WebView functionality. This Activity is a {@link
 * ListView} which starts other Activities, each of which may similarly be a ListView, or may
 * actually exercise specific {@link android.webkit.WebView} features.
 */
public class MainActivity extends Activity {

    private static class MenuItem {
        private Class mActivityToLaunch;
        MenuItem(Class activityToLaunch) {
            mActivityToLaunch = activityToLaunch;
        }

        @Override
        public String toString() {
            return mActivityToLaunch.getSimpleName();
        }

        public void start(Context activityContext) {
            activityContext.startActivity(new Intent(activityContext, mActivityToLaunch));
        }
    }


    private static final MenuItem[] sActivities = { new MenuItem(SafeBrowsingActivity.class) };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final String webViewVersion = WebViewCompat.getCurrentWebViewPackage(this).versionName;
        final String oldTitle = getTitle().toString();
        final String newTitle = oldTitle + " (" + webViewVersion + ")";
        setTitle(newTitle);

        final Context activityContext = this;
        ListView listView = findViewById(R.id.top_level_list);
        ArrayAdapter<MenuItem> featureArrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sActivities);
        listView.setAdapter(featureArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                featureArrayAdapter.getItem(position).start(activityContext);
            }
        });
    }
}
