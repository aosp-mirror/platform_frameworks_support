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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
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

class ShortcutInfoCompatSaver {

    static final String TAG = "ShortcutInfoCompatSaver";

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
    private static final String ATTR_DISABLED_MSG = "disabled_message";

    private static final String ATTR_ICON_RES_NAME = "icon_resource_name";
    private static final String ATTR_ICON_BMP_PATH = "icon_bitmap_path";

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TARGET_PACKAGE = "targetPackage";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String ATTR_NAME = "name";

    private static final String BUNDLE_KEY_BITMAP = "keyBitmap";
    private static final String BUNDLE_KEY_PATH = "keyPath";

    private static final int MSG_LOAD_FROM_XML = 1;
    private static final int MSG_SAVE_TO_XML = 2;
    private static final int MSG_SAVE_BITMAP = 3;
    private static final int MSG_DELETE_BITMAP = 4;

    private final Handler.Callback mBackgroundThreadCallback = new Handler.Callback() {
        @WorkerThread
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_FROM_XML:
                    synchronized (LOCK_OBJECT) {
                        loadFromXml((ArrayMap<String, ShortcutContainer>) msg.obj);
                        mLoadedFromDisk = true;
                        LOCK_OBJECT.notifyAll();
                    }
                    return true;
                case MSG_SAVE_TO_XML:
                    if (mBackgroundThreadHandler.hasMessages(MSG_SAVE_TO_XML)) {
                        // The XML file will be overwritten later, skip.
                        return true;
                    }

                    getXmlFile().getParentFile().mkdirs();
                    getBitmapsPath().mkdirs();

                    saveAsXml((ArrayMap<String, ShortcutContainer>) msg.obj);
                    return true;
                case MSG_SAVE_BITMAP:
                    if (mBackgroundThreadHandler.hasMessages(MSG_SAVE_BITMAP, msg.obj)) {
                        // This bitmap file will be overwritten later, skip.
                        return true;
                    }

                    saveBitmap((Bitmap) msg.getData().getParcelable(BUNDLE_KEY_BITMAP),
                            (String) msg.getData().getCharSequence(BUNDLE_KEY_PATH));
                    return true;
                case MSG_DELETE_BITMAP:
                    try {
                        File fileToDelete = new File((String) msg.obj);
                        if (fileToDelete.exists()) {
                            fileToDelete.delete();
                        }
                    } catch (Exception e) { /* Do nothing */ }
                    return true;
                default:
                    Log.wtf(TAG, "Unknown message type " + msg.what);
                    return false;
            }
        }
    };

    private final Context mContext;
    private final ArrayMap<String, ShortcutContainer> mShortcutsList = new ArrayMap<>();

    private final Handler mBackgroundThreadHandler;

    private static ShortcutInfoCompatSaver sINSTANCE;

    private static final Object LOCK_OBJECT = new Object();
    private boolean mLoadedFromDisk = false;

    static ShortcutInfoCompatSaver getInstance(Context context) {
        synchronized (LOCK_OBJECT) {
            if (sINSTANCE == null) {
                sINSTANCE = new ShortcutInfoCompatSaver(context);
            }
            return sINSTANCE;
        }
    }

    /*
     * Class to keep ShortcutInfoCompat + Path to its serialized Icon for lazy loading.
     */
    private class ShortcutContainer {
        String mResourceName;
        String mBitmapPath;
        ShortcutInfoCompat mShortcutInfo;

        ShortcutContainer setResourceName(String resourceName) {
            mResourceName = resourceName;
            return this;
        }

        ShortcutContainer setBitmapPath(String bitmapPath) {
            mBitmapPath = bitmapPath;
            return this;
        }

        ShortcutContainer setShortcutInfo(ShortcutInfoCompat shortcut) {
            mShortcutInfo = shortcut;
            return this;
        }

        @UiThread
        IconCompat getIcon() {
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
                while (mBackgroundThreadHandler.hasMessages(MSG_SAVE_BITMAP,
                        mShortcutInfo.getId())) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException unused) { /* Do nothing */ }
                }

                Bitmap bitmap = loadBitmap(mBitmapPath);
                if (bitmap != null) {
                    // TODO: Re-create an adaptive icon if the original icon was adaptive
                    return IconCompat.createWithBitmap(bitmap);
                }
            }
            return null;
        }

        @UiThread
        IconCompat prepareIconForSaving() {
            if (mShortcutInfo == null || mShortcutInfo.mIcon == null) {
                return null;
            }
            IconCompat icon = mShortcutInfo.mIcon;
            mShortcutInfo.mIcon = null;

            switch (icon.getType()) {
                case Icon.TYPE_RESOURCE:
                    mResourceName = mContext.getResources().getResourceName(icon.getResId());
                    return null;
                case Icon.TYPE_BITMAP:
                case Icon.TYPE_ADAPTIVE_BITMAP:
                    mBitmapPath = new File(getBitmapsPath(), UUID.randomUUID().toString())
                            .getAbsolutePath();
                    return icon;
            }
            return null;
        }
    }

    @AnyThread
    private File getBitmapsPath() {
        return new File(new File(mContext.getFilesDir(), DIRECTORY_TARGETS), DIRECTORY_BITMAPS);
    }

    @WorkerThread
    private File getXmlFile() {
        return new File(new File(mContext.getFilesDir(), DIRECTORY_TARGETS), FILENAME_XML);
    }

    @UiThread
    private ShortcutInfoCompatSaver(Context context) {
        mContext = context;

        HandlerThread thread = new HandlerThread(TAG + "BackgroundThread");
        thread.start();
        mBackgroundThreadHandler = new Handler(thread.getLooper(), mBackgroundThreadCallback);

        mBackgroundThreadHandler.obtainMessage(MSG_LOAD_FROM_XML, mShortcutsList).sendToTarget();
    }

    @UiThread
    private void awaitLoadingFromDisk() {
        while (!mLoadedFromDisk) {
            try {
                LOCK_OBJECT.wait();
            } catch (InterruptedException unused) { /* Do nothing */ }
        }
    }

    /**
     * For testing ONLY
     */
    @VisibleForTesting
    @UiThread
    void forceReloadFromDisk() {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            mLoadedFromDisk = false;
            mBackgroundThreadHandler.obtainMessage(MSG_LOAD_FROM_XML,
                    mShortcutsList).sendToTarget();
        }
    }

    @UiThread
    private ArrayMap<String, ShortcutContainer> cloneShortcutsList(
            ArrayMap<String, ShortcutContainer> shortcuts) {
        ArrayMap<String, ShortcutContainer> clone = new ArrayMap<>();
        for (ShortcutContainer item : shortcuts.values()) {
            clone.put(item.mShortcutInfo.getId(), new ShortcutContainer()
                    .setResourceName(item.mResourceName)
                    .setBitmapPath(item.mBitmapPath)
                    .setShortcutInfo(item.mShortcutInfo));
        }
        return clone;
    }

    @UiThread
    List<ShortcutInfoCompat> getShortcuts() {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
            for (ShortcutContainer item : mShortcutsList.values()) {
                shortcuts.add(item.mShortcutInfo);
            }
            return shortcuts;
        }
    }

    @UiThread
    boolean addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            for (ShortcutInfoCompat item : shortcuts) {
                Set<String> categories = item.getCategories();
                if (categories == null || categories.isEmpty()) {
                    continue;
                }

                if (mShortcutsList.containsKey(item.getId())) {
                    mShortcutsList.get(item.getId()).setShortcutInfo(item);
                } else {
                    mShortcutsList.put(item.getId(), new ShortcutContainer().setShortcutInfo(item));
                }
            }

            for (ShortcutContainer item : mShortcutsList.values()) {
                IconCompat icon = item.prepareIconForSaving();
                if (icon != null) {
                    Message msg = mBackgroundThreadHandler.obtainMessage(MSG_SAVE_BITMAP,
                            item.mShortcutInfo.getId());
                    Bundle bitmapBundle = new Bundle();
                    bitmapBundle.putParcelable(BUNDLE_KEY_BITMAP, icon.getBitmap());
                    bitmapBundle.putCharSequence(BUNDLE_KEY_PATH, item.mBitmapPath);
                    msg.setData(bitmapBundle);
                    msg.sendToTarget();
                }
            }
            mBackgroundThreadHandler.obtainMessage(MSG_SAVE_TO_XML,
                    cloneShortcutsList(mShortcutsList)).sendToTarget();

            return true;
        }
    }

    @UiThread
    void removeShortcuts(List<String> shortcutIds) {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            for (String id : shortcutIds) {
                if (!mShortcutsList.containsKey(id)) {
                    continue;
                }
                String pathToDelete = mShortcutsList.get(id).mBitmapPath;
                if (!TextUtils.isEmpty(pathToDelete)) {
                    mBackgroundThreadHandler.obtainMessage(MSG_DELETE_BITMAP, pathToDelete);
                }
            }

            mShortcutsList.removeAll(shortcutIds);
            mBackgroundThreadHandler.obtainMessage(MSG_SAVE_TO_XML,
                    cloneShortcutsList(mShortcutsList)).sendToTarget();
        }
    }

    @UiThread
    void removeAllShortcuts() {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            for (ShortcutContainer item : mShortcutsList.values()) {
                String pathToDelete = item.mBitmapPath;
                if (!TextUtils.isEmpty(pathToDelete)) {
                    mBackgroundThreadHandler.obtainMessage(MSG_DELETE_BITMAP, pathToDelete);
                }
            }
            mShortcutsList.clear();
            mBackgroundThreadHandler.obtainMessage(MSG_SAVE_TO_XML,
                    cloneShortcutsList(mShortcutsList)).sendToTarget();
        }
    }

    @UiThread
    IconCompat getShortcutIcon(String shortcutId) {
        synchronized (LOCK_OBJECT) {
            awaitLoadingFromDisk();

            if (mShortcutsList.containsKey(shortcutId)) {
                return mShortcutsList.get(shortcutId).getIcon();
            }
            return null;
        }
    }

    @WorkerThread
    private boolean loadFromXml(ArrayMap<String, ShortcutContainer> shortcutsList) {
        shortcutsList.clear();
        final File file = getXmlFile();
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

    @WorkerThread
    private boolean saveAsXml(ArrayMap<String, ShortcutContainer> shortcutsList) {
        final AtomicFile atomicFile = new AtomicFile(getXmlFile());

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

    @WorkerThread
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
        CharSequence disabledMessage = getAttributeValue(parser, ATTR_DISABLED_MSG);
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
        if (!TextUtils.isEmpty(disabledMessage)) {
            builder.setDisabledMessage(disabledMessage);
        }
        if (activity != null) {
            builder.setActivity(activity);
        }
        if (!intents.isEmpty()) {
            builder.setIntents(intents.toArray(new Intent[0]));
        }
        if (!categories.isEmpty()) {
            builder.setCategories(categories);
        }
        return new ShortcutContainer().setShortcutInfo(builder.build())
                .setResourceName(iconResourceName)
                .setBitmapPath(iconBitmapPath);
    }

    @WorkerThread
    private ComponentName parseComponentName(XmlPullParser parser) {
        String value = getAttributeValue(parser, ATTR_COMPONENT);
        return TextUtils.isEmpty(value) ? null : ComponentName.unflattenFromString(value);
    }

    @WorkerThread
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

    @WorkerThread
    private String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    @WorkerThread
    private void serializeShortcutContainer(XmlSerializer serializer, ShortcutContainer container)
            throws IOException {
        serializer.startTag(null, TAG_TARGET);

        ShortcutInfoCompat shortcut = container.mShortcutInfo;
        serializeAttribute(serializer, ATTR_ID, shortcut.getId());
        serializeAttribute(serializer, ATTR_SHORT_LABEL, shortcut.getShortLabel().toString());
        if (!TextUtils.isEmpty(shortcut.getLongLabel())) {
            serializeAttribute(serializer, ATTR_LONG_LABEL, shortcut.getLongLabel().toString());
        }
        if (!TextUtils.isEmpty(shortcut.getDisabledMessage())) {
            serializeAttribute(serializer, ATTR_DISABLED_MSG,
                    shortcut.getDisabledMessage().toString());
        }
        if (shortcut.getActivity() != null) {
            serializeAttribute(serializer, ATTR_COMPONENT,
                    shortcut.getActivity().flattenToString());
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

    @WorkerThread
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

    @WorkerThread
    private void serializeCategory(XmlSerializer serializer, String category) throws IOException {
        if (TextUtils.isEmpty(category)) {
            return;
        }
        serializer.startTag(null, TAG_CATEGORY);
        serializeAttribute(serializer, ATTR_NAME, category);
        serializer.endTag(null, TAG_CATEGORY);
    }

    @WorkerThread
    private static void serializeAttribute(XmlSerializer serializer, String attribute, String value)
            throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        serializer.attribute(null, attribute, value);
    }

    @UiThread
    private static Bitmap loadBitmap(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        try {
            byte[] buffer = new byte[512 * 1024];
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
            Log.wtf(TAG, "Unable to read bitmap from file", e);
            return null;
        }
    }

    @WorkerThread
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
