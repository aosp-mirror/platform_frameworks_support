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

package com.example.android.support.appcompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity containing various types of controls to demo their responsiveness. Using the default
 * app compat dark theme.
 */
public class ControlsActivity extends AppCompatActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button normalThemeButton = findViewById(R.id.normal_theme_button);
        Button lightThemeButton = findViewById(R.id.light_theme_button);
        Button dayNightThemeButton = findViewById(R.id.day_night_theme_button);
        Button enabledButton = findViewById(R.id.enabled_button);
        Button disabledButton = findViewById(R.id.disabled_button);
        Switch enabledSwitch = findViewById(R.id.enabled_switch);
        Switch disabledSwitch = findViewById(R.id.disabled_switch);
        CheckBox checkBox = findViewById(R.id.checkbox);
        EditText editText = findViewById(R.id.edittext);
        RadioButton rb1 = findViewById(R.id.rb1);
        RadioButton rb2 = findViewById(R.id.rb2);
        CheckBox starCheckBox = findViewById(R.id.star_checkbox);
        ToggleButton toggleButton = findViewById(R.id.toggle_button);
        Spinner spinner = findViewById(R.id.spinner);

        TextView enabledButtonNotification = findViewById(R.id.enabled_button_notification);
        TextView disabledButtonNotification = findViewById(R.id.disabled_button_notification);
        TextView enabledSwitchNotification = findViewById(R.id.enabled_switch_notification);
        TextView disabledSwitchNotification = findViewById(R.id.disabled_switch_notification);
        TextView checkBoxNotification = findViewById(R.id.checkbox_notification);
        TextView editTextNotification = findViewById(R.id.edittext_notification);
        TextView rb1Notification = findViewById(R.id.rb1_notification);
        TextView rb2Notification = findViewById(R.id.rb2_notification);
        TextView starNotification = findViewById(R.id.star_notification);
        TextView toggleButtonNotification = findViewById(R.id.toggle_button_notification);
        TextView spinnerNotification = findViewById(R.id.spinner_notification);

        normalThemeButton.setOnClickListener((View v) -> {
            try {
                startActivity(new Intent(ControlsActivity.this,
                        ControlsActivity.class));
            } finally {
                finish();
            }
        });

        lightThemeButton.setOnClickListener((View v) -> {
            try {
                startActivity(new Intent(ControlsActivity.this,
                        ControlsLightThemedActivity.class));
            } finally {
                finish();
            }
        });

        dayNightThemeButton.setOnClickListener((View v) -> {
            try {
                startActivity(new Intent(ControlsActivity.this,
                        ControlsDayNightThemedActivity.class));
            } finally {
                finish();
            }
        });

        enabledButton.setOnClickListener((View v) -> {
            enabledButtonNotification.setText(R.string.enabled_button_clicked_notification);
        });

        disabledButton.setOnClickListener((View v) -> {
            disabledButtonNotification.setText(R.string.disabled_button_clicked_notification);

        });

        enabledSwitch.setOnCheckedChangeListener((CompoundButton buttonView,
                boolean isChecked) -> {
            if (isChecked) {
                enabledSwitchNotification.setText(R.string.enabled_switch_on_notification);
            } else {
                enabledSwitchNotification.setText(R.string.enabled_switch_off_notification);
            }

        });

        disabledSwitch.setOnCheckedChangeListener((CompoundButton buttonView,
                boolean isChecked) -> {
            if (isChecked) {
                disabledSwitchNotification.setText(R.string.disabled_switch_on_notification);
            } else {
                disabledSwitchNotification.setText(R.string.disabled_switch_off_notification);
            }

        });

        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                checkBoxNotification.setText(R.string.checkbox_checked_notification);
            } else {
                checkBoxNotification.setText(R.string.checkbox_unchecked_notification);
            }

        });

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    editTextNotification.setText(R.string.edittext_empty);
                } else {
                    editTextNotification.setText(R.string.editbox_contains + s.toString() + "\"");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }
        });

        rb1.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                rb1Notification.setText(R.string.rb1_checked_notification);
            } else {
                rb1Notification.setText(R.string.rb1_unchecked_notification);
            }

        });

        rb2.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                rb2Notification.setText(R.string.rb2_checked_notification);
            } else {
                rb2Notification.setText(R.string.rb2_unchecked_notification);
            }
        });

        starCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                starNotification.setText(R.string.star_checked_notification);
            } else {
                starNotification.setText(R.string.star_unchecked_notification);
            }

        });

        toggleButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                toggleButtonNotification.setText(R.string.toggle_button_on_notification);
            } else {
                toggleButtonNotification.setText(R.string.toggle_button_off_notification);
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                    View selectedItemView, int position, long id) {
                spinnerNotification.setText(spinner.getSelectedItem().toString()
                        + R.string.is_selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

    }
}
