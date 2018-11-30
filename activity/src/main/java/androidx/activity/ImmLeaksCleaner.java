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

package androidx.activity;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.MainThread;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ImmLeaksCleaner implements GenericLifecycleObserver {
    private static final int NOT_INITIALIAZED = 0;
    private static final int INIT_SUCCESS = 1;
    private static final int INIT_FAILED = 2;
    private static int sReflectedFieldsInitialized = NOT_INITIALIAZED;
    private static Field sHField;
    private static Field sServedViewField;
    private static Method sFinishInputLockedMethod;

    private Activity mActivity;

    ImmLeaksCleaner(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return;
        }
        if (sReflectedFieldsInitialized == NOT_INITIALIAZED) {
            initializeReflectiveFields();
        }
        if (sReflectedFieldsInitialized == INIT_SUCCESS) {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager)
                        mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                Object lock = sHField.get(inputMethodManager);
                synchronized (lock) {
                    View servedView = (View) sServedViewField.get(inputMethodManager);
                    if (servedView != null && parentActivity(servedView) == mActivity) {
                        // servedView is not attached
                        sFinishInputLockedMethod.invoke(inputMethodManager);
                    }
                }
            } catch (InvocationTargetException unexpected) {
            } catch (IllegalAccessException unexpected) {
            }
        }
    }

    @MainThread
    private static void initializeReflectiveFields() {
        try {
            sReflectedFieldsInitialized = INIT_FAILED;
            sServedViewField = InputMethodManager.class.getDeclaredField("mServedView");
            sServedViewField.setAccessible(true);
            sHField = InputMethodManager.class.getDeclaredField("mH");
            sHField.setAccessible(true);
            sFinishInputLockedMethod = InputMethodManager.class.getDeclaredMethod(
                    "finishInputLocked");
            sFinishInputLockedMethod.setAccessible(true);
            sReflectedFieldsInitialized = INIT_SUCCESS;
        } catch (NoSuchFieldException e) {
            // very oem much custom ¯\_(ツ)_/¯
        } catch (NoSuchMethodException e) {

        }
    }

    private static Activity parentActivity(View view) {
        Context context = view.getContext();
        while (context != null && !(context instanceof Activity)) {
            context = context instanceof ContextWrapper
                    ? ((ContextWrapper) context).getBaseContext() : null;
        }
        return (Activity) context;
    }
}

