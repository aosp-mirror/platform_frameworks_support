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
import android.util.Log;
import android.util.Xml;

import androidx.collection.ArrayMap;
import androidx.core.util.AtomicFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShortcutInfoCompatSaver {

    public static final String TAG = "ShortcutInfoCompatSaver";

    private static final String DIRECTORY_TARGETS = "share_targets";
    private static final String DIRECTORY_BITMAPS = "bitmaps";
    private static final String FILENAME_XML = "targets.xml";

    private static final String TAG_ROOT = "share_targets";
    private static final String TAG_TARGET = "target";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSON = "person";

    private static final String ATTR_ID = "id";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_SHORT_LABEL = "short_label";
    private static final String ATTR_LONG_LABEL = "long_label";
    private static final String ATTR_DISABLED_MSG = "disabled_message";
    private static final String ATTR_CATEGORY = "disabled_message";
    private static final String ATTR_ALWAYS_BADGED = "always_badged";
    private static final String ATTR_LONG_LIVED = "long_lived";

    private static final String ATTR_ICON_RES_ID = "icon_resource_id";
    private static final String ATTR_ICON_BMP_PATH = "icon_bitmap_path";

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
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Failed to load from file " + file.getAbsolutePath(), e);
            return new ArrayMap<>();
        }

        return shortcuts;
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
                ShortcutInfoCompat info = shortcuts.get(key);
                serializer.startTag(null, TAG_TARGET);
                writeAttr(out, ATTR_ID, info.getId());
                writeAttr(out, ATTR_COMPONENT, sic.getActivity());
                writeAttr(out, ATTR_TITLE, sic.getShortLabel());
                writeAttr(out, ATTR_ICON_RES_ID, sic.getIcon().getResId());
                writeAttr(out, ATTR_ICON_BMP_PATH, sic.getBitmapPath());
                writeBundle(out, TAG_EXTRAS, ATTR_BUNDLE, sic.getChooserIntentExtras());
                for (IntentFilter intentFilter : sic.getChooserIntentFilters()) {
                    out.startTag(null, TAG_INTENT_FILTER);
                    intentFilter.writeToXml(out);
                    out.endTag(null, TAG_INTENT_FILTER);
                }
                serializer.endTag(null, TAG_TARGET);
            }
            serializer.endTag(null, TAG_ROOT);
            serializer.endDocument();

            stream.flush();
            fileStream.flush();
            atomicFile.finishWrite(fileStream);

        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Failed to write to file " + atomicFile.getBaseFile(), e);
            atomicFile.failWrite(fileStream);
            return false;
        }

        return true;
    }
}
