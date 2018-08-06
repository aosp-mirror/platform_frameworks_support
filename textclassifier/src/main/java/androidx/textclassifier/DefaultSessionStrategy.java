package androidx.textclassifier;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

public class DefaultSessionStrategy implements SessionStrategy {
    private TextClassifier mTextClassifier;
    private TextClassificationContext mTextClassificationContext;

    public DefaultSessionStrategy(
            @NonNull TextClassifier textClassifier,
            @NonNull TextClassificationContext textClassificationContext) {
        mTextClassifier = Preconditions.checkNotNull(textClassifier);
        mTextClassificationContext = Preconditions.checkNotNull(textClassificationContext);
    }

    @Override
    public TextSelection suggestSelection(TextSelection.Request request) {
        return mTextClassifier.suggestSelection(mTextClassificationContext, request);
    }

    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        return mTextClassifier.classifyText(mTextClassificationContext, request);
    }

    @Override
    public TextLinks generateLinks(TextLinks.Request request) {
        return mTextClassifier.generateLinks(mTextClassificationContext, request);
    }

    @Override
    public int getMaxGenerateLinksTextLength() {
        return mTextClassifier.getMaxGenerateLinksTextLength();
    }

    @Override
    public void reportSelectionEvent(SelectionEvent event) {
        mTextClassifier.onSelectionEvent(event);
    }
}
