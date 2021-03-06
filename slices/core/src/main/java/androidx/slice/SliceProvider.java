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
package androidx.slice;

import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceProvider.SLICE_TYPE;

import static androidx.slice.compat.SliceProviderCompat.EXTRA_BIND_URI;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_INTENT;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_PKG;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_PROVIDER_PKG;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_SLICE;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_SLICE_DESCENDANTS;
import static androidx.slice.compat.SliceProviderCompat.METHOD_GET_DESCENDANTS;
import static androidx.slice.compat.SliceProviderCompat.METHOD_GET_PINNED_SPECS;
import static androidx.slice.compat.SliceProviderCompat.METHOD_MAP_INTENT;
import static androidx.slice.compat.SliceProviderCompat.METHOD_MAP_ONLY_INTENT;
import static androidx.slice.compat.SliceProviderCompat.METHOD_PIN;
import static androidx.slice.compat.SliceProviderCompat.METHOD_SLICE;
import static androidx.slice.compat.SliceProviderCompat.METHOD_UNPIN;
import static androidx.slice.compat.SliceProviderCompat.addSpecs;
import static androidx.slice.compat.SliceProviderCompat.getSpecs;
import static androidx.slice.core.SliceHints.HINT_PERMISSION_REQUEST;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.CoreComponentFactory;
import androidx.core.os.BuildCompat;
import androidx.slice.compat.CompatPinnedList;
import androidx.slice.compat.SliceProviderWrapperContainer;
import androidx.slice.core.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A SliceProvider allows an app to provide content to be displayed in system spaces. This content
 * is templated and can contain actions, and the behavior of how it is surfaced is specific to the
 * system surface.
 * <p>
 * Slices are not currently live content. They are bound once and shown to the user. If the content
 * changes due to a callback from user interaction, then
 * {@link ContentResolver#notifyChange(Uri, ContentObserver)} should be used to notify the system.
 * </p>
 * <p>
 * The provider needs to be declared in the manifest to provide the authority for the app. The
 * authority for most slices is expected to match the package of the application.
 * </p>
 *
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg" />}
 * </pre>
 * <p>
 * Slices can be identified by a Uri or by an Intent. To link an Intent with a slice, the provider
 * must have an {@link IntentFilter} matching the slice intent. When a slice is being requested via
 * an intent, {@link #onMapIntentToUri(Intent)} can be called and is expected to return an
 * appropriate Uri representing the slice.
 *
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MY_SLICE_INTENT" />
 *     </intent-filter>
 * </provider>}
 * </pre>
 *
 * @see android.app.slice.Slice
 */
public abstract class SliceProvider extends ContentProvider implements
        CoreComponentFactory.CompatWrapped {

    private static Set<SliceSpec> sSpecs;

    private static final String TAG = "SliceProvider";

    private static final String DATA_PREFIX = "slice_data_";
    private static final long SLICE_BIND_ANR = 2000;

    private static final boolean DEBUG = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private CompatPinnedList mPinnedList;

    private String mCallback;

    /**
     * Implement this to initialize your slice provider on startup.
     * This method is called for all registered slice providers on the
     * application main thread at application launch time.  It must not perform
     * lengthy operations, or application startup will be delayed.
     *
     * <p>You should defer nontrivial initialization (such as opening,
     * upgrading, and scanning databases) until the slice provider is used
     * (via #onBindSlice, etc).  Deferred initialization
     * keeps application startup fast, avoids unnecessary work if the provider
     * turns out not to be needed, and stops database errors (such as a full
     * disk) from halting application launch.
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    public abstract boolean onCreateSliceProvider();

    @Override
    public Object getWrapper() {
        if (BuildCompat.isAtLeastP()) {
            return new SliceProviderWrapperContainer.SliceProviderWrapper(this);
        }
        return null;
    }

    @Override
    public final boolean onCreate() {
        mPinnedList = new CompatPinnedList(getContext(),
                DATA_PREFIX + getClass().getName());
        return onCreateSliceProvider();
    }

    @Override
    public final String getType(Uri uri) {
        if (DEBUG) Log.d(TAG, "getFormat " + uri);
        return SLICE_TYPE;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals(METHOD_SLICE)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            if (Binder.getCallingUid() != Process.myUid()) {
                getContext().enforceUriPermission(uri, Binder.getCallingPid(),
                        Binder.getCallingUid(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        "Slice binding requires write access to the uri");
            }
            Set<SliceSpec> specs = getSpecs(extras);

            Slice s = handleBindSlice(uri, specs, getCallingPackage());
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, s.toBundle());
            return b;
        } else if (method.equals(METHOD_MAP_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = onMapIntentToUri(intent);
            Bundle b = new Bundle();
            if (uri != null) {
                Set<SliceSpec> specs = getSpecs(extras);
                Slice s = handleBindSlice(uri, specs, getCallingPackage());
                b.putParcelable(EXTRA_SLICE, s.toBundle());
            } else {
                b.putParcelable(EXTRA_SLICE, null);
            }
            return b;
        } else if (method.equals(METHOD_MAP_ONLY_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = onMapIntentToUri(intent);
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, uri);
            return b;
        } else if (method.equals(METHOD_PIN)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Set<SliceSpec> specs = getSpecs(extras);
            String pkg = extras.getString(EXTRA_PKG);
            if (mPinnedList.addPin(uri, pkg, specs)) {
                handleSlicePinned(uri);
            }
            return null;
        } else if (method.equals(METHOD_UNPIN)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            String pkg = extras.getString(EXTRA_PKG);
            if (mPinnedList.removePin(uri, pkg)) {
                handleSliceUnpinned(uri);
            }
            return null;
        } else if (method.equals(METHOD_GET_PINNED_SPECS)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Bundle b = new Bundle();
            addSpecs(b, mPinnedList.getSpecs(uri));
            return b;
        } else if (method.equals(METHOD_GET_DESCENDANTS)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Bundle b = new Bundle();
            b.putParcelableArrayList(EXTRA_SLICE_DESCENDANTS,
                    new ArrayList<>(handleGetDescendants(uri)));
            return b;
        }
        return super.call(method, arg, extras);
    }

    private Collection<Uri> handleGetDescendants(Uri uri) {
        mCallback = "onGetSliceDescendants";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            return onGetSliceDescendants(uri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSlicePinned(final Uri sliceUri) {
        mCallback = "onSlicePinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            onSlicePinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSliceUnpinned(final Uri sliceUri) {
        mCallback = "onSliceUnpinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            onSliceUnpinned(sliceUri);
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
        if (Binder.getCallingUid() != Process.myUid()) {
            try {
                getContext().enforceUriPermission(sliceUri,
                        Binder.getCallingPid(), Binder.getCallingUid(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        "Slice binding requires write access to Uri");
            } catch (SecurityException e) {
                return createPermissionSlice(getContext(), sliceUri, pkg);
            }
        }
        return onBindSliceStrict(sliceUri, specs);
    }

    /**
     * Generate a slice that contains a permission request.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Slice createPermissionSlice(Context context, Uri sliceUri,
            String callingPackage) {
        Slice.Builder parent = new Slice.Builder(sliceUri);

        Slice.Builder action = new Slice.Builder(parent)
                .addHints(HINT_TITLE, HINT_SHORTCUT)
                .addAction(createPermissionIntent(context, sliceUri, callingPackage),
                        new Slice.Builder(parent).build(), null);

        parent.addSubSlice(new Slice.Builder(sliceUri.buildUpon().appendPath("permission").build())
                .addText(getPermissionString(context, callingPackage), null)
                .addSubSlice(action.build())
                .build());

        return parent.addHints(HINT_PERMISSION_REQUEST).build();
    }

    /**
     * Create a PendingIntent pointing at the permission dialog.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static PendingIntent createPermissionIntent(Context context, Uri sliceUri,
            String callingPackage) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(),
                "androidx.slice.compat.SlicePermissionActivity"));
        intent.putExtra(EXTRA_BIND_URI, sliceUri);
        intent.putExtra(EXTRA_PKG, callingPackage);
        intent.putExtra(EXTRA_PROVIDER_PKG, context.getPackageName());
        // Unique pending intent.
        intent.setData(sliceUri.buildUpon().appendQueryParameter("package", callingPackage)
                .build());

        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /**
     * Get string describing permission request.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
                return onBindSlice(sliceUri);
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
     * Implemented to create a slice.
     * <p>
     * onBindSlice should return as quickly as possible so that the UI tied
     * to this slice can be responsive. No network or other IO will be allowed
     * during onBindSlice. Any loading that needs to be done should happen
     * in the background with a call to {@link ContentResolver#notifyChange(Uri, ContentObserver)}
     * when the app is ready to provide the complete data in onBindSlice.
     * <p>
     *
     * @see {@link Slice}.
     * @see {@link android.app.slice.Slice#HINT_PARTIAL}
     */
    // TODO: Provide alternate notifyChange that takes in the slice (i.e. notifyChange(Uri, Slice)).
    public abstract Slice onBindSlice(Uri sliceUri);

    /**
     * Called to inform an app that a slice has been pinned.
     * <p>
     * Pinning is a way that slice hosts use to notify apps of which slices
     * they care about updates for. When a slice is pinned the content is
     * expected to be relatively fresh and kept up to date.
     * <p>
     * Being pinned does not provide any escalated privileges for the slice
     * provider. So apps should do things such as turn on syncing or schedule
     * a job in response to a onSlicePinned.
     * <p>
     * Pinned state is not persisted through a reboot, and apps can expect a
     * new call to onSlicePinned for any slices that should remain pinned
     * after a reboot occurs.
     *
     * @param sliceUri The uri of the slice being unpinned.
     * @see #onSliceUnpinned(Uri)
     */
    public void onSlicePinned(Uri sliceUri) {
    }

    /**
     * Called to inform an app that a slices is no longer pinned.
     * <p>
     * This means that no other apps on the device care about updates to this
     * slice anymore and therefore it is not important to be updated. Any syncs
     * or jobs related to this slice should be cancelled.
     * @see #onSlicePinned(Uri)
     */
    public void onSliceUnpinned(Uri sliceUri) {
    }

    /**
     * This method must be overridden if an {@link IntentFilter} is specified on the SliceProvider.
     * In that case, this method can be called and is expected to return a non-null Uri representing
     * a slice. Otherwise this will throw {@link UnsupportedOperationException}.
     *
     * @return Uri representing the slice associated with the provided intent.
     * @see {@link android.app.slice.Slice}
     */
    public @NonNull Uri onMapIntentToUri(Intent intent) {
        throw new UnsupportedOperationException(
                "This provider has not implemented intent to uri mapping");
    }

    /**
     * Obtains a list of slices that are descendants of the specified Uri.
     * <p>
     * Implementing this is optional for a SliceProvider, but does provide a good
     * discovery mechanism for finding slice Uris.
     *
     * @param uri The uri to look for descendants under.
     * @return All slices within the space.
     * @see androidx.slice.SliceManager#getSliceDescendants(Uri)
     */
    public Collection<Uri> onGetSliceDescendants(Uri uri) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    @RequiresApi(28)
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        return null;
    }

    @Nullable
    @Override
    @RequiresApi(16)
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        return null;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return 0;
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    @RequiresApi(19)
    public final Uri canonicalize(@NonNull Uri url) {
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void setSpecs(Set<SliceSpec> specs) {
        sSpecs = specs;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Set<SliceSpec> getCurrentSpecs() {
        return sSpecs;
    }
}
