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

package com.example.googlex.directshare;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.push_targets).setOnClickListener((view) -> {
            switch (view.getId()) {
                case R.id.push_targets:
                    pushDirectShareTargets();
                    break;
            }
        });
    }

    private void pushDirectShareTargets() {
        ShortcutInfoCompat.Builder b = new ShortcutInfoCompat.Builder(this, "myid");
        b.setLongLived(true);

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        // The list of DirectShare items. The system will show the items the way they are sorted
        // in this list.
        ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
        final int MAX_PERSONS = 2;
        for (int i = 0; i < MAX_PERSONS; i++) {
            ShortcutInfoCompat shortcutCompat = new ShortcutInfoCompat.Builder(this, "Person_" + (i + 1))
                    .setShortLabel("Person_" + (i + 1))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.logo_avatar))
                    .setIntent(intent)
                    .setLongLived(true)
                    .setPerson(new Person.Builder().build())
                    .setCategories(null)
                    .build();

            shortcuts.add(shortcutCompat.toShortcutInfo());
        }
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        shortcutManager.addDynamicShortcuts(shortcuts); // adding
    }
}
