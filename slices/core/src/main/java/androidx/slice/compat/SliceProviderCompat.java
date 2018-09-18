/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.slice.compat;

import static android.app.slice.Slice.HINT_PERMISSION_REQUEST;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceManager.CATEGORY_SLICE;
import static android.app.slice.SliceManager.SLICE_METADATA_KEY;
import static android.app.slice.SliceProvider.SLICE_TYPE;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;
import androidx.slice.Slice;
import androidx.slice.SliceItemHolder;
import androidx.slice.SliceProvider;
import androidx.slice.SliceProviderUtils;
import androidx.slice.SliceSpec;
import androidx.slice.core.R;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
@RequiresApi(19)
public class SliceProviderCompat {
    public static final String PERMS_PREFIX = "slice_perms_";
    private static final String TAG = "SliceProviderCompat";

    private static final long SLICE_BIND_ANR = 2000;

    public static final String METHOD_SLICE = "bind_slice";
    public static final String METHOD_MAP_INTENT = "map_slice";
    public static final String METHOD_PIN = "pin_slice";
    public static final String METHOD_UNPIN = "unpin_slice";
    public static final String METHOD_MAP_ONLY_INTENT = "map_only";
    public static final String METHOD_GET_DESCENDANTS = "get_descendants";

    public static final String EXTRA_INTENT = "slice_intent";
    public static final String EXTRA_SLICE = "slice";
    public static final String EXTRA_SLICE_DESCENDANTS = "slice_descendants";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    String mCallback;
    private final SliceProvider mProvider;
    private CompatPinnedList mPinnedList;
    private CompatPermissionManager mPermissionManager;

    public SliceProviderCompat(SliceProvider provider, CompatPermissionManager permissionManager,
            Context context) {
        mProvider = provider;
        mContext = context;
        String prefsFile = SliceProviderUtils.DATA_PREFIX + getClass().getName();
        SharedPreferences allFiles = mContext.getSharedPreferences(SliceProviderUtils.ALL_FILES, 0);
        Set<String> files = allFiles.getStringSet(SliceProviderUtils.ALL_FILES,
                Collections.<String>emptySet());
        if (!files.contains(prefsFile)) {
            // Make sure this is editable.
            files = new ArraySet<>(files);
            files.add(prefsFile);
            allFiles.edit()
                    .putStringSet(SliceProviderUtils.ALL_FILES, files)
                    .commit();
        }
        mPinnedList = new CompatPinnedList(mContext, prefsFile);
        mPermissionManager = permissionManager;
    }

    /**
     * Generate a slice that contains a permission request.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @RequiresApi(19)
    public static Slice createPermissionSlice(Context context, Uri sliceUri,
            String callingPackage) {
        PendingIntent action = createPermissionIntent(context, sliceUri, callingPackage);

        Slice.Builder parent = new Slice.Builder(sliceUri);
        Slice.Builder childAction = new Slice.Builder(parent)
                .addIcon(IconCompat.createWithResource(context,
                        R.drawable.abc_ic_permission), null)
                .addHints(Arrays.asList(HINT_TITLE, HINT_SHORTCUT))
                .addAction(action, new Slice.Builder(parent).build(), null);

        TypedValue tv = new TypedValue();
        new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light)
                .getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true);
        int deviceDefaultAccent = tv.data;

        parent.addSubSlice(new Slice.Builder(sliceUri.buildUpon().appendPath("permission").build())
                .addIcon(IconCompat.createWithResource(context,
                        R.drawable.abc_ic_arrow_forward), null)
                .addText(getPermissionString(context, callingPackage), null)
                .addInt(deviceDefaultAccent, SUBTYPE_COLOR)
                .addSubSlice(childAction.build(), null)
                .build(), null);
        return parent.addHints(Arrays.asList(HINT_PERMISSION_REQUEST)).build();
    }

    /**
     * Create a PendingIntent pointing at the permission dialog.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @RequiresApi(19)
    private static PendingIntent createPermissionIntent(Context context, Uri sliceUri,
            String callingPackage) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(),
                "androidx.slice.compat.SlicePermissionActivity"));
        intent.putExtra(SliceProviderUtils.EXTRA_BIND_URI, sliceUri);
        intent.putExtra(SliceProviderUtils.EXTRA_PKG, callingPackage);
        intent.putExtra(SliceProviderUtils.EXTRA_PROVIDER_PKG, context.getPackageName());
        // Unique pending intent.
        intent.setData(sliceUri.buildUpon().appendQueryParameter("package", callingPackage)
                .build());

        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /**
     * Get string describing permission request.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @RequiresApi(19)
    public static CharSequence getPermissionString(Context context, String callingPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            return context.getString(R.string.abc_slices_permission_request,
                    pm.getApplicationInfo(callingPackage, 0).loadLabel(pm),
                    context.getApplicationInfo().loadLabel(pm));
        } catch (PackageManager.NameNotFoundException e) {
            // This shouldn't be possible since the caller is verified.
            throw new RuntimeException("Unknown calling app", e);
        }
    }

    private Context getContext() {
        return mContext;
    }

    public String getCallingPackage() {
        return mProvider.getCallingPackage();
    }

    /**
     * Called by SliceProvider when compat is needed.
     */
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals(METHOD_SLICE)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            Set<SliceSpec> specs = SliceProviderUtils.getSpecs(extras);

