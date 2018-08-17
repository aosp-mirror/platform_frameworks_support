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

    private static final String ENABLED_BUTTON_CLICKED_NOTIFICATION =
            "The enabled button was clicked!";
    private static final String DISABLED_BUTTON_CLICKED_NOTIFICATION =
            "The disabled button was clicked!";
    private static final String ENABLED_SWITCH_OFF_NOTIFICATION =
            "The enabled switch is off";
    private static final String ENABLED_SWITCH_ON_NOTIFICATION =
            "The enabled switch is on";
    private static final String DISABLED_SWITCH_OFF_NOTIFICATION =
            "The disabled switch is off";
    private static final String DISABLED_SWITCH_ON_NOTIFICATION =
            "The disabled switch is on";
    private static final String CHECKBOX_CHECKED_NOTIFICATION =
            "Checkbox is checked";
    private static final String CHECKBOX_UNCHECKED_NOTIFICATION =
            "Checkbox is unchecked";
    private static final String EDITTEXT_IS_EMPTY =
            "The EditText box is empty";
    private static final String EDITBOX_CONTAINS =
            "The EditText says: \"";
    private static final String RADIOBUTTON_1_CHECKED_NOTIFICATION =
            "RadioButton1 is checked";
    private static final String RADIOBUTTON_2_CHECKED_NOTIFICATION =
            "RadioButton2 is checked";
    private static final String RADIOBUTTON_1_UNCHECKED_NOTIFICATION =
            "RadioButton1 is unchecked";
    private static final String RADIOBUTTON_2_UNCHECKED_NOTIFICATION =
            "RadioButton2 is unchecked";
    private static final String STAR_CHECKED_NOTIFICATION =
            "Star is checked";
    private static final String STAR_UNCHECKED_NOTIFICATION =
            "Star is unchecked";
    private static final String TOGGLEBUTTON_ON_NOTIFICATION =
            "The ToggleButton is on";
    private static final String TOGGLEBUTTON_OFF_NOTIFICATION =
            "The ToggleButton is off";
    private static final String IS_SELECTED =
            " is selected on the spinner";
    private static final String END_QUOTES = "\"";


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
            enabledButtonNotification.setText(ENABLED_BUTTON_CLICKED_NOTIFICATION);
        });

        disabledButton.setOnClickListener((View v) -> {
            disabledButtonNotification.setText(DISABLED_BUTTON_CLICKED_NOTIFICATION);

        });

        enabledSwitch.setOnCheckedChangeListener((CompoundButton buttonView,
                boolean isChecked) -> {
            if (isChecked) {
                enabledSwitchNotification.setText(ENABLED_SWITCH_ON_NOTIFICATION);
            } else {
                enabledSwitchNotification.setText(ENABLED_SWITCH_OFF_NOTIFICATION);
            }

        });

        disabledSwitch.setOnCheckedChangeListener((CompoundButton buttonView,
                boolean isChecked) -> {
            if (isChecked) {
                disabledSwitchNotification.setText(DISABLED_SWITCH_ON_NOTIFICATION);
            } else {
                disabledSwitchNotification.setText(DISABLED_SWITCH_OFF_NOTIFICATION);
            }

        });

        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                checkBoxNotification.setText(CHECKBOX_CHECKED_NOTIFICATION);
            } else {
                checkBoxNotification.setText(CHECKBOX_UNCHECKED_NOTIFICATION);
            }

        });

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    editTextNotification.setText(EDITTEXT_IS_EMPTY);
                } else {
                    editTextNotification.setText(EDITBOX_CONTAINS + s.toString() + END_QUOTES);
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
                rb1Notification.setText(RADIOBUTTON_1_CHECKED_NOTIFICATION);
            } else {
                rb1Notification.setText(RADIOBUTTON_1_UNCHECKED_NOTIFICATION);
            }

        });

        rb2.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                rb2Notification.setText(RADIOBUTTON_2_CHECKED_NOTIFICATION);
            } else {
                rb2Notification.setText(RADIOBUTTON_2_UNCHECKED_NOTIFICATION);
            }
        });

        starCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                starNotification.setText(STAR_CHECKED_NOTIFICATION);
            } else {
                starNotification.setText(STAR_UNCHECKED_NOTIFICATION);
            }

        });

        toggleButton.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                toggleButtonNotification.setText(TOGGLEBUTTON_ON_NOTIFICATION);
            } else {
                toggleButtonNotification.setText(TOGGLEBUTTON_OFF_NOTIFICATION);
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView,
                    View selectedItemView, int position, long id) {
                spinnerNotification.setText(spinner.getSelectedItem().toString() + IS_SELECTED);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

    }
}
