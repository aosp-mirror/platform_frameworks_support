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

package androidx.textclassifier;

import android.content.Context;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.lang.ref.WeakReference;

/**
 * Class to handle the creation of {@link TextClassifier}.
 */
public final class TextClassificationManager {

    // TextClassificationManager may be called from any thread.
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static WeakReference<TextClassificationManager> sInstance = new WeakReference<>(null);

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Context mAppContext;
    @GuardedBy("mLock")
    private TextClassifier mTextClassifier;
    @GuardedBy("mLock")
    private TextClassifier mDefaultTextClassifier;

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    TextClassificationManager(Context appContext) {
        mAppContext = appContext;
    }

    /**
     * Return an instance of {@link TextClassificationManager}.
     */
    public static TextClassificationManager of(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        final Context appContext = context.getApplicationContext();
        // Unfortunately, appContext can sometimes be null.
        if (appContext == null) {
            throw new IllegalArgumentException(
                    "Context without an application context is not supported.");
        }

        synchronized (sLock) {
            final TextClassificationManager tcm = sInstance.get();
            if (tcm == null || tcm.mAppContext != appContext) {
                sInstance = new WeakReference<>(new TextClassificationManager(appContext));
            }
            return sInstance.get();
        }
    }

    /**
     * Returns the text classifier set through {@link #setTextClassifier(TextClassifier)},
     * a default text classifier is returned if it is not ever set, or a {@code null} is set.
     */
    @NonNull
    public TextClassifier getTextClassifier() {
        synchronized (mLock) {
            if (mTextClassifier != null) {
                return mTextClassifier;
            }
            return defaultTextClassifier();
        }
    }

    /**
     * Sets a preferred text classifier.
     * <p>
     * To turn off the feature completely, you can set a {@link TextClassifier#NO_OP}.
     */
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        synchronized (mLock) {
            mTextClassifier = textClassifier;
        }
    }

    // TODO: Create a method to set the textClassifier on the platform TextClassificationManager
    // of a specified context so that platform features such as "Smart selection" can make use of
    // the same textClassifier as the one set on this object.

    /**
     * Returns the default text classifier.
     */
    private TextClassifier defaultTextClassifier() {
        synchronized (mLock) {
            if (mDefaultTextClassifier != null) {
                return mDefaultTextClassifier;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mDefaultTextClassifier = PlatformTextClassifierWrapper.create(mAppContext);
            } else {
                mDefaultTextClassifier = LegacyTextClassifier.of(mAppContext);
            }
            return mDefaultTextClassifier;
        }
    }
}
