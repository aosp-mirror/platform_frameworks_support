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

import android.app.PendingIntent;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.RemoteActionCompat;
import androidx.core.util.Function;
import androidx.core.util.Preconditions;
import androidx.textclassifier.widget.ToolbarController;

import java.util.List;

/**
 * Utility class for creating {@link TextLinks.TextLinkSpan}s.
 */
public final class TextLinkSpanFactory {

    private static final String LOG_TAG = "TextLinkSpanFactory";

    private TextLinkSpanFactory() {}

    /**
     * Action to perform when a {@link TextLinks.TextLinkSpan} is clicked.
     */
    public interface OnTextClassificationResult {

        /**
         * Default {@link OnTextClassificationResult} implementation.
         */
        OnTextClassificationResult DEFAULT = new OnTextClassificationResult() {
            @Override
            public void onTriggerActions(
                    TextClassification textClassification, TextView textView, int start, int end) {
                final List<RemoteActionCompat> actions = textClassification.getActions();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ToolbarController.getInstance(textView).show(actions, start, end);
                    return;
                }

                if (!actions.isEmpty()) {
                    try {
                        actions.get(0).getActionIntent().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(LOG_TAG, "Error handling TextLinkSpan click", e);
                    }
                    return;
                }

                Log.d(LOG_TAG, "Cannot trigger link. No actions found.");
            }
        };

        /**
         * Called when a {@link TextClassification} is returned in response to a
         * {@link TextLinks.TextLinkSpan} click.
         */
        void onTriggerActions(
                TextClassification textClassification, TextView textView, int start, int end);
    }

    /**
     * Creates a TextLink SpanFactory for the specified {@link OnTextClassificationResult}
     */
    public static Function<TextLinks.TextLinkSpanData, TextLinks.TextLinkSpan> of(
            OnTextClassificationResult onTextClassificationResult) {
        return new OnTriggerActionsSpanFactory(onTextClassificationResult);
    }

    /**
     * A TextLinkSpan factory that performs a specified action when the TextLinkSpan is clicked.
     */
    private static final class OnTriggerActionsSpanFactory implements
            Function<TextLinks.TextLinkSpanData, TextLinks.TextLinkSpan> {

        private final OnTextClassificationResult mOnTextClassificationResult;

        OnTriggerActionsSpanFactory(OnTextClassificationResult onTextClassificationResult) {
            mOnTextClassificationResult = Preconditions.checkNotNull(onTextClassificationResult);
        }

        @Override
        public TextLinks.TextLinkSpan apply(@NonNull TextLinks.TextLinkSpanData textLinkSpanData) {
            return new TextLinks.TextLinkSpan(textLinkSpanData, mOnTextClassificationResult);
        }
    }

}
