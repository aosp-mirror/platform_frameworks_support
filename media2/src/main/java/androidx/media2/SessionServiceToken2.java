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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.SessionToken2.SessionTokenType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Represents a {@link MediaSessionService2} or a {@link MediaLibraryService2}.
 * <p>
 * If you create a {@link MediaController2}, the controller will bind the service for obtaining
 * a session from it. The controller will control the session from the service.
 *
 * @see MediaController2
 * @see MediaBrowser2
 */
public class SessionServiceToken2 extends Token2 {
    private static final String TAG = "SessionServiceToken2";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {TYPE_SESSION_SERVICE, TYPE_LIBRARY_SERVICE, TYPE_BROWSER_SERVICE_LEGACY})
    public @interface SessionServiceTokenType {
    }

    /**
     * Type for {@link MediaLibrarySession}.
     */
    public static final int TYPE_SESSION_SERVICE = 1;

    /**
     * Type for {@link MediaLibraryService2}.
     */
    public static final int TYPE_LIBRARY_SERVICE = 2;

    /**
     * Type for {@link MediaBrowserServiceCompat}.
     */
    static final int TYPE_BROWSER_SERVICE_LEGACY = 100;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_UID = "android.media.servicetoken2.uid";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_TYPE = "android.media.servicetoken2.type";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final String KEY_COMPONENT_NAME = "android.media.servicetoken2.component_name";

    private final SessionServiceToken2Impl mImpl;

    /**
     * Constructor for the token. You can create token of {@link MediaSessionService2},
     * {@link MediaLibraryService2}, or {@link MediaBrowserServiceCompat}. It can be used by either
     * {@link MediaController2} or {@link MediaBrowser2}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the service.
     */
    public SessionServiceToken2(@NonNull Context context, @NonNull ComponentName serviceComponent) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (serviceComponent == null) {
            throw new IllegalArgumentException("serviceComponent shouldn't be null");
        }

        final PackageManager manager = context.getPackageManager();
        final int uid = getUid(manager, serviceComponent.getPackageName());
        final int type;
        if (isInterfaceDeclared(manager, MediaLibraryService2.SERVICE_INTERFACE,
                serviceComponent)) {
            type = TYPE_LIBRARY_SERVICE;
        } else if (isInterfaceDeclared(manager, MediaSessionService2.SERVICE_INTERFACE,
                serviceComponent)) {
            type = TYPE_SESSION_SERVICE;
        } else if (isInterfaceDeclared(manager, MediaBrowserServiceCompat.SERVICE_INTERFACE,
                serviceComponent)) {
            type = TYPE_BROWSER_SERVICE_LEGACY;
        } else {
            throw new IllegalArgumentException(serviceComponent + " doesn't implement none of"
                    + " MediaSessionService2, MediaLibraryService2, MediaBrowserService nor"
                    + " MediaBrowserServiceCompat. Use service's full name.");
        }
        if (type != TYPE_BROWSER_SERVICE_LEGACY) {
            mImpl = new SessionServiceToken2ImplBase(serviceComponent, uid, type);
        } else {
            mImpl = new SessionServiceToken2ImplLegacy(serviceComponent, uid);
        }
    }

    SessionServiceToken2(SessionServiceToken2Impl impl) {
        mImpl = impl;
    }

    @Override
    public int getUid() {
        return mImpl.getUid();
    }

    /**
     * @return package name
     */
    @Override
    public @NonNull String getPackageName() {
        return mImpl.getPackageName();
    }

    @Override
    public boolean isLegacy() {
        return mImpl.isLegacy();
    }

    public @SessionServiceTokenType int getServiceType() {
        return mImpl.getServiceType();
    }

    @Override
    public @NonNull Bundle toBundle() {
        return mImpl.toBundle();
    }

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return Token2 object
     */
    public static SessionServiceToken2 fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int type = bundle.getInt(KEY_TYPE, -1);
        SessionServiceToken2Impl impl = null;
        switch (type) {
            case TYPE_SESSION_SERVICE:
            case TYPE_LIBRARY_SERVICE:
                impl = SessionServiceToken2ImplBase.fromBundle(bundle);
                break;
            case TYPE_BROWSER_SERVICE_LEGACY:
                impl = SessionServiceToken2ImplLegacy.fromBundle(bundle);
                break;
        }
        return impl != null ? new SessionServiceToken2(impl) : null;
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SessionServiceToken2)) {
            return false;
        }
        SessionServiceToken2 other = (SessionServiceToken2) obj;
        return mImpl.equals(other.mImpl);
    }

    @Override
    public @NonNull String toString() {
        return mImpl.toString();
    }

    /**
     * @return service name. Can be {@code null} for TYPE_SESSION.
     */
    public @NonNull ComponentName getComponentName() {
        return mImpl.getComponentName();
    }

    private static boolean isInterfaceDeclared(PackageManager manager, String serviceInterface,
            ComponentName serviceComponent) {
        Intent serviceIntent = new Intent(serviceInterface);
        // Use queryIntentServices to find services with MediaLibraryService2.SERVICE_INTERFACE.
        // We cannot use resolveService with intent specified class name, because resolveService
        // ignores actions if Intent.setClassName() is specified.
        serviceIntent.setPackage(serviceComponent.getPackageName());

        List<ResolveInfo> list = manager.queryIntentServices(
                serviceIntent, PackageManager.GET_META_DATA);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo resolveInfo = list.get(i);
                if (resolveInfo == null || resolveInfo.serviceInfo == null) {
                    continue;
                }
                if (TextUtils.equals(
                        resolveInfo.serviceInfo.name, serviceComponent.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    interface SessionServiceToken2Impl extends Token2Impl {
        @Nullable ComponentName getComponentName();
        @SessionServiceTokenType int getServiceType();
    }

    private static class SessionServiceToken2ImplBase implements SessionServiceToken2Impl {
        private final ComponentName mComponentName;
        private final int mUid;
        private final @SessionTokenType int mType;

        SessionServiceToken2ImplBase(ComponentName serviceComponent, int uid, int type) {
            if (serviceComponent == null) {
                throw new IllegalArgumentException("serviceComponent shouldn't be null");
            }
            mComponentName = serviceComponent;
            mUid = uid;
            mType = type;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public String getPackageName() {
            return mComponentName.getPackageName();
        }

        @Override
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_COMPONENT_NAME, mComponentName);
            bundle.putInt(KEY_UID, mUid);
            bundle.putInt(KEY_TYPE, mType);
            return bundle;
        }

        public static SessionServiceToken2ImplBase fromBundle(Bundle bundle) {
            Parcelable componentName = bundle.getParcelable(KEY_COMPONENT_NAME);
            if (!(componentName instanceof ComponentName)) {
                return null;
            }
            int uid = bundle.getInt(KEY_UID, -1);
            int type = bundle.getInt(KEY_TYPE);
            return new SessionServiceToken2ImplBase((ComponentName) componentName, uid, type);
        }

        @Override
        public boolean isLegacy() {
            return false;
        }

        @Override
        public ComponentName getComponentName() {
            return mComponentName;
        }

        @Override
        public int getServiceType() {
            return mType;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mUid, mType, mComponentName);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SessionServiceToken2ImplBase)) {
                return false;
            }
            SessionServiceToken2ImplBase other = (SessionServiceToken2ImplBase) obj;
            return mUid == other.mUid
                    && mType == other.mType
                    && ObjectsCompat.equals(mComponentName, other.mComponentName);
        }

        @Override
        public String toString() {
            return "SessionServiceToken2 {cmp=" + mComponentName + "}";
        }
    }

    private static class SessionServiceToken2ImplLegacy implements SessionServiceToken2Impl {
        private final int mUid;
        private final ComponentName mComponentName;

        SessionServiceToken2ImplLegacy(ComponentName serviceComponent, int uid) {
            if (serviceComponent == null) {
                throw new IllegalArgumentException("serviceComponent shouldn't be null.");
            }

            mUid = uid;
            mComponentName = serviceComponent;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public String getPackageName() {
            return mComponentName.getPackageName();
        }

        @Override
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_TYPE, TYPE_BROWSER_SERVICE_LEGACY);
            bundle.putInt(KEY_UID, mUid);
            bundle.putParcelable(KEY_COMPONENT_NAME, mComponentName);
            return bundle;
        }

        public static SessionServiceToken2ImplLegacy fromBundle(Bundle bundle) {
            int uid = bundle.getInt(KEY_UID, UID_UNKNOWN);
            Parcelable componentName = bundle.getParcelable(KEY_COMPONENT_NAME);
            if (!(componentName instanceof ComponentName)) {
                return null;
            }
            return new SessionServiceToken2ImplLegacy((ComponentName) componentName, uid);
        }

        @Override
        public boolean isLegacy() {
            return true;
        }

        @Override
        public ComponentName getComponentName() {
            return mComponentName;
        }

        @Override
        public int getServiceType() {
            return TYPE_BROWSER_SERVICE_LEGACY;
        }

        @Override
        public int hashCode() {
            return mComponentName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj instanceof SessionServiceToken2ImplLegacy)) {
                return false;
            }
            SessionServiceToken2ImplLegacy other = (SessionServiceToken2ImplLegacy) obj;
            return mUid == other.mUid
                    && ObjectsCompat.equals(mComponentName, other.mComponentName);
        }

        @Override
        public String toString() {
            return "SessionServiceToken2 {legacyToken=" + mComponentName + "}";
        }
    }
}
