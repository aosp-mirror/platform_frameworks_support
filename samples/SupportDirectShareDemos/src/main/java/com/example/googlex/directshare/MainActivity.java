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
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final String CATEGORY_TEXT_SHARE_TARGET = "com.example.googlex.directshare.category.TEXT_SHARE_TARGET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.push_targets).setOnClickListener(mOnClickListener);
        findViewById(R.id.remove_targets).setOnClickListener(mOnClickListener);
        findViewById(R.id.parse_share_targets).setOnClickListener(mOnClickListener);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.push_targets:
                    pushDirectShareTargets();
                    break;
                case R.id.remove_targets:
                    removeAllDirectShareTargets();
                    break;
                case R.id.parse_share_targets:
                    parseShareTargets();
                    break;
            }
        }
    };

    private void pushDirectShareTargets() {
        ShortcutInfoCompat.Builder b = new ShortcutInfoCompat.Builder(this, "myid");
        b.setLongLived(true);

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        Set<String> categories = new HashSet<>();
        categories.add(CATEGORY_TEXT_SHARE_TARGET);

        ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        final int MAX_PERSONS = 2;
        for (int i = 0; i < MAX_PERSONS; i++) {
            shortcuts.add(new ShortcutInfoCompat.Builder(this, "Person_" + (i + 1))
                    .setShortLabel("Person_" + (i + 1))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.logo_avatar))
                    .setIntent(intent)
                    .setLongLived(true)
                    .setPerson(new Person.Builder().build())
                    .setCategories(categories)
                    .build());
        }

        ShortcutManagerCompat.addDynamicShortcuts(this, shortcuts); // adding
    }

    private void removeAllDirectShareTargets() {
        System.out.println("METTT: removing all dynamic shortcuts");
        ShortcutManagerCompat.removeAllDynamicShortcuts(this);
    }

    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String TAG_DATA = "data";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_CATEGORY = "category";

    private static final String ATTR_MIME_TYPE = "mimeType";
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TARGET_PACKAGE = "targetPackage";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String ATTR_NAME = "name";

    private void parseShareTargets() {
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.shortcuts);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                    System.out.println("METTT: found share-target start tag!");
                    parseShareTarget(parser);
                }
            }

            parser.close();
        } catch (Exception e) {
            System.out.println("METTT: Failed to parse the Xml resource: " + e);
        }
    }

    private void parseShareTarget(XmlResourceParser parser) throws Exception {
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_DATA:
                        System.out.println("METTT: found data start tag!");
                        parseShareTargetData(parser);
                        break;
                    case TAG_INTENT:
                        System.out.println("METTT: found intent start tag!");
                        parseShareTargetIntent(parser);
                        break;
                    case TAG_CATEGORY:
                        System.out.println("METTT: found category start tag!");
                        parseShareTargetCategory(parser);
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                break;
            }
        }
    }

    private void parseShareTargetData(XmlResourceParser parser) {
        String mimeType = getAttributeValue(parser, ATTR_MIME_TYPE);
        System.out.println("METTT: mimeType=" + mimeType);
    }

    private void parseShareTargetIntent(XmlResourceParser parser) {
        String action = getAttributeValue(parser, ATTR_ACTION);
        String targetPackage = getAttributeValue(parser, ATTR_TARGET_PACKAGE);
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);
        System.out.println("METTT: action=" + action + " targetPackage=" + targetPackage + " targetClass=" + targetClass);
    }

    private void parseShareTargetCategory(XmlResourceParser parser) {
        String name = getAttributeValue(parser, ATTR_NAME);
        System.out.println("METTT: name=" + name);
    }

    protected static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    protected static int getAttributeResourceValue(XmlResourceParser parser, String attribute,
            int defaultValue) {
        int value = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", attribute,
                defaultValue);
        if (value == defaultValue) {
            value = parser.getAttributeResourceValue(null, attribute, defaultValue);
        }
        return value;
    }
}
