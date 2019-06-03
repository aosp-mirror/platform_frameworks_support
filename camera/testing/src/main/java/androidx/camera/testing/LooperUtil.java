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

package androidx.camera.testing;

import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Utility functions for {@link Looper} */
public final class LooperUtil {
    /** Returns the {@link MessageQueue} for the given {@link Looper}. */
    public static MessageQueue getMessageQueue(Looper looper) {
        if (Build.VERSION.SDK_INT >= 23) {
            return looper.getQueue();
        } else {
            Method getQueue;
            try {
                getQueue = Looper.class.getMethod("getQueue");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to retrieve getQueue via reflection.");
            }
            try {
                return (MessageQueue) getQueue.invoke(looper);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Unable to invoke getQueue via reflection.");
            }
        }
    }

    private LooperUtil() {
    }
}
