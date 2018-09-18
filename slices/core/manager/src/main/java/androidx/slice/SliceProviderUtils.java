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

package androidx.slice;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.slice.compat.CompatPinnedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class SliceProviderUtils {
    private static final String TAG = "SliceProviderUtils";

    public static final String DATA_PREFIX = "slice_data_";
    public static final String ALL_FILES = DATA_PREFIX + "all_slice_files";
    public static final String EXTRA_SUPPORTED_SPECS = "specs";
    public static final String EXTRA_SUPPORTED_SPECS_REVS = "revs";

    public static final String EXTRA_BIND_URI = "slice_uri";
    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_PROVIDER_PKG = "provider_pkg";
    public static final String METHOD_GET_PINNED_SPECS = "get_specs";
    public static final String METHOD_CHECK_PERMISSION = "check_perms";
    public static final String METHOD_GRANT_PERMISSION = "grant_perms";
    public static final String METHOD_REVOKE_PERMISSION = "revoke_perms";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_PID = "pid";
    public static final String ARG_SUPPORTS_VERSIONED_PARCELABLE = "supports_versioned_parcelable";
    public static final String EXTRA_RESULT = "result";

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSpecs(Uri)}.
     */
    public static Set<SliceSpec> getPinnedSpecs(Context context, Uri uri) {
        ProviderHolder holder = acquireClient(
                context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_PINNED_SPECS,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            if (res != null) {
                return getSpecs(res);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get pinned specs", e);
        } finally {
            holder.close();
        }
        return null;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#checkSlicePermission}.
     */
    public static int checkSlicePermission(Context context, String packageName, Uri uri, int pid,
            int uid) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, packageName);
            extras.putInt(EXTRA_PID, pid);
            extras.putInt(EXTRA_UID, uid);

            final Bundle res = holder.mProvider.call(METHOD_CHECK_PERMISSION,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            if (res != null) {
                return res.getInt(EXTRA_RESULT);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to check slice permission", e);
        }
        return PERMISSION_DENIED;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#grantSlicePermission}.
     */
    public static void grantSlicePermission(Context context, String packageName, String toPackage,
            Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(
                    METHOD_GRANT_PERMISSION,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE,
                    extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#revokeSlicePermission}.
     */
    public static void revokeSlicePermission(Context context, String packageName, String toPackage,
            Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(
                    METHOD_REVOKE_PERMISSION,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE,
                    extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSlices}.
     */
    public static List<Uri> getPinnedSlices(Context context) {
        ArrayList<Uri> pinnedSlices = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(ALL_FILES, 0);
        Set<String> prefSet = prefs.getStringSet(ALL_FILES,
                Collections.<String>emptySet());
        for (String pref : prefSet) {
            pinnedSlices.addAll(new CompatPinnedList(context, pref).getPinnedSlices());
        }
        return pinnedSlices;
    }

    /**
     * Acquires a ContentProviderClient. Make sure the returned {@link ProviderHolder} is
     * closed after use.
     */
    public static ProviderHolder acquireClient(ContentResolver resolver, Uri uri) {
        ContentProviderClient provider = resolver.acquireUnstableContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for " + uri);
        }
        return new ProviderHolder(provider);
    }

    /**
     * Compat way to push specs through the call.
     */
    public static void addSpecs(Bundle extras, Set<SliceSpec> supportedSpecs) {
        ArrayList<String> types = new ArrayList<>();
        ArrayList<Integer> revs = new ArrayList<>();
        for (SliceSpec spec : supportedSpecs) {
            types.add(spec.getType());
            revs.add(spec.getRevision());
        }
        extras.putStringArrayList(EXTRA_SUPPORTED_SPECS, types);
        extras.putIntegerArrayList(EXTRA_SUPPORTED_SPECS_REVS, revs);
    }

    /**
     * Compat way to push specs through the call.
     */
    public static Set<SliceSpec> getSpecs(Bundle extras) {
        ArraySet<SliceSpec> specs = new ArraySet<>();
        ArrayList<String> types = extras.getStringArrayList(
                EXTRA_SUPPORTED_SPECS);
        ArrayList<Integer> revs = extras.getIntegerArrayList(
                EXTRA_SUPPORTED_SPECS_REVS);
        if (types != null && revs != null) {
            for (int i = 0; i < types.size(); i++) {
                specs.add(new SliceSpec(types.get(i), revs.get(i)));
            }
        }
        return specs;
    }

    /**
     * Holder class for a {@link ContentProviderClient}. Ensure the {@link ProviderHolder}
     * is closed after use.
     */
    public static class ProviderHolder implements AutoCloseable {
        public final ContentProviderClient mProvider;

        ProviderHolder(ContentProviderClient provider) {
            this.mProvider = provider;
        }

        @Override
        public void close() {
            if (mProvider == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mProvider.close();
            } else {
                mProvider.release();
            }
        }
    }

    private SliceProviderUtils() {
    }
}
