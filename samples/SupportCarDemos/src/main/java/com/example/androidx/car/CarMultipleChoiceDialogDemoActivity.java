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

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.car.app.CarMultipleChoiceDialog;
import androidx.car.widget.CarToolbar;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * An activity to demo a CarMultipleChoiceDialog. Clicking the create button will display a dialog
 * with a list of multiple-choice list items.
 */
public class CarMultipleChoiceDialogDemoActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_dialog_activity);

        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setTitle(R.string.multiple_choice_dialog_title);
        toolbar.setNavigationIconOnClickListener(v -> onNavigateUp());

        findViewById(R.id.create_dialog).setOnClickListener(v -> createAndShowDialog());
    }

    private void createAndShowDialog() {
        Dialog test = new CarMultipleChoiceDialog.Builder(this)
                .setTitle("Sample multiple choice dialog")
                .setBody("You can put any generic body text here.")
                .setItems(createSelectionItems(), null)
                .setPositiveButton("Okay",
                        (dialog, checkedItems) -> Toast.makeText(
                                CarMultipleChoiceDialogDemoActivity.this,
                                String.format(Locale.getDefault(), "Checked items: %s",
                                        Arrays.toString(checkedItems)), Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel")
                .create();
        test.setCanceledOnTouchOutside(true);
        test.show();
    }

    private List<CarMultipleChoiceDialog.Item> createSelectionItems() {
        List<CarMultipleChoiceDialog.Item> items = new ArrayList<>();

        CarMultipleChoiceDialog.Item item;

        for (int i = 0; i < 5; i++) {
            item = new CarMultipleChoiceDialog.Item(
                    String.format(Locale.getDefault(), "Item with index: %d", i));
            items.add(item);
        }

        for (int i = 5; i < 10; i++) {
            item = new CarMultipleChoiceDialog.Item(
                    String.format(Locale.getDefault(), "Item with index: %d", i), "With body text");
            items.add(item);
        }

        return items;
    }
}
