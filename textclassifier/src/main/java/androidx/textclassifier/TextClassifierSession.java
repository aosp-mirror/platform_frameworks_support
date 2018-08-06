package androidx.textclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

public class TextClassifierSession implements SessionStrategy {
    private SessionStrategy mSessionStrategy;

    public TextClassifierSession(
            @NonNull TextClassifier textClassifier,
            @NonNull TextClassificationContext textClassificationContext) {
        this(new DefaultSessionStrategy(textClassifier, textClassificationContext));
    }

    TextClassifierSession(@NonNull SessionStrategy sessionStrategy) {
        mSessionStrategy = Preconditions.checkNotNull(sessionStrategy);
    }

    /**
     * Returns suggested text selection start and end indices, recognized entity types, and their
     * associated confidence scores. The entity types are ordered from highest to lowest scoring.
     *
     * <p><strong>NOTE: </strong>Call on a worker thread.
     *
     * @param request the text selection request
     */
    @Override
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
     * @param request the text classification request
     */
    @Override
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
     * @param request the text links request
     * @see #getMaxGenerateLinksTextLength()
     */
    @Override
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
    @Override
    public int getMaxGenerateLinksTextLength() {
        return mSessionStrategy.getMaxGenerateLinksTextLength();
    }

    /**
     * Reports a selection event.
     */
    @Override
    public void reportSelectionEvent(@NonNull SelectionEvent event) {
        Preconditions.checkNotNull(event);
        mSessionStrategy.reportSelectionEvent(event);
    }
}
