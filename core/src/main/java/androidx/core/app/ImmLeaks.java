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

package androidx.core.app;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ImmLeaks {

    private static ImmLeaks sInstance;
    private static boolean sInitialized;

    @SuppressLint("PrivateApi")
    @MainThread
    @Nullable
    static ImmLeaks getInstance() {
        if (sInitialized) {
            return sInstance;
        }
        sInitialized = true;
        try {
            Field servedViewField = InputMethodManager.class.getDeclaredField("mServedView");
            servedViewField.setAccessible(true);
            Field hField = InputMethodManager.class.getDeclaredField("mH");
            hField.setAccessible(true);
            Method finishInputLockedMethod = InputMethodManager.class.getDeclaredMethod(
                    "finishInputLocked");
            Method peekInstanceMethod = InputMethodManager.class.getDeclaredMethod("peekInstance");
            finishInputLockedMethod.setAccessible(true);
            sInstance = new ImmLeaks(hField, servedViewField, finishInputLockedMethod,
                    peekInstanceMethod);
        } catch (NoSuchFieldException e) {
            // very oem much custom ¯\_(ツ)_/¯
        } catch (NoSuchMethodException e) {
        }
        return sInstance;
    }

    private final Field mHField;
    private final Field mServedViewField;
    private final Method mFinishInputLockedMethod;
    private final Method mPeekInstanceMethod;

    ImmLeaks(Field hField, Field servedViewField,
            Method finishInputLockedMethod, Method peekInstanceMethod) {
        mHField = hField;
        mServedViewField = servedViewField;
        mFinishInputLockedMethod = finishInputLockedMethod;
        mPeekInstanceMethod = peekInstanceMethod;
    }

    void clearInputMethodManagerLeak() {
        try {
            Object imm = mPeekInstanceMethod.invoke(null);
            if (imm == null) {
                // nothing to do InputMethodManager wasn't even initialized
                return;
            }
            InputMethodManager inputMethodManager = (InputMethodManager) imm;
            Object lock = mHField.get(inputMethodManager);
            synchronized (lock) {
                View servedView = (View) mServedViewField.get(inputMethodManager);
                if (servedView != null && servedView.getWindowVisibility() == View.GONE) {
                    // servedView is not attached
                    mFinishInputLockedMethod.invoke(inputMethodManager);
                }
            }
        } catch (InvocationTargetException unexpected) {
            Log.e("IMMLeaks", "Unexpected reflection exception", unexpected);
        } catch (IllegalAccessException unexpected) {
            Log.e("IMMLeaks", "Unexpected reflection exception", unexpected);
        }
    }
}
