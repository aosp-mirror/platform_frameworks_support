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

import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Version of {@link AppWidgetProvider} that implements a {@link CallbackReceiver}.
 */
public class AppWidgetProviderWithCallbacks extends AppWidgetProvider implements CallbackReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BroadcastReceiverWithCallbacks.ACTION_BROADCAST_CALLBACK.equals(intent.getAction())) {
            CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());

            CallbackHandlerRegistry.sInstance.invokeCallback(context, this, intent);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public final RemoteCallback createRemoteCallback(Context context,
            String methodName, Object... args) {
        CallbackHandlerRegistry.sInstance.ensureInitialized(getClass());
        Bundle arguments = CallbackHandlerRegistry.sInstance.createArguments(this, methodName,
                args);
        Intent intent = new Intent(BroadcastReceiverWithCallbacks.ACTION_BROADCAST_CALLBACK);
        intent.setComponent(new ComponentName(context.getPackageName(), getClass().getName()));
        intent.putExtras(arguments);
        return new RemoteCallback(context, RemoteCallback.TYPE_RECEIVER, intent,
                getClass().getName(), arguments);
    }
}
