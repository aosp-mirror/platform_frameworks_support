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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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
                    loadFromXml((ArrayMap<String, ShortcutContainer>) msg.obj);
                    deleteOldBitmaps((ArrayMap<String, ShortcutContainer>) msg.obj);

                    sLoadingCompleteSignal.countDown();
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
                    ShortcutContainer shortcut = (ShortcutContainer) msg.obj;
                    if (!shortcut.mNeedToSaveIcon) {
                        return true;
                    }

                    if (saveBitmap(shortcut.mShortcutInfo.mIcon.getBitmap(),
                            shortcut.mBitmapPath)) {
                        shortcut.clearInMemoryIcon();
                    }
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

    private static final Object GET_INSTANCE_LOCK = new Object();
    private static ShortcutInfoCompatSaver sINSTANCE;

    private static CountDownLatch sLoadingCompleteSignal = new CountDownLatch(1);

    static ShortcutInfoCompatSaver getInstance(Context context) {
        synchronized (GET_INSTANCE_LOCK) {
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
        private final Object mLock = new Object();

        final String mResourceName;
        final String mBitmapPath;
        final ShortcutInfoCompat mShortcutInfo;

        boolean mNeedToSaveIcon = false;

        ShortcutContainer(ShortcutInfoCompat shortcut) {
            String resourceName = null;
            String bitmapPath = null;

            if (shortcut != null && shortcut.mIcon != null) {
                IconCompat icon = shortcut.mIcon;
                switch (icon.getType()) {
                    case Icon.TYPE_RESOURCE:
                        resourceName = mContext.getResources().getResourceName(icon.getResId());
                        break;
                    case Icon.TYPE_BITMAP:
                    case Icon.TYPE_ADAPTIVE_BITMAP:
                        bitmapPath = new File(getBitmapsPath(), UUID.randomUUID().toString())
                                .getAbsolutePath();
                        mNeedToSaveIcon = true;
                        break;
                }
            }
            mShortcutInfo = shortcut;
            mResourceName = resourceName;
            mBitmapPath = bitmapPath;
        }

        ShortcutContainer(ShortcutInfoCompat shortcut, String resourceName, String bitmapPath) {
            mShortcutInfo = shortcut;
            mResourceName = resourceName;
            mBitmapPath = bitmapPath;
        }

        ShortcutContainer(ShortcutContainer shortcut) {
            mShortcutInfo = shortcut.mShortcutInfo;
            mResourceName = shortcut.mResourceName;
            mBitmapPath = shortcut.mBitmapPath;
            mNeedToSaveIcon = shortcut.mNeedToSaveIcon;
        }

        @AnyThread
        IconCompat getIcon() {
            synchronized (mLock) {
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
                    if (mShortcutInfo.mIcon != null) {
                        return mShortcutInfo.mIcon;
                    }

                    Bitmap bitmap = loadBitmap(mBitmapPath);
                    if (bitmap != null) {
                        // TODO: Re-create an adaptive icon if the original icon was adaptive
                        return IconCompat.createWithBitmap(bitmap);
                    }
                }
                return null;
            }
        }

        @AnyThread void clearInMemoryIcon() {
            synchronized (mLock) {
                mNeedToSaveIcon = false;
                if (mShortcutInfo != null) {
                    mShortcutInfo.mIcon = null;
                }
            }
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

    @AnyThread
    private ShortcutInfoCompatSaver(Context context) {
        mContext = context;

        HandlerThread thread = new HandlerThread(TAG + "BackgroundThread");
        thread.start();
        mBackgroundThreadHandler = new Handler(thread.getLooper(), mBackgroundThreadCallback);

        mBackgroundThreadHandler.obtainMessage(MSG_LOAD_FROM_XML, mShortcutsList).sendToTarget();
    }

    @UiThread
    private void postSaveXmlLocked() {
        ArrayMap<String, ShortcutContainer> clone = new ArrayMap<>(mShortcutsList.size());
        for (ShortcutContainer item : mShortcutsList.values()) {
            clone.put(item.mShortcutInfo.getId(), new ShortcutContainer(item));
        }
        mBackgroundThreadHandler.obtainMessage(MSG_SAVE_TO_XML, clone).sendToTarget();
    }

    @AnyThread
    private void awaitLoadingFromDisk() {
        try {
            sLoadingCompleteSignal.await();
        } catch (InterruptedException unused) { /* Do nothing */ }
    }

    /**
     * For testing ONLY
     */
    @VisibleForTesting
    @UiThread
    void forceReloadFromDisk() {
        awaitLoadingFromDisk();

        synchronized (mShortcutsList) {
            sLoadingCompleteSignal = new CountDownLatch(1);
            mBackgroundThreadHandler.obtainMessage(MSG_LOAD_FROM_XML,
                    mShortcutsList).sendToTarget();
        }
    }

    @UiThread
    List<ShortcutInfoCompat> getShortcuts() {
        awaitLoadingFromDisk();

        synchronized (mShortcutsList) {
            ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
            for (ShortcutContainer item : mShortcutsList.values()) {
                shortcuts.add(item.mShortcutInfo);
            }
            return shortcuts;
        }
    }

    @UiThread
    boolean addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        awaitLoadingFromDisk();

        synchronized (mShortcutsList) {
            for (ShortcutInfoCompat item : shortcuts) {
                Set<String> categories = item.getCategories();
                if (categories == null || categories.isEmpty()) {
                    continue;
                }

                if (mShortcutsList.containsKey(item.getId())) {
                    ShortcutContainer container = mShortcutsList.get(item.getId());
                    if (!TextUtils.isEmpty(container.mBitmapPath)) {
                        container.mNeedToSaveIcon = false;
                        // Delete the old bitmap
                        mBackgroundThreadHandler.obtainMessage(MSG_DELETE_BITMAP,
                                container.mBitmapPath);
                    }
                }
                mShortcutsList.put(item.getId(), new ShortcutContainer(item));
            }

            for (ShortcutContainer item : mShortcutsList.values()) {
                if (item.mNeedToSaveIcon) {
                    mBackgroundThreadHandler.obtainMessage(MSG_SAVE_BITMAP, item).sendToTarget();
                }
            }
            postSaveXmlLocked();

            return true;
        }
    }

    @UiThread
    void removeShortcuts(List<String> shortcutIds) {
        awaitLoadingFromDisk();

        synchronized (mShortcutsList) {
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
            postSaveXmlLocked();
        }
    }

    @UiThread
    void removeAllShortcuts() {
        awaitLoadingFromDisk();

        synchronized (mShortcutsList) {
            for (ShortcutContainer item : mShortcutsList.values()) {
                String pathToDelete = item.mBitmapPath;
                if (!TextUtils.isEmpty(pathToDelete)) {
                    mBackgroundThreadHandler.obtainMessage(MSG_DELETE_BITMAP, pathToDelete);
                }
            }
            mShortcutsList.clear();
            postSaveXmlLocked();
        }
    }

    @AnyThread
    IconCompat getShortcutIcon(String shortcutId) {
        awaitLoadingFromDisk();

        ShortcutContainer container;
        synchronized (mShortcutsList) {
            container = mShortcutsList.get(shortcutId);
        }

        if (container != null) {
            return container.getIcon();
        }

        return null;
    }

    @WorkerThread
    private void deleteOldBitmaps(ArrayMap<String, ShortcutContainer> shortcutsList) {
        List<String> bitmapPaths = new ArrayList<>();
        for (ShortcutContainer item : shortcutsList.values()) {
            if (!TextUtils.isEmpty(item.mBitmapPath)) {
                bitmapPaths.add(item.mBitmapPath);
            }
        }

        for (File bitmap : getBitmapsPath().listFiles()) {
            if (!bitmapPaths.contains(bitmap.getAbsolutePath())) {
                bitmap.delete();
            }
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
        return new ShortcutContainer(builder.build(), iconResourceName, iconBitmapPath);
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

    @AnyThread
    private static Bitmap loadBitmap(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return BitmapFactory.decodeFile(path);
    }

    @WorkerThread
    private static boolean saveBitmap(Bitmap bitmap, String path) {
        if (bitmap == null || TextUtils.isEmpty(path)) {
            return false;
        }

        try (FileOutputStream fileStream = new FileOutputStream(new File(path))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, fileStream)) {
                Log.wtf(TAG, "Unable to compress bitmap");
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            return false;
        }
        return true;
    }
}
