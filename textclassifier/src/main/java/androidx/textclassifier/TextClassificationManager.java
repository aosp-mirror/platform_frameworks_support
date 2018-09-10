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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.util.WeakHashMap;

/**
 * Class to handle the creation of {@link TextClassifier}.
 */
public final class TextClassificationManager {
    private final Context mContext;
    @Nullable
    @GuardedBy("sLock")
    private static TextClassificationManager sInstance;
    @GuardedBy("mLock")
    private WeakHashMap<Context, TextClassifier> mMapping = new WeakHashMap<>();
    private Object mLock = new Object();
    private static final Object sLock = new Object();

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    TextClassificationManager(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    /**
     * Return an instance of {@link TextClassificationManager}.
     */
    public static TextClassificationManager of(@NonNull Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                Context appContext = context.getApplicationContext();
                sInstance = new TextClassificationManager(appContext);
            }
            return sInstance;
        }
    }

    /**
     * Returns the text classifier set through {@link #setTextClassifier(Context, TextClassifier)},
     * a default text classifier is returned if it is not ever set, or a {@code null} is set.
     */
    @NonNull
    public TextClassifier getTextClassifier(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        TextClassifier textClassifier;
        synchronized (mLock) {
            textClassifier = mMapping.get(context);
        }
        if (textClassifier != null) {
            return textClassifier;
        }
        return defaultTextClassifier();

    }

    /**
     * Sets a preferred text classifier to the given context.
     * <p>
     * To turn off the feature completely, you can set a {@link TextClassifier#NO_OP}. If
     * @{code null} is set, default text classifier is used.
     * <p>
     * The text classifier is only set to the given context. For example, calling
     * {@link #setTextClassifier(Context, TextClassifier)} with activity A context will have no
     * effect to Activity B.
     */
    public void setTextClassifier(
            @NonNull Context context, @Nullable TextClassifier textClassifier) {
        Preconditions.checkNotNull(context);

        synchronized (mLock) {
            mMapping.put(context, textClassifier);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPlatformTextClassifier(context, textClassifier);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setPlatformTextClassifier(
            @NonNull Context context,
            @Nullable TextClassifier textClassifier) {
        android.view.textclassifier.TextClassificationManager textClassificationManager =
                (android.view.textclassifier.TextClassificationManager)
                        context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);

        android.view.textclassifier.TextClassifier platformTextClassifier =
                textClassifier == null ? null : new PlatformTextClassifier(context, textClassifier);
        textClassificationManager.setTextClassifier(platformTextClassifier);
    }

    /**
     * Returns the default text classifier.
     */
    private TextClassifier defaultTextClassifier() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PlatformTextClassifierWrapper.create(mContext);
        }
        return LegacyTextClassifier.of(mContext);
    }
}
