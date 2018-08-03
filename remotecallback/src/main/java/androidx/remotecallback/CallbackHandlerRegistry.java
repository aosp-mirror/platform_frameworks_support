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

import android.content.Context;
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

    private final ArrayMap<Class<? extends CallbackReceiver>,
            ArrayMap<String, CallbackHandler<? extends CallbackReceiver>>> mClsLookup =
                    new ArrayMap<>();

    /**
     * Takes a set of arguments for the specified method/receiver and turns
     * them into the Bundle of arguments that will be used to invoke the
     * method.
     *
     * This method will also validate the number of arguments and their types,
     * throwing an IllegalArgumentException or a ClassCastException
     * respectively if anything is wrong.
     */
    public <T extends CallbackReceiver> Bundle createArguments(T receiver,
            String methodName, Object[] args) {
        ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> map =
                findMap(receiver.getClass());
        if (map == null) {
            Log.e(TAG, "No map found for " + receiver.getClass().getName());
            return new Bundle();
        }
        @SuppressWarnings("unchecked")
        CallbackHandler<T> callbackHandler = (CallbackHandler<T>) map.get(methodName);
        if (callbackHandler == null) {
            Log.e(TAG, "No handler found for " + methodName + " on "
                    + receiver.getClass().getName());
            return new Bundle();
        }
        Bundle bundle = callbackHandler.assembleArguments(args);
        bundle.putString(EXTRA_METHOD, methodName);
        return bundle;
    }

    <T extends CallbackReceiver> void ensureInitialized(Class<T> cls) {
        if (!mClsLookup.containsKey(cls)) {
            runInit(cls);
        }
    }

    /**
     * Trigger a call to a callback using arguments that were generated with
     * {@link #createArguments}.
     */
    public <T extends CallbackReceiver> void invokeCallback(Context context, T receiver,
                Intent intent) {
        invokeCallback(context, receiver, intent.getExtras());
    }

    /**
     * Trigger a call to a callback using arguments that were generated with
     * {@link #createArguments}.
     */
    public <T extends CallbackReceiver> void invokeCallback(Context context, T receiver,
                Bundle bundle) {
        ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> map =
                findMap(receiver.getClass());
        if (map == null) {
            Log.e(TAG, "No map found for " + receiver.getClass().getName());
            return;
        }
        String method = bundle.getString(EXTRA_METHOD);
        @SuppressWarnings("unchecked")
        CallbackHandler<T> callbackHandler = (CallbackHandler<T>) map.get(method);
        if (callbackHandler == null) {
            Log.e(TAG, "No handler found for " + method + " on " + receiver.getClass().getName());
            return;
        }
        callbackHandler.executeCallback(context, receiver, bundle);
    }

    private ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> findMap(Class<?> aClass) {
        ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> map = mClsLookup.get(aClass);
        if (map != null) {
            return map;
        }
        if (aClass.getSuperclass() != null) {
            return findMap(aClass.getSuperclass());
        }
        return null;
    }

    private <T extends CallbackReceiver> void runInit(Class<T> cls) {
        mClsLookup.put(cls, new ArrayMap<String, CallbackHandler<? extends CallbackReceiver>>());
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

    private <T extends CallbackReceiver> void registerHandler(Class<T> cls, String method,
            CallbackHandler<T> handler) {
        ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> map = mClsLookup.get(cls);
        if (map == null) {
            throw new IllegalStateException("registerHandler called before init was run");
        }
        map.put(method, handler);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Runnable> findInitClass(Class<? extends CallbackReceiver> cls)
            throws ClassNotFoundException {
        String pkg = cls.getPackage().getName();
        String c = String.format("%s.%sInitializer", pkg, cls.getSimpleName());
        return (Class<? extends Runnable>) Class.forName(c, false, cls.getClassLoader());
    }

    /**
     * Registers a callback handler to be executed when a given PendingIntent is fired
     * for a {@link RemoteCallback}.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    public static <T extends CallbackReceiver> void registerCallbackHandler(Class<T> cls,
            String method, CallbackHandler<T> handler) {
        sInstance.registerHandler(cls, method, handler);
    }

    /**
     * The interface used to trigger a callback when the pending intent is fired.
     * Note: This should only be referenced by generated code, there is no reason to reference
     * this otherwise.
     * @param <T> The receiver type for this callback handler.
     */
    public interface CallbackHandler<T extends CallbackReceiver> {
        /**
         * Executes a callback given a Bundle of aurgements.
         * Note: This should only be called by generated code, there is no reason to reference this
         * otherwise.
         */
        void executeCallback(Context context, T receiver, Bundle arguments);

        /**
         * Transforms arguments into a Bundle that can cross processes and later be used with
         * {@link #executeCallback(Object, Bundle)}.
         * Note: This should only be called by generated code, there is no reason to reference this
         * otherwise.
         */
        Bundle assembleArguments(Object... args);
    }
}
