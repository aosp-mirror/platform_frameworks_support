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

package com.example.androidx.car;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.car.widget.CarToolbar;

/**
 * Demo activity for {@link androidx.car.widget.CarToolbar}.
 */
public class CarToolbarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_toolbar);

        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setNavigationIconOnClickListener(v -> {
            this.onSupportNavigateUp();
        });
        ImageButton navButton = findViewById(R.id.nav_button);
        Log.i("yaoyx", "nav button width is " + navButton.getWidth());
        int width = navButton.getWidth() + (10 * 2);
        toolbar.setNavigationIconContainerWidth(width);
    }
}
