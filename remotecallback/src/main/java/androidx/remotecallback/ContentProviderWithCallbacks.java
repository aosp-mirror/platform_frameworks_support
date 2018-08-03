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

package androidx.remotecallback;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Bundle;

public abstract class ContentProviderWithCallbacks extends ContentProvider implements
        CallbackReceiver {

    private String mAuthority;

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        mAuthority = info.authority;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (ProviderRelayReceiver.METHOD_PROVIDER_CALLBACK.equals(method)) {
            CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());
            CallbackHandlerRegistry.sInstance.invoke(this, extras);
        }
        return super.call(method, arg, extras);
    }

    @Override
    public final RemoteCallback createRemoteCallback(Context context,
            String methodName, Object... args) {
        if (mAuthority == null) {
            throw new IllegalStateException(
                    "ContentProvider must be attached before creating callbacks");
        }
        CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());
        Bundle arguments = CallbackHandlerRegistry.sInstance.createArguments(this, methodName,
                args);
        arguments.putString(ProviderRelayReceiver.EXTRA_AUTHORITY, mAuthority);
        Intent intent = new Intent(ProviderRelayReceiver.ACTION_PROVIDER_RELAY);
        intent.setComponent(new ComponentName(context.getPackageName(),
                ProviderRelayReceiver.class.getName()));
        intent.putExtras(arguments);
        return new RemoteCallback(context, RemoteCallback.TYPE_PROVIDER, intent,
                getClass().getName(), arguments);
    }
}
