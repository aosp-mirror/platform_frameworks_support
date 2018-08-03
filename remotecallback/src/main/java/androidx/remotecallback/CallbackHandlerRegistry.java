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

import static androidx.remotecallback.RemoteCallback.EXTRA_METHOD;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.collection.ArrayMap;

import java.lang.reflect.InvocationTargetException;

/**
 * The holder for callbacks that are tagged with {@link RemoteCallable}.
 * Note: This should only be referenced by generated code, there is no reason to reference this
 * otherwise.
 */
public class CallbackHandlerRegistry {
    static final CallbackHandlerRegistry sInstance = new CallbackHandlerRegistry();
    private static final String TAG = "CallbackHandlerRegistry";

    private final ArrayMap<Class, ArrayMap<String, CallbackHandler>> mClsLookup = new ArrayMap<>();

    public <T extends CallbackReceiver> Bundle createArguments(T receiver,
            String methodName, Object[] args) {
        ArrayMap<String, CallbackHandler> map = findMap(receiver.getClass());
        if (map == null) {
            Log.e(TAG, "No map found for " + receiver.getClass().getName());
            return new Bundle();
        }
        CallbackHandler callbackHandler = map.get(methodName);
        if (callbackHandler == null) {
            Log.e(TAG, "No handler found for " + methodName + " on "
                    + receiver.getClass().getName());
            return new Bundle();
        }
        Bundle bundle = ((CallbackHandler<T>) callbackHandler).assembleArguments(args);
        bundle.putString(EXTRA_METHOD, methodName);
        return bundle;
    }

    <T extends CallbackReceiver> void ensureInitialized(Class<T> cls) {
        if (!mClsLookup.containsKey(cls)) {
            runInit(cls);
        }
    }

    <T extends CallbackReceiver> void invoke(T receiver, Intent intent) {
        invoke(receiver, intent.getExtras());
    }

    <T extends CallbackReceiver> void invoke(T receiver, Bundle bundle) {
        ArrayMap<String, CallbackHandler> map = findMap(receiver.getClass());
        if (map == null) {
            Log.e(TAG, "No map found for " + receiver.getClass().getName());
            return;
        }
        String method = bundle.getString(EXTRA_METHOD);
        CallbackHandler callbackHandler = map.get(method);
        if (callbackHandler == null) {
            Log.e(TAG, "No handler found for " + method + " on " + receiver.getClass().getName());
            return;
        }
        ((CallbackHandler<T>) callbackHandler).executeCallback(receiver, bundle);
    }

    private ArrayMap<String, CallbackHandler> findMap(Class<?> aClass) {
        ArrayMap<String, CallbackHandler> map = mClsLookup.get(aClass);
        if (map != null) {
            return map;
        }
        if (aClass.getSuperclass() != null) {
            return findMap(aClass.getSuperclass());
        }
        return null;
    }

    private <T extends CallbackReceiver> void runInit(Class<T> cls) {
        mClsLookup.put(cls, new ArrayMap<String, CallbackHandler>());
        try {
            // This is the only bit of reflection/keeping that needs to exist, one init class
            // per callback receiver.
            ((Runnable) findInitClass(cls).getDeclaredConstructor().newInstance()).run();
        } catch (InstantiationException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        }
    }

    private <T> void registerHandler(Class<T> cls, String method,
            CallbackHandler<T> handler) {
        ArrayMap<String, CallbackHandler> map = mClsLookup.get(cls);
        if (map == null) {
            throw new IllegalStateException("registerHandler called before init was run");
        }
        map.put(method, handler);
    }

    private static Class findInitClass(Class<? extends CallbackReceiver> cls)
            throws ClassNotFoundException {
        String pkg = cls.getPackage().getName();
        String c = String.format("%s.%sInitializer", pkg, cls.getSimpleName());
        return Class.forName(c, false, cls.getClassLoader());
    }

    /**
     * Registers a callback handler to be executed when a given PendingIntent is fired
     * for a {@link RemoteCallback}.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    public static <T> void registerCallbackHandler(Class<T> cls, String method,
            CallbackHandler<T> handler) {
        sInstance.registerHandler(cls, method, handler);
    }

    /**
     * The interface used to trigger a callback when the pending intent is fired.
     * Note: This should only be referenced by generated code, there is no reason to reference
     * this otherwise.
     */
    public interface CallbackHandler<T> {
        /**
         * Executes a callback given a Bundle of aurgements.
         * Note: This should only be called by generated code, there is no reason to reference this
         * otherwise.
         */
        public void executeCallback(T receiver, Bundle arguments);

        /**
         * Transforms arguments into a Bundle that can cross processes and later be used with
         * {@link #executeCallback(Object, Bundle)}.
         * Note: This should only be called by generated code, there is no reason to reference this
         * otherwise.
         */
        public Bundle assembleArguments(Object... args);
    }
}
