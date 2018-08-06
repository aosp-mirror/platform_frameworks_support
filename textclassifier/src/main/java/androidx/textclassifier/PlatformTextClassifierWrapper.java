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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

/**
 * Provides a {@link androidx.textclassifier.TextClassifier} interface for a
 * {@link android.view.textclassifier.TextClassifier} object.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
public class PlatformTextClassifierWrapper extends TextClassifier {
    private final Context mContext;
    private final TextClassifier mFallback;

    public PlatformTextClassifierWrapper(@NonNull Context context) {
        super();
        mContext = Preconditions.checkNotNull(context);
        mFallback = LegacyTextClassifier.of(context);
    }

    @Override
    public TextClassifierSession createSession(
            TextClassificationContext textClassificationContext) {
        android.view.textclassifier.TextClassificationManager textClassificationManager =
                (android.view.textclassifier.TextClassificationManager)
                        mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
        android.view.textclassifier.TextClassifier platformTextClassifier;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            platformTextClassifier =
                    textClassificationManager.createTextClassificationSession(
                            (android.view.textclassifier.TextClassificationContext)
                                    textClassificationContext.toPlatform());
        } else {
            // No session handling before P.
            platformTextClassifier = textClassificationManager.getTextClassifier();
        }

        SessionStrategy sessionStrategy = new ProxySessionStrategy(mContext, platformTextClassifier);
        return new TextClassifierSession(sessionStrategy);
    }

    /**
     * Delegates session handling to {@link android.view.textclassifier.TextClassifier}.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private class ProxySessionStrategy implements SessionStrategy {
        private final android.view.textclassifier.TextClassifier mPlatformTextClassifier;
        private final Context mContext;

        ProxySessionStrategy(
                @NonNull Context context,
                @NonNull android.view.textclassifier.TextClassifier textClassifier) {
            mContext = Preconditions.checkNotNull(context);
            mPlatformTextClassifier = Preconditions.checkNotNull(textClassifier);
        }

        @Override
        public void destroy() {
            mPlatformTextClassifier.destroy();
        }

        @Override
        public void reportSelectionEvent(@NonNull SelectionEvent event) {
            Preconditions.checkNotNull(event);
            mPlatformTextClassifier.onSelectionEvent(
                    (android.view.textclassifier.SelectionEvent) event.toPlatform());
        }

        @Override
        public boolean isDestroyed() {
            return mPlatformTextClassifier.isDestroyed();
        }

        @Override
        public TextClassification classifyText(TextClassification.Request request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return TextClassification.fromPlatform(mContext,
                        mPlatformTextClassifier.classifyText(
                                (android.view.textclassifier.TextClassification.Request)
                                        request.toPlatform()));
            }
            TextClassification textClassification = TextClassification.fromPlatform(mContext,
                    mPlatformTextClassifier.classifyText(
                            request.getText(),
                            request.getStartIndex(),
                            request.getEndIndex(),
                            ConvertUtils.unwrapLocalListCompat(request.getDefaultLocales())));
            return textClassification;
        }

        @Override
        public TextLinks generateLinks(TextLinks.Request request) {
            if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.P) {
                return TextLinks.fromPlatform(mPlatformTextClassifier.generateLinks(
                        request.toPlatform()), request.getText());
            }
            return mFallback.generateLinks(request);
        }

        @Override
        public TextSelection suggestSelection(TextSelection.Request request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return TextSelection.fromPlatform(
                        mPlatformTextClassifier.suggestSelection(
                                (android.view.textclassifier.TextSelection.Request)
                                        request.toPlatform()));
            }
            return TextSelection.fromPlatform(
                    mPlatformTextClassifier.suggestSelection(
                            request.getText(),
                            request.getStartIndex(),
                            request.getEndIndex(),
                            ConvertUtils.unwrapLocalListCompat(request.getDefaultLocales())));
        }

        @Override
        public int getMaxGenerateLinksTextLength() {
            return PlatformTextClassifierWrapper.this.getMaxGenerateLinksTextLength();
        }
    }
}
