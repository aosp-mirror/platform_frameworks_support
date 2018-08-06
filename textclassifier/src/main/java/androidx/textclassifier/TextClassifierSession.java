/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

/**
 * Interface for providing text classification related features.
 *
 * TextClassifier acts as a proxy to either the system provided TextClassifier, or an
 * equivalent implementation provided by an app. Each instance of the class therefore represents
 * one connection to the classifier implementation.
 *
 * <p>Text classifier is session-aware and it can't be reused once {@link #destroy()} is called.
 *
 * <p>Unless otherwise stated, methods of this interface are blocking operations.
 * Avoid calling them on the UI thread.
 */
public class TextClassifierSession {

    @NonNull
    private SessionStrategy mSessionStrategy;

    /**
     * Creates a {@link TextClassifier} by using default session handling.
     */
    public TextClassifierSession(
            @NonNull TextClassifier textClassifier,
            @NonNull TextClassificationContext textClassificationContext) {
        mSessionStrategy = new DefaultSessionStrategy(textClassificationContext, textClassifier);
    }

    /**
     * Creates a {@link TextClassifier} by using default session handling.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public TextClassifierSession(@NonNull SessionStrategy sessionStrategy) {
        mSessionStrategy = Preconditions.checkNotNull(sessionStrategy);
    }

    /**
     * Returns suggested text selection start and end indices, recognized entity types, and their
     * associated confidence scores. The entity types are ordered from highest to lowest scoring.
     *
     * <p><strong>NOTE: </strong>Call on a worker thread.
     *
     * <p><strong>NOTE: </strong>If a TextClassifier has been destroyed, calls to this
     * method should throw an {@link IllegalStateException}. See {@link #isDestroyed()}.
     *
     * @param request the text selection request
     */
    @WorkerThread
    @NonNull
    public TextSelection suggestSelection(@NonNull TextSelection.Request request) {
        Preconditions.checkNotNull(request);
        return mSessionStrategy.suggestSelection(request);
    }

    /**
     * Classifies the specified text and returns a {@link TextClassification} object that can be
     * used to generate a widget for handling the classified text.
     *
     * <p><strong>NOTE: </strong>Call on a worker thread.
     *
     * <strong>NOTE: </strong>If a TextClassifier has been destroyed, calls to this
     * method should throw an {@link IllegalStateException}. See {@link #isDestroyed()}.
     *
     * @param request the text classification request
     */
    @WorkerThread
    @NonNull
    public TextClassification classifyText(@NonNull TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        return mSessionStrategy.classifyText(request);
    }

    /**
     * Generates and returns a {@link TextLinks} that may be applied to the text to annotate it with
     * links information.
     *
     * <p><strong>NOTE: </strong>Call on a worker thread.
     *
     * <strong>NOTE: </strong>If a TextClassifier has been destroyed, calls to this
     * method should throw an {@link IllegalStateException}. See {@link #isDestroyed()}.
     *
     * @param request the text links request
     *
     * @see #getMaxGenerateLinksTextLength()
     */
    @WorkerThread
    @NonNull
    public TextLinks generateLinks(@NonNull TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        return mSessionStrategy.generateLinks(request);
    }

    /**
     * Returns the maximal length of text that can be processed by generateLinks.
     *
     * @see #generateLinks(TextLinks.Request)
     */
    public int getMaxGenerateLinksTextLength() {
        return mSessionStrategy.getMaxGenerateLinksTextLength();
    }

    public void destroy() {
        mSessionStrategy.destroy();
    }

    public boolean isDestroyed() {
        return mSessionStrategy.isDestroyed();
    }

    /**
     * Reports a selection event.
     *
     * <strong>NOTE: </strong>If a TextClassifier has been destroyed, calls to this
     * method should throw an {@link IllegalStateException}. See {@link #isDestroyed()}.
     */
    public final void reportSelectionEvent(@NonNull SelectionEvent event) {
        Preconditions.checkNotNull(event);
        mSessionStrategy.reportSelectionEvent(event);
    }

    /**
     * Called when a selection event is reported.
     */
    public void onSelectionEvent(@NonNull SelectionEvent event) {
    }
}
