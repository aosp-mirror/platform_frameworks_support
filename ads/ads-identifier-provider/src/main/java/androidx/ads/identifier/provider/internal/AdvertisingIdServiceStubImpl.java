/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier.provider.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import androidx.ads.identifier.provider.AdvertisingIdProviderInterface;
import androidx.ads.identifier.provider.IAdvertisingIdService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class AdvertisingIdServiceStubImpl extends IAdvertisingIdService.Stub {

    private static final String ADVERTISING_ID_IMPL = "androidx.ads.identifier.provider.impl";

    private AdvertisingIdProviderInterface mProvider;

    AdvertisingIdServiceStubImpl(Context context) {
        if (mProvider != null) {
            return;
        }
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            // This shouldn't be possible since the package is self.
            throw new RuntimeException("Package not found", e);
        }
        String implClassName = applicationInfo.metaData.getString(ADVERTISING_ID_IMPL);
        if (implClassName == null || implClassName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Please set the metadata " + ADVERTISING_ID_IMPL
                            + " to the Ad ID provider implementation class name in your "
                            + "AndroidManifest.xml");
        }
        Class<?> implClass;
        try {
            implClass = Class.forName(implClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Ad ID provider implementation class with name " + implClassName
                            + " not found.");
        }
        Class<? extends AdvertisingIdProviderInterface> providerClass;
        try {
            providerClass = implClass.asSubclass(AdvertisingIdProviderInterface.class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "Ad ID provider implementation class with name " + implClassName
                            + " should implement androidx.ads.identifier.provider"
                            + ".AdvertisingIdProviderInterface.");
        }
        Constructor<? extends AdvertisingIdProviderInterface> constructor;
        try {
            constructor = providerClass.getConstructor(Context.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Ad ID provider implementation class with name " + implClassName
                            + " should has a constructor(Context).");
        }
        try {
            mProvider = constructor.newInstance(context);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getId() throws RemoteException {
        return mProvider.getId();
    }

    @Override
    public boolean isLimitAdTrackingEnabled() throws RemoteException {
        return mProvider.isLimitAdTrackingEnabled();
    }
}
