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
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import androidx.collection.ArrayMap;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.AtomicFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final String TAG_CATEGORY = "categories";

    private static final String ATTR_ID = "id";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_SHORT_LABEL = "short_label";
    private static final String ATTR_LONG_LABEL = "long_label";

    private static final String ATTR_ICON_RES_NAME = "icon_resource_name";
    private static final String ATTR_ICON_BMP_PATH = "icon_bitmap_path";

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TARGET_PACKAGE = "targetPackage";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String ATTR_NAME = "name";

    private Context mContext;
    private ArrayMap<String, ShortcutContainer> mShortcutsList = new ArrayMap<>();

    private Handler mBackgroundThreadHandler;

    private static ShortcutInfoCompatSaver INSTANCE;

    public static  ShortcutInfoCompatSaver getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ShortcutInfoCompatSaver(context);
        }
        return INSTANCE;
    }

    /*
     * Class to keep ShortcutInfoCompat + Path to its serialized Icon for lazy loading.
     */
    private class ShortcutContainer {
        private String mResourceName;
        private String mBitmapPath;
        private ShortcutInfoCompat mShortcutInfo;

        public ShortcutContainer setResourceName(String resourceName) {
            mResourceName = resourceName;
            return this;
        }

        public ShortcutContainer setBitmapPath(String bitmapPath) {
            mBitmapPath = bitmapPath;
            return this;
        }

        public ShortcutContainer setShortcutInfo(ShortcutInfoCompat shortcut) {
            mShortcutInfo = shortcut;
            return this;
        }

        public IconCompat getIcon() {
            if (!TextUtils.isEmpty(mResourceName)) {
                int id = 0;
                try {
                    id = mContext.getResources().getIdentifier(mResourceName, null, null);
                } catch (Exception e) { /* Do nothing */ }
                if (id != 0) {
                    return IconCompat.createWithResource(mContext, id);
                }
            }

            if (!TextUtils.isEmpty(mBitmapPath)) {
                Bitmap bitmap = loadBitmap(mBitmapPath);
                if (bitmap != null) {
                    return IconCompat.createWithBitmap(bitmap);
                }
            }
            return null;
        }
    }

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

    private void createDirectories() {
        getXmlFile(mContext.getFilesDir()).getParentFile().mkdirs();
        getBitmapsPath(mContext.getFilesDir()).mkdirs();
    }

    private void emptyDirectories() {
        createDirectories();

        getXmlFile(mContext.getFilesDir()).delete();
        for (File bitmap : getBitmapsPath(mContext.getFilesDir()).listFiles()) {
            bitmap.delete();
        }
    }

    private ShortcutInfoCompatSaver(Context context) {
        mContext = context;

        HandlerThread thread = new HandlerThread(TAG + "BackgroundThread");
        thread.start();
        mBackgroundThreadHandler = new Handler(thread.getLooper());

        createDirectories();
        loadFromXml(mShortcutsList);
    }

    private ArrayMap<String, ShortcutContainer> cloneShortcutsList(
            ArrayMap<String, ShortcutContainer> shortcuts) {
        ArrayMap<String, ShortcutContainer> clone = new ArrayMap<>();
        for (ShortcutContainer item : shortcuts.values()) {
            ShortcutContainer cloneContainer = new ShortcutContainer()
                    .setResourceName(item.mResourceName)
                    .setBitmapPath(item.mBitmapPath)
                    .setShortcutInfo(item.mShortcutInfo);
            clone.put(item.mShortcutInfo.getId(), cloneContainer);
        }
        return clone;
    }

    public synchronized List<ShortcutInfoCompat> getShortcuts() {
        ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        for (ShortcutContainer item : mShortcutsList.values()) {
            shortcuts.add(item.mShortcutInfo);
        }
        return shortcuts;
    }

    public synchronized boolean addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        for (ShortcutInfoCompat item : shortcuts) {
            if (mShortcutsList.containsKey(item.getId())) {
                mShortcutsList.get(item.getId()).setShortcutInfo(item);
            } else {
                mShortcutsList.put(item.getId(), new ShortcutContainer().setShortcutInfo(item));
            }
        }

        final ArrayMap<String, ShortcutContainer> clonedList = cloneShortcutsList(mShortcutsList);
        mBackgroundThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                saveAsXml(clonedList);
            }
        });

        return true;
    }

    public synchronized void removeShortcuts(List<String> shortcutIds) {
        final ArrayList<String> pathList = new ArrayList<>();
        for (String id : shortcutIds) {
            if (!mShortcutsList.containsKey(id)) {
                continue;
            }
            String pathToDelete =  mShortcutsList.get(id).mBitmapPath;
            if (!TextUtils.isEmpty(pathToDelete)) {
                pathList.add(pathToDelete);
            }
        }
        mShortcutsList.removeAll(shortcutIds);

        final ArrayMap<String, ShortcutContainer> clonedList = cloneShortcutsList(mShortcutsList);
        mBackgroundThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (String pathToDelete : pathList) {
                    try {
                        File bitmapToDelete = new File(pathToDelete);
                        if (bitmapToDelete.exists()) {
                            bitmapToDelete.delete();
                        }
                    }
                    catch (Exception e) { /* Do nothing */ }
                }
                saveAsXml(clonedList);
            }
        });
    }

    public synchronized void removeAllShortcuts() {
        mShortcutsList.clear();

        final ArrayMap<String, ShortcutContainer> clonedList = cloneShortcutsList(mShortcutsList);
        mBackgroundThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                emptyDirectories();
                saveAsXml(clonedList);
            }
        });
    }

    public synchronized IconCompat getShortcutIcon(String shortcutId) {
        if (mShortcutsList.containsKey(shortcutId)) {
            return mShortcutsList.get(shortcutId).getIcon();
        }
        return null;
    }

    private boolean loadFromXml(ArrayMap<String, ShortcutContainer> shortcutsList) {
        shortcutsList.clear();
        final File file = getXmlFile(mContext.getFilesDir());
        try {
            if (!file.exists()) {
                return false;
            }
            final FileInputStream stream = new FileInputStream(file);
            final XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, "UTF_8");

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_TARGET)) {
                    ShortcutContainer shortcut = parseShortcutContainer(parser);
                    if (shortcut != null && shortcut.mShortcutInfo != null) {
                        shortcutsList.put(shortcut.mShortcutInfo.getId(), shortcut);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load from file " + file.getAbsolutePath(), e);
            shortcutsList.clear();
            return false;
        }
        return true;
    }

    private boolean saveAsXml(ArrayMap<String, ShortcutContainer> shortcutsList) {
        final AtomicFile atomicFile = new AtomicFile(getXmlFile(mContext.getFilesDir()));

        FileOutputStream fileStream = null;
        try {
            fileStream = atomicFile.startWrite();
            final BufferedOutputStream stream = new BufferedOutputStream(fileStream);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(stream, "UTF_8");
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_ROOT);

            for (ShortcutContainer shortcut : shortcutsList.values()) {
                serializeShortcutContainer(serializer, shortcut);
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

    private ShortcutContainer parseShortcutContainer(XmlPullParser parser) throws Exception {
        if (!parser.getName().equals(TAG_TARGET)) {
            return null;
        }

        String id = getAttributeValue(parser, ATTR_ID);
        CharSequence label = getAttributeValue(parser, ATTR_SHORT_LABEL);
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(label)) {
            return null;
        }

        CharSequence longLabel = getAttributeValue(parser, ATTR_LONG_LABEL);
        ComponentName activity = parseComponentName(parser);
        String iconResourceName = getAttributeValue(parser, ATTR_ICON_RES_NAME);
        String iconBitmapPath = getAttributeValue(parser, ATTR_ICON_BMP_PATH);

        ArrayList<Intent> intents = new ArrayList<>();
        Set<String> categories = new HashSet<>();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_INTENT:
                        Intent intent = parseIntent(parser);
                        if (intent != null) {
                            intents.add(intent);
                        }
                        break;
                    case TAG_CATEGORY:
                        String category = getAttributeValue(parser, ATTR_NAME);
                        if (!TextUtils.isEmpty(category)) {
                            categories.add(category);
                        }
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_TARGET)) {
                break;
            }
        }

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(mContext, id)
                .setShortLabel(label);
        if (!TextUtils.isEmpty(longLabel)) {
            builder.setLongLabel(longLabel);
        }
        if (activity != null) {
            builder.setActivity(activity);
        }
        if (!intents.isEmpty()) {
            builder.setIntents(intents.toArray(new Intent[intents.size()]));
        }
        if (!categories.isEmpty()) {
            builder.setCategories(categories);
        }
        return new ShortcutContainer().setShortcutInfo(builder.build())
                .setResourceName(iconResourceName)
                .setBitmapPath(iconBitmapPath);
    }

    private ComponentName parseComponentName(XmlPullParser parser) {
        String value = getAttributeValue(parser, ATTR_COMPONENT);
        return TextUtils.isEmpty(value) ? null : ComponentName.unflattenFromString(value);
    }

    private Intent parseIntent(XmlPullParser parser) {
        String action = getAttributeValue(parser, ATTR_ACTION);
        String targetPackage = getAttributeValue(parser, ATTR_TARGET_PACKAGE);
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);

        if (action == null) {
            return null;
        }
        Intent intent = new Intent(action);
        if (!TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass)) {
            intent.setClassName(targetPackage, targetClass);
        }
        return intent;
    }

    private String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    private void serializeShortcutContainer(XmlSerializer serializer, ShortcutContainer container)
            throws IOException {
        serializer.startTag(null, TAG_TARGET);

        ShortcutInfoCompat shortcut = container.mShortcutInfo;
        serializeAttribute(serializer, ATTR_ID, shortcut.getId());
        serializeAttribute(serializer, ATTR_SHORT_LABEL, shortcut.getShortLabel().toString());
        if (!TextUtils.isEmpty(shortcut.getLongLabel())) {
            serializeAttribute(serializer, ATTR_LONG_LABEL, shortcut.getLongLabel().toString());
        }
        if (shortcut.getActivity() != null) {
            serializeAttribute(serializer, ATTR_COMPONENT,
                    shortcut.getActivity().flattenToString());
        }
        if (shortcut.mIcon != null) {
            saveIconAndUpdateShortcutContainer(container);
        }
        if (!TextUtils.isEmpty(container.mResourceName)) {
            serializeAttribute(serializer, ATTR_ICON_RES_NAME, container.mResourceName);
        }
        if (!TextUtils.isEmpty(container.mBitmapPath)) {
            serializeAttribute(serializer, ATTR_ICON_BMP_PATH, container.mBitmapPath);
        }
        for (Intent intent : shortcut.getIntents()) {
            serializeIntent(serializer, intent);
        }
        for (String category : shortcut.mCategories) {
            serializeCategory(serializer, category);
        }

        serializer.endTag(null, TAG_TARGET);
    }

    private void saveIconAndUpdateShortcutContainer(ShortcutContainer container) {
        IconCompat icon = container.mShortcutInfo.mIcon;
        if (icon == null) {
            return;
        }

        switch (icon.getType()) {
            case Icon.TYPE_RESOURCE:
                String resourceName = mContext.getResources().getResourceName(icon.getResId());
                container.setResourceName(resourceName);
                break;
            case Icon.TYPE_BITMAP:
            case Icon.TYPE_ADAPTIVE_BITMAP:
                String path = getUniqueBitmapFile(mContext.getFilesDir()).getAbsolutePath();
                if (saveBitmap(icon.getBitmap(), path)) {
                    container.setBitmapPath(path);
                }
                break;
        }

        container.mShortcutInfo.mIcon = null;
    }

    private void serializeIntent(XmlSerializer serializer, Intent intent) throws IOException {
        serializer.startTag(null, TAG_INTENT);

        serializeAttribute(serializer, ATTR_ACTION, intent.getAction());
        if (intent.getComponent() != null) {
            serializeAttribute(serializer, ATTR_TARGET_PACKAGE,
                    intent.getComponent().getPackageName());
            serializeAttribute(serializer, ATTR_TARGET_CLASS, intent.getComponent().getClassName());
        }

        serializer.endTag(null, TAG_INTENT);
    }

    private void serializeCategory(XmlSerializer serializer, String category) throws IOException {
        if (TextUtils.isEmpty(category)) {
            return;
        }

        serializer.startTag(null, TAG_CATEGORY);
        serializeAttribute(serializer, ATTR_NAME, category);
        serializer.endTag(null, TAG_CATEGORY);
    }

    private static void serializeAttribute(XmlSerializer serializer, String attribute, String value)
            throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        serializer.attribute(null, attribute, value);
    }

    private static Bitmap loadBitmap(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        try {
            final int READ_SIZE = 512 * 1024;
            byte[] buffer = new byte[READ_SIZE];
            FileInputStream fileStream = new FileInputStream(new File(path));
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                int readSize;
                while ((readSize = fileStream.read(buffer)) > 0) {
                    byteStream.write(buffer, 0, readSize);
                }
                byte[] totalBytes = byteStream.toByteArray();
                return BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length);
            } finally {
                fileStream.close();
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            return null;
        }
    }

    private static boolean saveBitmap(Bitmap bitmap, String path) {
        if (bitmap == null || TextUtils.isEmpty(path)) {
            return false;
        }

        try {
            FileOutputStream fileStream = new FileOutputStream(new File(path));
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, byteStream)) {
                    Log.wtf(TAG, "Unable to compress bitmap");
                }
                byteStream.flush();
                fileStream.write(byteStream.toByteArray());
                fileStream.flush();
            } finally {
                fileStream.close();
                byteStream.close();
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            return false;
        }
        return true;
    }
}
