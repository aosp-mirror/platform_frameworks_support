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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

/**
 * Class to handle the creation of {@link TextClassifier}.
 */
public final class TextClassificationManager {
    /**
     * A factory that returns the default text classifier.
     */
    @NonNull
    private final TextClassifierFactory mDefaultFactory;
    @NonNull
    private final Context mContext;
    @Nullable
    private static TextClassificationManager sInstance;
    @Nullable
    private TextClassifierFactory mTextClassifierFactory;

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    TextClassificationManager(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
        mDefaultFactory = new TextClassifierFactory() {
            @Override
            public TextClassifier create(TextClassificationContext textClassificationContext) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return PlatformTextClassifierWrapper.create(
                            mContext, textClassificationContext);
                }
                return LegacyTextClassifier.of(mContext);
            }
        };
        mTextClassifierFactory = mDefaultFactory;
    }

    /**
     * Return an instance of {@link TextClassificationManager}.
     */
    public static TextClassificationManager of(@NonNull Context context) {
        if (sInstance == null) {
            Context appContext = context.getApplicationContext();
            sInstance = new TextClassificationManager(appContext);
        }
        return sInstance;
    }

    /**
     * Returns a newly created text classifier.
     * <p>
     * If a factory is set through {@link #setTextClassifierFactory(TextClassifierFactory)},
     * an instance created by the factory will be returned. Otherwise, a default text classifier
     * will be returned.
     */
    @NonNull
    public TextClassifier createTextClassifier(
            @NonNull TextClassificationContext textClassificationContext) {
        Preconditions.checkNotNull(textClassificationContext);
        return getTextClassifierFactory().create(textClassificationContext);
    }

    /**
     * Sets a factory that can create a preferred text classifier.
     * <p>
     * To turn off the feature completely, you can set a factory that returns
     * {@link TextClassifierFactory#NO_OP_FACTORY}.
     * <p>
     * You can clear the preferred factory by setting {@code null}, the default factory will then
     * be used.
     */
    public void setTextClassifierFactory(@Nullable TextClassifierFactory factory) {
        mTextClassifierFactory = factory;
    }

    /**
     * Returns a text classifier factory.
     * <p>
     * If a factory is set through {@link #setTextClassifierFactory(TextClassifierFactory)},
     * that factory will be returned. Otherwise, the default factory will be returned.
     */
    @NonNull
    public TextClassifierFactory getTextClassifierFactory() {
        if (mTextClassifierFactory != null) {
            return mTextClassifierFactory;
        }
        return mDefaultFactory;
    }

    /**
     * Returns the default text classifier factory, regardless of the one set through
     * {@link #setTextClassifierFactory}.
     * <p>
     * In post O, the default factory will return a text classifier that bases on the
     * platform {@link android.view.textclassifier.TextClassifier}. Otherwise, the default
     * factory will return a text classifier that is backed by the legacy
     * {@link androidx.core.text.util.LinkifyCompat} API.
     */
    public TextClassifierFactory getDefaultTextClassifierFactory() {
        return mDefaultFactory;
    }

}
