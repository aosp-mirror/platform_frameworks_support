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

import android.content.Context;

/**
 * An objects that can receive remote callbacks.
 * <p>
 * Remote callbacks provide an easy way to bundle arguments and pass them
 * directly into a method rather than managing PendingIntents manually.
 * <p>
 * Example:
 * <pre>public class MyReceiver extends BroadcastReceiverWithCallbacks {
 *   public PendingIntent getPendingIntent(Context context, int value1, int value2) {
 *     return createRemoteCallback(context, "doMyAction", value1, value2)
 *         .toPendingIntent();
 *   }
 *
 *   @RemoteCallable
 *   public void doMyAction(int value1, int value2) {
 *     ...
 *   }
 * }</pre>
 * <p>
 * The following types are supported as parameter types for methods tagged
 * with {@link RemoteCallable}.
 * <ul>
 * <li>byte/Byte/byte[]</li>
 * <li>char/Character/char[]</li>
 * <li>short/Short/short[]</li>
 * <li>int/Integer/int[]</li>
 * <li>long/Long/long[]</li>
 * <li>float/Float/float[]</li>
 * <li>double/Double/double[]</li>
 * <li>boolean/Boolean/boolean[]</li>
 * <li>String/String[]</li>
 * <li>Uri</li>
 * <li>Context *</li>
 * </ul>
 * * Context is a special kind of parameter, in that it cannot be specified
 *   during createRemoteCallback and does not count toward the parameter count,
 *   it instead is passed directly through to provide a valid context at the time
 *   of the callback in case no other one is available.
 * <p>
 * This interface shouldn't be implemented in apps, instead extend one of
 * the implementations of it provided.
 * <ul>
 * <li>{@link BroadcastReceiverWithCallbacks}</li>
 * <li>{@link AppWidgetProviderWithCallbacks}</li>
 * <li>{@link ContentProviderWithCallbacks}</li>
 * </ul>
 * <p>
 * Just like PendingIntents, Remote Callbacks don't require components be
 * exported. They also ensure that all parameters always have a value in the
 * PendingIntent generated, which ensures that the caller cannot inject new values
 * except when explicitly requested by the receiving app. They also generate the
 * intent Uris to ensure that the callbacks stay separate and don't collide with
 * each other.
 *
 * @see RemoteCallable
 * @see RemoteInputHolder
 */
public interface CallbackReceiver {

    /**
     * Creates a {@link RemoteCallback} that will call the method with name
     * methodName with the arguments specified when triggered. Only methods
     * tagged with {@link RemoteCallable} can be used here.
     */
    RemoteCallback createRemoteCallback(Context context, String methodName,
            Object... args);
}
