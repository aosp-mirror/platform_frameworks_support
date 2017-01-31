/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.app;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LongSparseArray;

import java.lang.reflect.Field;
import java.util.Map;

class ResourcesFlusher {
    private static final String TAG = "ResourcesFlusher";

    private static Field sDrawableCacheField;
    private static boolean sDrawableCacheFieldFetched;

    private static Class sThemedResourceCacheClazz;
    private static boolean sThemedResourceCacheClazzFetched;

    private static Field sThemedResourceCache_mUnthemedEntriesField;
    private static boolean sThemedResourceCache_mUnthemedEntriesFieldFetched;

    private static Field sResourcesImplField;
    private static boolean sResourcesImplFieldFetched;

    static boolean flush(@NonNull final Resources resources) {
        final int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 24) {
            return flushNougats(resources);
        } else if (sdk >= 23) {
            return flushMarshmallows(resources);
        } else if (sdk >= 21) {
            return flushLollipops(resources);
        }
        return false;
    }

    private static boolean flushLollipops(@NonNull final Resources resources) {
        if (!sDrawableCacheFieldFetched) {
            try {
                sDrawableCacheField = Resources.class.getDeclaredField("mDrawableCache");
                sDrawableCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Could not retrieve Resources#mDrawableCache field", e);
            }
            sDrawableCacheFieldFetched = true;
        }
        if (sDrawableCacheField != null) {
            Map drawableCache = null;
            try {
                drawableCache = (Map) sDrawableCacheField.get(resources);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not retrieve value from Resources#mDrawableCache", e);
            }
            if (drawableCache != null) {
                drawableCache.clear();
                return true;
            }
        }
        return false;
    }

    private static boolean flushMarshmallows(@NonNull final Resources resources) {
        if (!sDrawableCacheFieldFetched) {
            try {
                sDrawableCacheField = Resources.class.getDeclaredField("mDrawableCache");
                sDrawableCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Could not retrieve Resources#mDrawableCache field", e);
            }
            sDrawableCacheFieldFetched = true;
        }

        Object drawableCache = null;
        if (sDrawableCacheField != null) {
            try {
                drawableCache = sDrawableCacheField.get(resources);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not retrieve value from Resources#mDrawableCache", e);
            }
        }

        if (drawableCache == null) {
            // If there is no drawable cache, there's nothing to flush...
            return false;
        }

        return drawableCache != null && flushThemedResourcesCache(drawableCache);
    }

    private static boolean flushNougats(@NonNull final Resources resources) {
        if (!sResourcesImplFieldFetched) {
            try {
                sResourcesImplField = Resources.class.getDeclaredField("mResourcesImpl");
                sResourcesImplField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Could not retrieve Resources#mResourcesImpl field", e);
            }
            sResourcesImplFieldFetched = true;
        }

        if (sResourcesImplField == null) {
            // If the mResourcesImpl field isn't available, bail out now
            return false;
        }

        Object resourcesImpl = null;
        try {
            resourcesImpl = sResourcesImplField.get(resources);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Could not retrieve value from Resources#mResourcesImpl", e);
        }

        if (resourcesImpl == null) {
            // If there is no impl instance, bail out now
            return false;
        }

        if (!sDrawableCacheFieldFetched) {
            try {
                sDrawableCacheField = resourcesImpl.getClass().getDeclaredField("mDrawableCache");
                sDrawableCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Could not retrieve ResourcesImpl#mDrawableCache field", e);
            }
            sDrawableCacheFieldFetched = true;
        }

        Object drawableCache = null;
        if (sDrawableCacheField != null) {
            try {
                drawableCache = sDrawableCacheField.get(resourcesImpl);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not retrieve value from ResourcesImpl#mDrawableCache", e);
            }
        }

        return drawableCache != null && flushThemedResourcesCache(drawableCache);
    }

    private static boolean flushThemedResourcesCache(@NonNull final Object cache) {
        if (!sThemedResourceCacheClazzFetched) {
            try {
                sThemedResourceCacheClazz = Class.forName("android.content.res.ThemedResourceCache");
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not find ThemedResourceCache class", e);
            }
            sThemedResourceCacheClazzFetched = true;
        }

        if (sThemedResourceCacheClazz == null) {
            // If the ThemedResourceCache class isn't available, bail out now
            return false;
        }

        if (!sThemedResourceCache_mUnthemedEntriesFieldFetched) {
            try {
                sThemedResourceCache_mUnthemedEntriesField =
                        sThemedResourceCacheClazz.getDeclaredField("mUnthemedEntries");
                sThemedResourceCache_mUnthemedEntriesField.setAccessible(true);
            } catch (NoSuchFieldException ee) {
                Log.e(TAG, "Could not retrieve ThemedResourceCache#mUnthemedEntries field", ee);
            }
            sThemedResourceCache_mUnthemedEntriesFieldFetched = true;
        }

        if (sThemedResourceCache_mUnthemedEntriesField == null) {
            // Didn't get mUnthemedEntries field, bail out...
            return false;
        }

        LongSparseArray unthemedEntries = null;
        try {
            unthemedEntries = (LongSparseArray)
                    sThemedResourceCache_mUnthemedEntriesField.get(cache);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Could not retrieve value from ThemedResourceCache#mUnthemedEntries", e);
        }

        if (unthemedEntries != null) {
            unthemedEntries.clear();
            return true;
        }
        return false;
    }
}
