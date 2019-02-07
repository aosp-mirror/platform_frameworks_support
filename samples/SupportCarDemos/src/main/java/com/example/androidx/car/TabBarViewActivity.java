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

package com.example.androidx.car;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.car.widget.CarTabBarView;
import androidx.car.widget.CarTabItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link androidx.car.widget.CarTabBarView}.
 */
public class TabBarViewActivity extends AppCompatActivity {
    private static final String TAG = "TabBarViewActivity";
    private final List<CarTabItem> mTabItems = new ArrayList<>();
    private CarTabBarView mTabBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_bar_view_activity);

        mTabBarView = findViewById(R.id.car_tab_bar);
        mTabBarView.setOnTabSelectListener(
                position -> Toast.makeText(/* context= */ this,
                        "Tab " + position + " selected", Toast.LENGTH_SHORT).show());

        findViewById(R.id.tab_align_start).setOnClickListener(
                v -> mTabBarView.setTabAlignment(CarTabBarView.TabAlignment.START));

        findViewById(R.id.tab_align_center).setOnClickListener(
                v -> mTabBarView.setTabAlignment(CarTabBarView.TabAlignment.CENTER));

        findViewById(R.id.add_tab).setOnClickListener(v -> addTab());
        findViewById(R.id.remove_tab).setOnClickListener(v -> removeTab());
    }

    private void addTab() {
        int currentSize = mTabItems.size();

        mTabItems.add(new CarTabItem.Builder()
                .setText("Tab " + currentSize)
                .setIcon(Icon.createWithResource(this, R.drawable.ic_home))
                .build());

        mTabBarView.setTabs(mTabItems);
    }

    private void removeTab() {
        // Can't remove any more items.
        if (mTabItems.size() < 1) {
            return;
        }

        mTabItems.remove(mTabItems.size() - 1);
        mTabBarView.setTabs(mTabItems);
    }
}
