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

package androidx.core.content.pm;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class XmlShareTargetParser {
    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String TAG_DATA = "data";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_CATEGORY = "category";

    private static final String ATTR_MIME_TYPE = "mimeType";
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TARGET_PACKAGE = "targetPackage";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String ATTR_NAME = "name";

    public static ArrayList<ShareTargetCompat> parseShareTargets(Context context) {
        ArrayList<ShareTargetCompat> targets = new ArrayList<>();
        XmlResourceParser parser = getXmlResourceParser(context);

        try {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                    ShareTargetCompat target = parseShareTarget(parser);
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("METT: Failed to parse the Xml resource: " + e);
        }

        parser.close();
        return targets;
    }

    private static XmlResourceParser getXmlResourceParser(Context context) {
        // TODO: Parse the main manifest to find the right Xml resource
        Resources res = context.getResources();
        return res.getXml(res.getIdentifier("shortcuts", "xml", context.getPackageName()));
    }

    private static ShareTargetCompat parseShareTarget(XmlResourceParser parser) throws Exception {
        String dataType = null;
        Intent intent = null;
        String category = null;

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_DATA:
                        dataType = parseDataType(parser);
                        break;
                    case TAG_INTENT:
                        intent = parseIntent(parser);
                        break;
                    case TAG_CATEGORY:
                        category = parseCategory(parser);
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                break;
            }
        }
        if (dataType == null || intent == null || category == null) {
            return null;
        }
        return new ShareTargetCompat(dataType, intent, category);
    }

    private static String parseDataType(XmlResourceParser parser) {
        return getAttributeValue(parser, ATTR_MIME_TYPE);
    }

    private static Intent parseIntent(XmlResourceParser parser) {
        String action = getAttributeValue(parser, ATTR_ACTION);
        String targetPackage = getAttributeValue(parser, ATTR_TARGET_PACKAGE);
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);
        if (action == null || targetClass == null || targetPackage == null) {
            return null;
        }
        return new Intent(action).setClassName(targetPackage, targetClass);
    }

    private static String parseCategory(XmlResourceParser parser) {
        return getAttributeValue(parser, ATTR_NAME);
    }

    private static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    private static int getAttributeResourceValue(XmlResourceParser parser, String attribute,
            int defaultValue) {
        int value = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", attribute,
                defaultValue);
        if (value == defaultValue) {
            value = parser.getAttributeResourceValue(null, attribute, defaultValue);
        }
        return value;
    }
}
