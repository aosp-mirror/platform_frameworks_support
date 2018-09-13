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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.AnyThread;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.ResolvableFuture;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides APIs to access and update a persistable list of {@link ShortcutInfoCompat}. This class
 * keeps an up-to-date cache of the complete list in memory for quick access, except shortcuts'
 * Icons, which are stored on the disk and only loaded from disk separately if necessary.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
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

    final Context mContext;

    // mShortcutsMap is strictly only accessed by mCacheUpdateService
    final Map<String, ShortcutContainer> mShortcutsMap = new ArrayMap<>();

    // Single threaded tasks queue for updating the in memory cache. All the read/write access to
    // mShortcutsMap must be done using this executor.
    final ExecutorService mCacheUpdateService;
    // Single threaded tasks queue for IO operations on disk
    final ExecutorService mDiskIoService;
    // The maximum time background idle threads will wait for new tasks before terminating
    private static final int EXECUTOR_KEEP_ALIVE_TIME_SECS = 20;

    private static final Object GET_INSTANCE_LOCK = new Object();
    private static volatile ShortcutInfoCompatSaver sINSTANCE;

    @AnyThread
    static ShortcutInfoCompatSaver getInstance(Context context) {
        if (sINSTANCE == null) {
            synchronized (GET_INSTANCE_LOCK) {
                if (sINSTANCE == null) {
                    sINSTANCE = new ShortcutInfoCompatSaver(context,
                            createExecutorService(),
                            createExecutorService());
                }
            }
        }
        return sINSTANCE;
    }

    @AnyThread
    static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
                // Set to 0 to avoid persistent background thread when idle
                0, /* core pool size */
                // Set to 1 to ensure tasks will run strictly in the submit order
                1, /* max pool size */
                EXECUTOR_KEEP_ALIVE_TIME_SECS, /* keep alive time */
                TimeUnit.SECONDS, /* keep alive time unit */
                new LinkedBlockingQueue<Runnable>() /* Not used */);
    }

    /**
     * Class to keep {@link ShortcutInfoCompat}s with extra info (resource name or file path) about
     * their serialized Icon for lazy loading.
     */
    private class ShortcutContainer {
        private final Object mLock = new Object();

        final String mResourceName;
        final String mBitmapPath;
        final ShortcutInfoCompat mShortcutInfo;

        @AnyThread
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
                        // Choose a unique file name to serialize the bitmap
                        bitmapPath = new File(getBitmapsPath(), UUID.randomUUID().toString())
                                .getAbsolutePath();
                        break;
                }
            }
            mShortcutInfo = shortcut;
            mResourceName = resourceName;
            mBitmapPath = bitmapPath;
        }

        @AnyThread
        ShortcutContainer(ShortcutInfoCompat shortcut, String resourceName, String bitmapPath) {
            mShortcutInfo = shortcut;
            mResourceName = resourceName;
            mBitmapPath = bitmapPath;
        }

        @WorkerThread
        IconCompat getIcon() throws Exception {
            if (!TextUtils.isEmpty(mResourceName)) {
                int id = 0;
                try {
                    id = mContext.getResources().getIdentifier(mResourceName, null, null);
                } catch (Exception e) {
                    /* Do nothing, continue and try mBitmapPath */
                }
                if (id != 0) {
                    return IconCompat.createWithResource(mContext, id);
                }
            }

            if (!TextUtils.isEmpty(mBitmapPath)) {
                synchronized (mLock) {
                    if (mShortcutInfo.mIcon != null) {
                        // Icon is still waiting in the queue to be saved. Return from the cache.
                        return mShortcutInfo.mIcon;
                    }
                }
                final String bitmapPath = mBitmapPath;
                Bitmap bitmap = mDiskIoService.submit(
                        new Callable<Bitmap>() {
                            @Override
                            public Bitmap call() {
                                return loadBitmap(bitmapPath);
                            }
                        }).get();
                if (bitmap != null) {
                    // TODO: Re-create an adaptive icon if the original icon was adaptive
                    return IconCompat.createWithBitmap(bitmap);
                }
            }
            return null;
        }

        /**
         * Removes the Icon from the in memory cache. Called after the icon is successfully saved to
         * the disk, or when the shortcut gets updated with a new Icon while it was waiting to be
         * saved, and there is no need to save the old icon anymore.
         */
        @AnyThread
        void clearInMemoryIcon() {
            synchronized (mLock) {
                mShortcutInfo.mIcon = null;
            }
        }

        @AnyThread
        boolean needToSaveIcon() {
            synchronized (mLock) {
                if (mShortcutInfo.mIcon == null) {
                    return false;
                }
                int iconType = mShortcutInfo.mIcon.getType();
                if (iconType == Icon.TYPE_BITMAP || iconType == Icon.TYPE_ADAPTIVE_BITMAP) {
                    return true;
                }
            }
            return false;
        }
    }

    @AnyThread
    ShortcutInfoCompatSaver(Context context, ExecutorService cacheUpdateService,
            ExecutorService diskIoService) {
        mContext = context.getApplicationContext();
        mCacheUpdateService = cacheUpdateService;
        mDiskIoService = diskIoService;

        final ResolvableFuture<Boolean> loadTask = ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    getXmlFile().getParentFile().mkdirs();
                    getBitmapsPath().mkdirs();

                    mShortcutsMap.putAll(loadFromXml());
                    deleteDanglingBitmaps(new ArrayList<>(mShortcutsMap.values()));
                    loadTask.set(true);
                } catch (Exception e) {
                    loadTask.setException(e);
                }
            }
        });
        loadTask.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    loadTask.get();
                } catch (Exception e) {
                    /* Failed to read from disk and initialize the in memory cache */
                }
            }
        }, mCacheUpdateService);
    }

    @AnyThread
    synchronized ResolvableFuture<Boolean> addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        final List<ShortcutInfoCompat> shortcutList = new ArrayList<>(shortcuts);
        final ResolvableFuture<List<ShortcutContainer>> cacheUpdateFuture =
                ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (ShortcutInfoCompat item : shortcutList) {
                        Set<String> categories = item.getCategories();
                        if (categories == null || categories.isEmpty()) {
                            continue;
                        }

                        ShortcutContainer oldContainer = mShortcutsMap.get(item.getId());
                        if (oldContainer != null && !TextUtils.isEmpty(
                                oldContainer.mBitmapPath)) {
                            // Skip saving the old icon, if it is still waiting to get saved.
                            oldContainer.clearInMemoryIcon();
                        }

                        ShortcutContainer container = new ShortcutContainer(item);
                        mShortcutsMap.put(item.getId(), container);
                    }
                    cacheUpdateFuture.set(new ArrayList<>(mShortcutsMap.values()));
                } catch (Exception e) {
                    cacheUpdateFuture.setException(e);
                }
            }
        });

        return asyncSaveMemoryMapToDisk(cacheUpdateFuture);
    }

    @AnyThread
    synchronized ResolvableFuture<Boolean> removeShortcuts(List<String> shortcutIds) {
        final List<String> idList = new ArrayList<>(shortcutIds);
        final ResolvableFuture<List<ShortcutContainer>> cacheUpdateFuture =
                ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String id : idList) {
                        mShortcutsMap.remove(id);
                    }
                    cacheUpdateFuture.set(new ArrayList<>(mShortcutsMap.values()));
                } catch (Exception e) {
                    cacheUpdateFuture.setException(e);
                }
            }
        });

        return asyncSaveMemoryMapToDisk(cacheUpdateFuture);
    }

    @AnyThread
    synchronized ResolvableFuture<Boolean> removeAllShortcuts() {
        final ResolvableFuture<List<ShortcutContainer>> cacheUpdateFuture =
                ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mShortcutsMap.clear();
                    cacheUpdateFuture.set(new ArrayList<>(mShortcutsMap.values()));
                } catch (Exception e) {
                    cacheUpdateFuture.setException(e);
                }
            }
        });

        return asyncSaveMemoryMapToDisk(cacheUpdateFuture);
    }

    ResolvableFuture<Boolean> asyncSaveMemoryMapToDisk(
            final ResolvableFuture<List<ShortcutContainer>> cacheUpdateFuture) {
        final ResolvableFuture<Boolean> result = ResolvableFuture.create();

        cacheUpdateFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    List<ShortcutContainer> containerList = cacheUpdateFuture.get();
                    for (ShortcutContainer container : containerList) {
                        if (container.needToSaveIcon()) {
                            saveBitmap(container.mShortcutInfo.mIcon.getBitmap(),
                                    container.mBitmapPath);
                            container.clearInMemoryIcon();
                        }
                    }
                    deleteDanglingBitmaps(containerList);
                    saveAsXml(containerList);
                    result.set(true);
                } catch (Exception e) {
                    result.setException(e);
                }
            }
        }, mDiskIoService);

        return result;
    }

    @WorkerThread
    List<ShortcutInfoCompat> getShortcuts() throws Exception {
        return mCacheUpdateService.submit(new Callable<ArrayList<ShortcutInfoCompat>>() {
            @Override
            public ArrayList<ShortcutInfoCompat> call() {
                ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
                for (ShortcutContainer item : mShortcutsMap.values()) {
                    shortcuts.add(new ShortcutInfoCompat.Builder(mContext, item.mShortcutInfo)
                            .build());
                }
                return shortcuts;
            }
        }).get();
    }

    @WorkerThread
    IconCompat getShortcutIcon(final String shortcutId) throws Exception {
        ShortcutContainer container = mCacheUpdateService.submit(
                new Callable<ShortcutContainer>() {
                    @Override
                    public ShortcutContainer call() {
                        return mShortcutsMap.get(shortcutId);
                    }
                }).get();
        if (container != null) {
            return container.getIcon();
        }
        return null;
    }

    /**
     * Delete bitmap files from the disk if they are not associated with any shortcuts in the list.
     *
     * Strictly called by mDiskIoService only
     */
    void deleteDanglingBitmaps(List<ShortcutContainer> shortcutsList) {
        List<String> bitmapPaths = new ArrayList<>();
        for (ShortcutContainer item : shortcutsList) {
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

    /**
     * @return Directory where the bitmaps will get stored.
     */
    @AnyThread
    File getBitmapsPath() {
        return new File(new File(mContext.getFilesDir(), DIRECTORY_TARGETS), DIRECTORY_BITMAPS);
    }

    /**
     * @return Path of the XML file that is used to save the list of shortcuts.
     */
    @AnyThread
    File getXmlFile() {
        return new File(new File(mContext.getFilesDir(), DIRECTORY_TARGETS), FILENAME_XML);
    }

    @WorkerThread
    Map<String, ShortcutContainer> loadFromXml() {
        Map<String, ShortcutContainer> shortcutsList = new ArrayMap<>();
        final File file = getXmlFile();
        try {
            if (!file.exists()) {
                return shortcutsList;
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
            throw new RuntimeException("Failed to load from file " + file.getAbsolutePath(), e);
        }
        return shortcutsList;
    }

    @WorkerThread
    void saveAsXml(List<ShortcutContainer> shortcutsList) {
        final AtomicFile atomicFile = new AtomicFile(getXmlFile());
        FileOutputStream fileStream = null;
        try {
            fileStream = atomicFile.startWrite();
            final BufferedOutputStream stream = new BufferedOutputStream(fileStream);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(stream, "UTF_8");
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_ROOT);

            for (ShortcutContainer shortcut : shortcutsList) {
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
            throw new RuntimeException("Failed to write to file " + atomicFile.getBaseFile(), e);
        }
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
    private void serializeAttribute(XmlSerializer serializer, String attribute, String value)
            throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        serializer.attribute(null, attribute, value);
    }

    @WorkerThread
    Bitmap loadBitmap(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return BitmapFactory.decodeFile(path);
    }

    /*
     * Suppress wrong thread warning since Bitmap.compress() and saveBitmap() are both annotated
     * @WorkerThread, but from different packages.
     * androidx.annotation.WorkerThread vs android.annotation.WorkerThread
     */
    @WorkerThread
    @SuppressWarnings("WrongThread")
    void saveBitmap(Bitmap bitmap, String path) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap is null");
        }
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path is empty");
        }

        try (FileOutputStream fileStream = new FileOutputStream(new File(path))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, fileStream)) {
                Log.wtf(TAG, "Unable to compress bitmap");
                throw new RuntimeException("Unable to compress bitmap for saving " + path);
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            throw new RuntimeException("Unable to write bitmap to file " + path, e);
        }
    }
}