            Slice s = handleBindSlice(uri, specs, getCallingPackage());
            Bundle b = new Bundle();
            if (SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE.equals(arg)) {
                synchronized (SliceItemHolder.sSerializeLock) {
                    b.putParcelable(EXTRA_SLICE, s != null ? ParcelUtils.toParcelable(s) : null);
                }
            } else {
                b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
            }
            return b;
        } else if (method.equals(METHOD_MAP_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            if (uri != null) {
                Set<SliceSpec> specs = SliceProviderUtils.getSpecs(extras);
                Slice s = handleBindSlice(uri, specs, getCallingPackage());
                if (SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE.equals(arg)) {
                    synchronized (SliceItemHolder.sSerializeLock) {
                        b.putParcelable(EXTRA_SLICE,
                                s != null ? ParcelUtils.toParcelable(s) : null);
                    }
                } else {
                    b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
                }
            } else {
                b.putParcelable(EXTRA_SLICE, null);
            }
            return b;
        } else if (method.equals(METHOD_MAP_ONLY_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, uri);
            return b;
        } else if (method.equals(METHOD_PIN)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            Set<SliceSpec> specs = SliceProviderUtils.getSpecs(extras);
            String pkg = extras.getString(SliceProviderUtils.EXTRA_PKG);
            if (mPinnedList.addPin(uri, pkg, specs)) {
                handleSlicePinned(uri);
            }
            return null;
        } else if (method.equals(METHOD_UNPIN)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            String pkg = extras.getString(SliceProviderUtils.EXTRA_PKG);
            if (mPinnedList.removePin(uri, pkg)) {
                handleSliceUnpinned(uri);
            }
            return null;
        } else if (method.equals(SliceProviderUtils.METHOD_GET_PINNED_SPECS)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            Bundle b = new Bundle();
            ArraySet<SliceSpec> specs = mPinnedList.getSpecs(uri);
            if (specs.size() == 0) {
                throw new IllegalStateException(uri + " is not pinned");
            }
            SliceProviderUtils.addSpecs(b, specs);
            return b;
        } else if (method.equals(METHOD_GET_DESCENDANTS)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            Bundle b = new Bundle();
            b.putParcelableArrayList(EXTRA_SLICE_DESCENDANTS,
                    new ArrayList<>(handleGetDescendants(uri)));
            return b;
        } else if (method.equals(SliceProviderUtils.METHOD_CHECK_PERMISSION)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            String pkg = extras.getString(SliceProviderUtils.EXTRA_PKG);
            int pid = extras.getInt(SliceProviderUtils.EXTRA_PID);
            int uid = extras.getInt(SliceProviderUtils.EXTRA_UID);
            Bundle b = new Bundle();
            b.putInt(SliceProviderUtils.EXTRA_RESULT,
                    mPermissionManager.checkSlicePermission(uri, pid, uid));
            return b;
        } else if (method.equals(SliceProviderUtils.METHOD_GRANT_PERMISSION)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            String toPkg = extras.getString(SliceProviderUtils.EXTRA_PKG);
            if (Binder.getCallingUid() != Process.myUid()) {
                throw new SecurityException("Only the owning process can manage slice permissions");
            }
            mPermissionManager.grantSlicePermission(uri, toPkg);
        } else if (method.equals(SliceProviderUtils.METHOD_REVOKE_PERMISSION)) {
            Uri uri = extras.getParcelable(SliceProviderUtils.EXTRA_BIND_URI);
            String toPkg = extras.getString(SliceProviderUtils.EXTRA_PKG);
            if (Binder.getCallingUid() != Process.myUid()) {
                throw new SecurityException("Only the owning process can manage slice permissions");
            }
            mPermissionManager.revokeSlicePermission(uri, toPkg);
        }
        return null;
    }

    private Collection<Uri> handleGetDescendants(Uri uri) {
        mCallback = "onGetSliceDescendants";
        return mProvider.onGetSliceDescendants(uri);
    }

    private void handleSlicePinned(final Uri sliceUri) {
        mCallback = "onSlicePinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSlicePinned(sliceUri);
            mProvider.handleSlicePinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSliceUnpinned(final Uri sliceUri) {
        mCallback = "onSliceUnpinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSliceUnpinned(sliceUri);
            mProvider.handleSliceUnpinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private Slice handleBindSlice(final Uri sliceUri, final Set<SliceSpec> specs,
            final String callingPkg) {
        // This can be removed once Slice#bindSlice is removed and everyone is using
        // SliceManager#bindSlice.
        String pkg = callingPkg != null ? callingPkg
                : getContext().getPackageManager().getNameForUid(Binder.getCallingUid());
        if (mPermissionManager.checkSlicePermission(sliceUri, Binder.getCallingPid(),
                Binder.getCallingUid()) != PERMISSION_GRANTED) {
            return createPermissionSlice(getContext(), sliceUri, pkg);
        }
        return onBindSliceStrict(sliceUri, specs);
    }

    private Slice onBindSliceStrict(Uri sliceUri, Set<SliceSpec> specs) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        mCallback = "onBindSlice";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeath()
                    .build());
            SliceProvider.setSpecs(specs);
            try {
                return mProvider.onBindSlice(sliceUri);
            } catch (Exception e) {
                Log.wtf(TAG, "Slice with URI " + sliceUri.toString() + " is invalid.", e);
                return null;
            } finally {
                SliceProvider.setSpecs(null);
                mHandler.removeCallbacks(mAnr);
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private final Runnable mAnr = new Runnable() {
        @Override
        public void run() {
            Process.sendSignal(Process.myPid(), Process.SIGNAL_QUIT);
            Log.wtf(TAG, "Timed out while handling slice callback " + mCallback);
        }
    };

    /**
     * Compat version of {@link SliceProvider#bindSlice}.
     */
    public static Slice bindSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(
                context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(SliceProviderUtils.EXTRA_BIND_URI, uri);
            SliceProviderUtils.addSpecs(extras, supportedSpecs);
            final Bundle res = holder.mProvider.call(METHOD_SLICE,
                    SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            return parseSlice(context, res);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        } finally {
            holder.close();
        }
    }

    /**
     * Compat version of {@link SliceProvider#bindSlice}.
     */
    public static Slice bindSlice(Context context, Intent intent,
            Set<SliceSpec> supportedSpecs) {
        Preconditions.checkNotNull(intent, "intent");
        Preconditions.checkArgument(intent.getComponent() != null || intent.getPackage() != null
                        || intent.getData() != null,
                String.format("Slice intent must be explicit %s", intent));
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return bindSlice(context, intentData, supportedSpecs);
        }
        // Otherwise ask the app
        Intent queryIntent = new Intent(intent);
        if (!queryIntent.hasCategory(CATEGORY_SLICE)) {
            queryIntent.addCategory(CATEGORY_SLICE);
        }
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(queryIntent, 0);
        if (providers == null || providers.isEmpty()) {
            // There are no providers, see if this activity has a direct link.
            ResolveInfo resolve = context.getPackageManager().resolveActivity(intent,
                    PackageManager.GET_META_DATA);
            if (resolve != null && resolve.activityInfo != null
                    && resolve.activityInfo.metaData != null
                    && resolve.activityInfo.metaData.containsKey(SLICE_METADATA_KEY)) {
                return bindSlice(context, Uri.parse(
                        resolve.activityInfo.metaData.getString(SLICE_METADATA_KEY)),
                        supportedSpecs);
            }
            return null;
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(resolver, uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            SliceProviderUtils.addSpecs(extras, supportedSpecs);
            final Bundle res = holder.mProvider.call(METHOD_MAP_INTENT,
                    SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            return parseSlice(context, res);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        } finally {
            holder.close();
        }
    }

    private static Slice parseSlice(final Context context, Bundle res) {
        if (res == null) {
            return null;
        }
        synchronized (SliceItemHolder.sSerializeLock) {
            try {
                SliceItemHolder.sHandler = new SliceItemHolder.HolderHandler() {
                    @Override
                    public void handle(SliceItemHolder holder, String format) {
                        if (holder.mVersionedParcelable instanceof IconCompat) {
                            IconCompat icon = (IconCompat) holder.mVersionedParcelable;
                            icon.checkResource(context);
                            if (icon.getType() == Icon.TYPE_RESOURCE && icon.getResId() == 0) {
                                holder.mVersionedParcelable = null;
                            }
                        }
                    }
                };
                res.setClassLoader(SliceProviderCompat.class.getClassLoader());
                Parcelable parcel = res.getParcelable(EXTRA_SLICE);
                if (parcel == null) {
                    return null;
                }
                if (parcel instanceof Bundle) {
                    return new Slice((Bundle) parcel);
                }
                return ParcelUtils.fromParcelable(parcel);
            } finally {
                SliceItemHolder.sHandler = null;
            }
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#pinSlice}.
     */
    public static void pinSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(
                context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(SliceProviderUtils.EXTRA_BIND_URI, uri);
            extras.putString(SliceProviderUtils.EXTRA_PKG, context.getPackageName());
            SliceProviderUtils.addSpecs(extras, supportedSpecs);
            holder.mProvider.call(METHOD_PIN, SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE,
                    extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to pin slice", e);
        } finally {
            holder.close();
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#unpinSlice}.
     */
    public static void unpinSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        if (SliceProviderUtils.getPinnedSlices(context).contains(uri)) {
            SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(
                    context.getContentResolver(), uri);
            if (holder.mProvider == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            try {
                Bundle extras = new Bundle();
                extras.putParcelable(SliceProviderUtils.EXTRA_BIND_URI, uri);
                extras.putString(SliceProviderUtils.EXTRA_PKG, context.getPackageName());
                SliceProviderUtils.addSpecs(extras, supportedSpecs);
                holder.mProvider.call(METHOD_UNPIN,
                        SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to unpin slice", e);
            } finally {
                holder.close();
            }
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#mapIntentToUri}.
     */
    public static Uri mapIntentToUri(Context context, Intent intent) {
        Preconditions.checkNotNull(intent, "intent");
        Preconditions.checkArgument(intent.getComponent() != null || intent.getPackage() != null
                        || intent.getData() != null,
                String.format("Slice intent must be explicit %s", intent));
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return intentData;
        }
        // Otherwise ask the app
        Intent queryIntent = new Intent(intent);
        if (!queryIntent.hasCategory(CATEGORY_SLICE)) {
            queryIntent.addCategory(CATEGORY_SLICE);
        }
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(queryIntent, 0);
        if (providers == null || providers.isEmpty()) {
            // There are no providers, see if this activity has a direct link.
            ResolveInfo resolve = context.getPackageManager().resolveActivity(intent,
                    PackageManager.GET_META_DATA);
            if (resolve != null && resolve.activityInfo != null
                    && resolve.activityInfo.metaData != null
                    && resolve.activityInfo.metaData.containsKey(SLICE_METADATA_KEY)) {
                return Uri.parse(
                        resolve.activityInfo.metaData.getString(SLICE_METADATA_KEY));
            }
            return null;
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        try (SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(resolver,
                uri)) {
            if (holder.mProvider == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            final Bundle res = holder.mProvider.call(METHOD_MAP_ONLY_INTENT,
                    SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            if (res != null) {
                return res.getParcelable(EXTRA_SLICE);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to map slice", e);
        }
        return null;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getSliceDescendants(Uri)}
     */
    public static @NonNull Collection<Uri> getSliceDescendants(Context context, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (SliceProviderUtils.ProviderHolder holder = SliceProviderUtils.acquireClient(resolver,
                uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(SliceProviderUtils.EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_DESCENDANTS,
                    SliceProviderUtils.ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            if (res != null) {
                return res.getParcelableArrayList(EXTRA_SLICE_DESCENDANTS);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
        return Collections.emptyList();
    }
}
