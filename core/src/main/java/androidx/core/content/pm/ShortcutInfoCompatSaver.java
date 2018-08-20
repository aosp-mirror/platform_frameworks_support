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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import androidx.collection.ArrayMap;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.AtomicFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShortcutInfoCompatSaver {

    public static final String TAG = "ShortcutInfoCompatSaver";

    private static final String DIRECTORY_TARGETS = "share_targets";
    private static final String DIRECTORY_BITMAPS = "bitmaps";
    private static final String FILENAME_XML = "targets.xml";

    private static final String TAG_ROOT = "share_targets";
    private static final String TAG_TARGET = "target";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSON = "person";
    private static final String TAG_CATEGORY = "categories";

    private static final String ATTR_ID = "id";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_SHORT_LABEL = "short_label";
    private static final String ATTR_LONG_LABEL = "long_label";
    private static final String ATTR_DISABLED_MSG = "disabled_message";
    private static final String ATTR_ALWAYS_BADGED = "always_badged";
    private static final String ATTR_LONG_LIVED = "long_lived";

    private static final String ATTR_ICON_RES_ID = "icon_resource_id";
    private static final String ATTR_ICON_BMP_PATH = "icon_bitmap_path";

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TARGET_PACKAGE = "targetPackage";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String ATTR_NAME = "name";

    private static final String ATTR_URI = "uri";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_BOT = "bot";
    private static final String ATTR_IMPORTANT = "important";

    private static File getUniqueBitmapFile(File appFilesPath) {
        return new File(getBitmapsPath(appFilesPath), UUID.randomUUID().toString());
    }

    private static File getBitmapsPath(File appFilesPath) {
        return new File(getTargetsPath(appFilesPath), DIRECTORY_BITMAPS);
    }

    private static File getXmlFile(File appFilesPath) {
        return new File(getTargetsPath(appFilesPath), FILENAME_XML);
    }

    private static File getTargetsPath(File appFilesPath) {
        return new File(appFilesPath, DIRECTORY_TARGETS);
    }

    private ShortcutInfoCompatSaver() {
        /* Hide constructor */
    }

    public static ArrayMap<String, ShortcutInfoCompat> loadFromXml(Context context) {
        ArrayMap<String, ShortcutInfoCompat> shortcuts = new ArrayMap<>();
        final File file = getXmlFile(context.getFilesDir());
        try {
            if (!file.exists()) {
                return shortcuts;
            }
            final FileInputStream stream = new FileInputStream(file);
            final XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, "UTF_8");

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_TARGET)) {
                    ShortcutInfoCompat info = parseShortcutInfoCompat(context, parser);
                    if (info != null && !TextUtils.isEmpty(info.getId())) {
                        shortcuts.put(info.getId(), info);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load from file " + file.getAbsolutePath(), e);
            return new ArrayMap<>();
        }
        return shortcuts;
    }

    private static ShortcutInfoCompat parseShortcutInfoCompat(Context context, XmlPullParser parser) throws Exception {
        if (!parser.getName().equals(TAG_TARGET)) {
            return null;
        }

        String id = getAttributeValue(parser, ATTR_ID);
        CharSequence label = getAttributeValue(parser, ATTR_SHORT_LABEL);
        CharSequence longLabel = getAttributeValue(parser, ATTR_LONG_LABEL);
        CharSequence disabledMessage = getAttributeValue(parser, ATTR_DISABLED_MSG);
        boolean isAlwaysBadged = parseBoolean(parser, ATTR_ALWAYS_BADGED, false);
        boolean isLongLived = parseBoolean(parser, ATTR_LONG_LIVED, false);
        ComponentName activity = parseComponentName(parser);
        IconCompat icon = parseIconCompat(context, parser);

        ArrayList<Intent> intents = new ArrayList<>();
        ArrayList<Person> persons = new ArrayList<>();
        Set<String> categories = new HashSet<>();

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_INTENT:
                        intents.add(parseIntent(parser));
                        break;
                    case TAG_PERSON:
                        persons.add(parsePerson(context, parser));
                        break;
                    case TAG_CATEGORY:
                        categories.add(parseCategory(parser));
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_TARGET)) {
                break;
            }
        }

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(label)
                .setLongLabel(longLabel)
                .setDisabledMessage(disabledMessage);
        if (isAlwaysBadged) {
            builder.setAlwaysBadged();
        }
        if (isLongLived) {
            builder.setLongLived();
        }
        if (activity != null) {
            builder.setActivity(activity);
        }
        if (icon != null) {
            builder.setIcon(icon);
        }
        if (!intents.isEmpty()) {
            builder.setIntents(intents.toArray(new Intent[intents.size()]));
        }
        if (!persons.isEmpty()) {
            builder.setPersons(intents.toArray(new Person[persons.size()]));
        }
        if (!categories.isEmpty()) {
            builder.setCategories(categories);
        }
        return builder.build();
    }

    private static boolean parseBoolean(XmlPullParser parser, String attribute, boolean defaultValue) {
        String value = getAttributeValue(parser, attribute);
        return TextUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    private static ComponentName parseComponentName(XmlPullParser parser) {
        String value = getAttributeValue(parser, ATTR_COMPONENT);
        return TextUtils.isEmpty(value) ? null : ComponentName.unflattenFromString(value);
    }

    private static IconCompat parseIconCompat(Context context, XmlPullParser parser) {
        String resourceId = getAttributeValue(parser, ATTR_ICON_RES_ID);
        if (!TextUtils.isEmpty(resourceId)) {
            int id = 0;
            try {
                id = Integer.parseInt(resourceId);
            } catch (Exception e) { /* Do nothing */ }
            if (id != 0) {
                return IconCompat.createWithResource(context, id);
            }
        }

        String bitmapPath = getAttributeValue(parser, ATTR_ICON_BMP_PATH);
        if (!TextUtils.isEmpty(bitmapPath)) {
            Bitmap bitmap = loadBitmap(bitmapPath);
            if (bitmap != null) {
                return IconCompat.createWithBitmap(bitmap);
            }
        }

        return null;
    }

    private static Intent parseIntent(XmlPullParser parser) {
        String action = getAttributeValue(parser, ATTR_ACTION);
        String targetPackage = getAttributeValue(parser, ATTR_TARGET_PACKAGE);
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);
        return new Intent(action).setClassName(targetPackage, targetClass);
    }

    private static Person parsePerson(Context context, XmlPullParser parser) {
        if (!parser.getName().equals(TAG_PERSON)) {
            return null;
        }

        CharSequence name = getAttributeValue(parser, ATTR_NAME);
        String uri = getAttributeValue(parser, ATTR_URI);
        String key = getAttributeValue(parser, ATTR_KEY);
        boolean isBot = parseBoolean(parser, ATTR_BOT, false);
        boolean isImportant = parseBoolean(parser, ATTR_IMPORTANT, false);
        IconCompat icon = parseIconCompat(context, parser);

        Person.Builder builder = new Person.Builder()
                .setName(name)
                .setUri(uri)
                .setKey(key)
                .setBot(isBot)
                .setImportant(isImportant);
        if (icon != null) {
            builder.setIcon(icon);
        }

        return builder.build();
    }

    private static String parseCategory(XmlPullParser parser) {
        return getAttributeValue(parser, ATTR_NAME);
    }

    private static String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android", attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    static boolean saveAsXml(Context context, ArrayMap<String, ShortcutInfoCompat> shortcuts) {
        final File file = getXmlFile(context.getFilesDir());
        file.getParentFile().mkdirs();
        final AtomicFile atomicFile = new AtomicFile(file);

        FileOutputStream fileStream = null;
        try {
            fileStream = atomicFile.startWrite();
            final BufferedOutputStream stream = new BufferedOutputStream(fileStream);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(stream, "UTF_8");
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_ROOT);

            for (String key : shortcuts.keySet()) {
                serializeShortcutInfoCompat(context, serializer, shortcuts.get(key));
            }

            serializer.endTag(null, TAG_ROOT);
            serializer.endDocument();

            stream.flush();
            fileStream.flush();
            atomicFile.finishWrite(fileStream);

        } catch (Exception e) {
            Log.e(TAG, "Failed to write to file " + atomicFile.getBaseFile(), e);
            atomicFile.failWrite(fileStream);
            return false;
        }

        return true;
    }

    private static void serializeShortcutInfoCompat(Context context, XmlSerializer serializer, ShortcutInfoCompat shortcut) throws IOException {
        serializer.startTag(null, TAG_TARGET);

        serializeAttribute(serializer, ATTR_ID, shortcut.getId());
        serializeAttribute(serializer, ATTR_SHORT_LABEL, shortcut.getShortLabel().toString());
        serializeAttribute(serializer, ATTR_LONG_LABEL, shortcut.getLongLabel().toString());
        serializeAttribute(serializer, ATTR_DISABLED_MSG, shortcut.getDisabledMessage().toString());
        serializeAttribute(serializer, ATTR_ALWAYS_BADGED, Boolean.toString(shortcut.getAlwaysBadged()));
        serializeAttribute(serializer, ATTR_LONG_LIVED, Boolean.toString(shortcut.getLongLived()));
        serializeAttribute(serializer, ATTR_COMPONENT, shortcut.getActivity().flattenToString());
        serializeIconCompat(context, serializer, shortcut.getIcon());
        for (Intent intent : shortcut.getIntents()) {
            serializeIntent(serializer, intent);
        }
        for (Person person : shortcut.getPersons()) {
            serializePerson(context, serializer, person);
        }
        for (String category : shortcut.getCategories()) {
            serializeCategory(serializer, category);
        }

        serializer.endTag(null, TAG_TARGET);
    }

    private static void serializeIconCompat(Context context, XmlSerializer serializer, IconCompat icon) throws IOException {
        switch (icon.getType()) {
            case Icon.TYPE_RESOURCE:
                serializeAttribute(serializer, ATTR_ICON_RES_ID, Integer.toString(icon.getResId()));
                break;
            case Icon.TYPE_BITMAP:
                String path = getUniqueBitmapFile(context.getFilesDir()).getAbsolutePath();
                if (saveBitmap(icon.getBitmap(), path)) {
                    serializeAttribute(serializer, ATTR_ICON_BMP_PATH, path);
                }
                break;
            default:
        }
    }

    private static void serializeIntent(XmlSerializer serializer, Intent intent) throws IOException {
        serializer.startTag(null, TAG_INTENT);

        serializeAttribute(serializer, ATTR_ACTION, intent.getAction());
        serializeAttribute(serializer, ATTR_TARGET_PACKAGE, intent.getComponent().getPackageName());
        serializeAttribute(serializer, ATTR_TARGET_CLASS, intent.getComponent().getClassName());

        serializer.endTag(null, TAG_INTENT);
    }

    private static void serializePerson(Context context, XmlSerializer serializer, Person person) throws IOException {
        serializer.startTag(null, TAG_PERSON);

        serializeAttribute(serializer, ATTR_NAME, person.getName().toString());
        serializeAttribute(serializer, ATTR_URI, person.getUri());
        serializeAttribute(serializer, ATTR_KEY, person.getKey());
        serializeAttribute(serializer, ATTR_BOT, Boolean.toString(person.isBot()));
        serializeAttribute(serializer, ATTR_IMPORTANT, Boolean.toString(person.isImportant()));
        serializeIconCompat(context, serializer, person.getIcon());

        serializer.endTag(null, TAG_PERSON);
    }

    private static void serializeCategory(XmlSerializer serializer, String category) throws IOException {
        serializer.startTag(null, TAG_CATEGORY);

        serializeAttribute(serializer, ATTR_NAME, category);

        serializer.endTag(null, TAG_CATEGORY);
    }

    private static void serializeAttribute(XmlSerializer serializer, String attribute, String value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        serializer.attribute(null, attribute, value);
    }

    final static int MAX_BITMAP_BYTES = 512 * 1024;

    private static Bitmap loadBitmap(String path) {
        try {
            byte[] bytes = new byte[MAX_BITMAP_BYTES + 1];
            FileInputStream fileStream = new FileInputStream(new File(path));

            try {
                int readSize = fileStream.read(bytes);
                if (readSize > MAX_BITMAP_BYTES) {
                    Log.wtf(TAG, "Bitmap file size exceeds maximum allowed. Dropping.");
                } else if (readSize > 0) {
                    return BitmapFactory.decodeByteArray(bytes, 0, readSize);
                }
            } finally {
                fileStream.close();
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            return null;
        }
        return null;
    }

    private static boolean saveBitmap(Bitmap bitmap, String path) {
        try {
            FileOutputStream fileStream = new FileOutputStream(new File(path));
            ByteArrayOutputStream stream = new ByteArrayOutputStream(MAX_BITMAP_BYTES);
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, stream)) {
                    Log.wtf(TAG, "Unable to compress bitmap");
                }
                stream.flush();
                byte[] bytes = stream.toByteArray();
                if (bytes.length > MAX_BITMAP_BYTES) {
                    Log.wtf(TAG, "Bitmap is too large to write to file. Dropping.");
                } else {
                    fileStream.write(bytes);
                    fileStream.flush();
                }
            } finally {
                fileStream.close();
                stream.close();
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            return false;
        }
        return true;
    }
}
